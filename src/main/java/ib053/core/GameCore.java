package ib053.core;

import com.esotericsoftware.jsonbeans.JsonReader;
import com.esotericsoftware.jsonbeans.JsonValue;
import com.koloboke.collect.map.LongObjMap;
import com.koloboke.collect.map.hash.HashLongObjMap;
import com.koloboke.collect.map.hash.HashLongObjMaps;
import com.koloboke.function.LongObjConsumer;
import ib053.core.activities.LevelUpActivity;
import ib053.core.activities.LocationActivity;
import ib053.frontend.Frontend;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;

/**
 * Stores the game's state and manages its lifecycle and boilerplate.
 */
public final class GameCore {

    private final Frontend[] frontends;
    public final ScheduledExecutorService eventLoop = new ScheduledThreadPoolExecutor(1) {
        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);

            if (t != null) {
                System.err.println("EventLoop item crashed");
                t.printStackTrace(System.err);
            }
        }
    };

    private static final long STARTING_LOCATION_ID = 0;

    public final LongObjMap<Location> worldLocations;
    public final LongObjMap<Item> worldItems;
    public final LongObjMap<Enemy> worldEnemies;

    private final LongObjMap<List<Player>> playersInLocation;
    private final LongObjMap<LocationActivity> locationActivities;

    private final List<Player> players = new ArrayList<>();

    /** Creates the game core and starts the event loop, starting the game.
     * Handles the initialization of front-ends. */
    public GameCore(File locationFile, File itemFile, File enemyFile, Frontend...frontends) {
        this.frontends = frontends;

        final JsonReader jsonReader = new JsonReader();

        { // Load locations
            final JsonValue locationJson = jsonReader.parse(locationFile);
            assert locationJson.isArray();
            final HashLongObjMap<Location> locations = HashLongObjMaps.newMutableMap((int) (locationJson.size * 1.25f));
            for (JsonValue value : locationJson) {
                final Location location = Location.read(value);
                final Location previous = locations.put(location.id, location);
                if (previous != null) {
                    throw new IllegalArgumentException("Locations " + location.name + " and " + previous.name + " share identical ID " + previous.id);
                }
            }
            worldLocations = HashLongObjMaps.newImmutableMap(locations);

            // Fill playersInLocation with ArrayLists
            playersInLocation = HashLongObjMaps.newImmutableMap((map) -> {
                worldLocations.keySet().forEach((LongConsumer) id -> map.accept(id, new ArrayList<>()));
            });

            // Pre-create Location activities
            locationActivities = HashLongObjMaps.newImmutableMap((map) -> {
                worldLocations.forEach((LongObjConsumer<? super Location>) (id, location) -> map.accept(id, new LocationActivity(location)));
            });
        }

        { // Load items
            final JsonValue itemJson = jsonReader.parse(itemFile);
            assert itemJson.isArray();
            final HashLongObjMap<Item> items = HashLongObjMaps.newMutableMap((int) (itemJson.size * 1.25f));
            for (JsonValue value : itemJson) {
                final Item item = Item.read(value);
                final Item previous = items.put(item.id, item);
                if (previous != null) {
                    throw new IllegalArgumentException("Items " + item.name + " and " + previous.name + " share identical ID " + previous.id);
                }
            }
            worldItems = HashLongObjMaps.newImmutableMap(items);
        }

        { // Load enemies
            final JsonValue itemJson = jsonReader.parse(enemyFile);
            assert itemJson.isArray();
            final HashLongObjMap<Enemy> enemies = HashLongObjMaps.newMutableMap((int) (itemJson.size * 1.25f));
            for (JsonValue value : itemJson) {
                final Enemy enemy = Enemy.read(value);
                final Enemy previous = enemies.put(enemy.id, enemy);
                if (previous != null) {
                    throw new IllegalArgumentException("Enemies " + enemy.name + " and " + previous.name + " share identical ID " + previous.id);
                }
            }
            worldEnemies = HashLongObjMaps.newImmutableMap(enemies);
        }

        // Initialize this in the event loop, so that nothing may disrupt the initialization
        eventLoop.execute(() -> {
            for (Frontend frontend : frontends) {
                frontend.initialize(this);
            }

            for (Frontend frontend : frontends) {
                frontend.begin();
            }
        });
    }

    /** Creates a whole new Player, with given name.
     * Call initNewPlayer when ready to start playing.
     * @return new player or null if the name is already taken. */
    public Player createNewPlayer(String name) {
        assert name != null;
        long id = 1; // Sequential player IDs for now
        //Check if name is not taken
        for (Player player : players) {
            if (player.getId() >= id) {
                id = player.getId() + 1;
            }
            if (player.getName().equals(name)) {
                return null;
            }
        }

        final Attributes attributes = new Attributes(true);
        attributes.set(Attribute.LEVEL, 1);

        attributes.set(Attribute.STRENGTH, 5);
        attributes.set(Attribute.DEXTERITY, 5);
        attributes.set(Attribute.AGILITY, 5);
        attributes.set(Attribute.LUCK, 5);
        attributes.set(Attribute.STAMINA, 5);

        final Player player = new Player(this, id, name, attributes);
        players.add(player);
        return player;
    }

    public void initNewPlayer(Player player) {
        assert player.currentActivity == null;
        assert player.location == null;

        player.setEquipment(worldItems.get(1));//Give player dull knife
        changePlayerLocation(player, worldLocations.get(STARTING_LOCATION_ID));
        changePlayerActivity(player, locationActivities.get(STARTING_LOCATION_ID));
    }

    public void changePlayerLocation(Player player, Location toPlace) {
        assert player != null;
        assert toPlace != null;

        if (player.location != null) {
            final List<Player> playersInOldLocation = playersInLocation.get(player.location.id);
            assert playersInOldLocation != null;
            playersInOldLocation.remove(player);
        }
        player.location = toPlace;
        final List<Player> playersInNewLocation = playersInLocation.get(toPlace.id);
        assert playersInNewLocation != null;
        playersInNewLocation.add(player);
    }

    /**
     * Change activity that player is engaged in.
     * This is the only way to change player's activity.
     *
     * Calls respective Activity.begin/endActivity() and {@link #notifyPlayerActivityChanged(Player)}.
     */
    public void changePlayerActivity(Player player, Activity newActivity) {
        assert player != null;
        assert newActivity != null;

        if (player.currentActivity != null) {
            player.currentActivity.endActivity(player);
            player.currentActivity.engagedPlayers.remove(player);
        }

        if (newActivity.core == null) {
            newActivity.core = this;
            newActivity.initialize();
        } else if (newActivity.core != this) {
            throw new IllegalArgumentException("Activity "+newActivity+" already assigned to a different core!");
        }

        player.currentActivity = newActivity;
        player.currentActivity.engagedPlayers.add(player);
        newActivity.beginActivity(player);

        notifyPlayerActivityChanged(player);
    }

    /** Changes the player's activity to default activity (LocationActivity, usually) */
    public void changePlayerActivityToDefault(Player player) {
        final LocationActivity locationActivity = locationActivities.get(player.getLocation().id);
        assert locationActivity != null;
        changePlayerActivity(player, locationActivity);
    }

    /** Called when player has new actions to choose from.
     * Delegates this notification to frontends.
     * Called automatically when changing activity. */
    public void notifyPlayerActivityChanged(Player player) {
        for (Frontend frontend : frontends) {
            frontend.playerActivityChanged(player);
        }
    }

    /** Called by {@link Activity} when player has witnessed an Event.
     * Delegates this notification to frontends. */
    public void notifyPlayerEventHappened(Player player, Event event) {
        for (Frontend frontend : frontends) {
            frontend.playerReceiveEvent(player, event);
        }
    }

    public void giveExperience(Player player, int experiencePoints) {
        player.experience += experiencePoints;
        final int xpToNextLevel = player.getXpToNextLevel();
        if (player.experience >= xpToNextLevel) {
            notifyPlayerEventHappened(player, new Event("You have gained "+experiencePoints+" xp"));
            changePlayerActivity(player, LevelUpActivity.INSTANCE);
        } else {
            notifyPlayerEventHappened(player, new Event("You have gained "+experiencePoints+" xp ("+(player.experience * 100 / xpToNextLevel)+"% to next level)"));
        }
    }

    public void setActivityActions(Activity activity, List<Action> actions) {
        activity.actions.clear();
        activity.actions.addAll(actions);
        for (Player engagedPlayer : activity.engagedPlayers) {
            notifyPlayerActivityChanged(engagedPlayer);
        }
    }

    public void setActivityActions(Activity activity, Action...actions) {
        activity.actions.clear();
        Collections.addAll(activity.actions, actions);
        for (Player engagedPlayer : activity.engagedPlayers) {
            notifyPlayerActivityChanged(engagedPlayer);
        }
    }

    public void addActivityAction(Activity activity, Action action) {
        activity.actions.add(action);
        for (Player engagedPlayer : activity.engagedPlayers) {
            notifyPlayerActivityChanged(engagedPlayer);
        }
    }

    public boolean removeActivityAction(Activity activity, Action action) {
        if(activity.actions.remove(action)) {
            for (Player engagedPlayer : activity.engagedPlayers) {
                notifyPlayerActivityChanged(engagedPlayer);
            }
            return true;
        }
        return false;
    }

    public void clearActivityActions(Activity activity) {
        if (!activity.actions.isEmpty()) {
            activity.actions.clear();
            for (Player engagedPlayer : activity.engagedPlayers) {
                notifyPlayerActivityChanged(engagedPlayer);
            }
        }
    }

    /** @return Player prevously created with given ID, or null if no such player exists. */
    public Player findPlayer(long id) {
        for (Player player : players) {
            if (player.getId() == id) {
                return player;
            }
        }
        return null;
    }

    public void shutdown() {
        eventLoop.shutdown();
        try {
            if(eventLoop.awaitTermination(10, TimeUnit.SECONDS)) {
                System.out.println("Event loop terminated");
            } else {
                System.err.println("Event loop doesn't want to terminate, shutting down");
                eventLoop.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.err.println("Event loop awaitTermination has been interrupted, shutting down");
            eventLoop.shutdownNow();
        }
    }
}

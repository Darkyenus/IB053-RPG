package ib053.core;

import com.esotericsoftware.jsonbeans.Json;
import com.esotericsoftware.jsonbeans.JsonReader;
import com.esotericsoftware.jsonbeans.JsonValue;
import com.esotericsoftware.jsonbeans.OutputType;
import com.koloboke.collect.map.LongObjMap;
import com.koloboke.collect.map.hash.HashLongObjMap;
import com.koloboke.collect.map.hash.HashLongObjMaps;
import ib053.core.activities.LevelUpActivity;
import ib053.core.activities.LocationActivity;
import ib053.frontend.Frontend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;

/**
 * Stores the game's state and manages its lifecycle and boilerplate.
 */
public final class GameCore {

    private static final Logger LOG = LoggerFactory.getLogger(GameCore.class);

    private final Frontend[] frontends;
    private final ScheduledExecutorService eventLoop = new ScheduledThreadPoolExecutor(1) {
        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);

            if (t == null && r instanceof Future) {
                try {
                    ((Future) r).get();
                } catch (Throwable e) {
                    t = e;
                }
            }

            if (t != null) {
                LOG.error("EventLoop item crashed", t);
            }
        }
    };

    private static final long STARTING_LOCATION_ID = 0;

    private static final String LOCATION_FILE_NAME = "locations.json";
    private final LongObjMap<Location> worldLocations;
    private static final String ITEM_FILE_NAME = "items.json";
    private final LongObjMap<Item> worldItems;
    private static final String ENEMY_FILE_NAME = "enemies.json";
    private final LongObjMap<Enemy> worldEnemies;

    private final File stateFolder;
    private static final String PLAYER_FILE_NAME = "players.json";
    private final LongObjMap<Player> players = HashLongObjMaps.newMutableMap();
    private final LongObjMap<List<Player>> playersInLocation;

    private static final String ACTIVITY_FILE_NAME = "activities.json";
    final ActivityCache activityCache;

    /** Creates the game core and starts the event loop, starting the game.
     * Handles the initialization of front-ends. */
    public GameCore(File resourceFolder, File stateFolder, Frontend...frontends) {
        this.frontends = frontends;
        this.stateFolder = stateFolder;
        final File locationFile = new File(resourceFolder, LOCATION_FILE_NAME);
        final File itemFile = new File(resourceFolder, ITEM_FILE_NAME);
        final File enemyFile = new File(resourceFolder, ENEMY_FILE_NAME);
        final File playerFile = new File(stateFolder, PLAYER_FILE_NAME);
        final File activityFile = new File(stateFolder, ACTIVITY_FILE_NAME);

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

            LOG.info("Loaded {} locations from {}", worldLocations.size(), locationFile);
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
            LOG.info("Loaded {} items from {}", worldItems.size(), itemFile);
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
            LOG.info("Loaded {} enemies from {}", worldEnemies.size(), enemyFile);
        }

        { // Load activities
            activityCache = new ActivityCache(activityFile, this);
            activityCache.load();
        }

        { // Load players
            if (playerFile.exists()) {
                final JsonValue playerArray = new JsonReader().parse(playerFile);
                assert playerArray.isArray();

                for (JsonValue playerJson : playerArray) {
                    final Player player;
                    try {
                        player = Player.read(playerJson, this);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid save file", e);
                    }
                    final Player oldPlayer = players.put(player.getId(), player);
                    if (oldPlayer != null) {
                        throw new IllegalArgumentException("Invalid save file, two players ("+player.getName()+" and "+oldPlayer.getName()+") share the same ID "+ player.getId()+"!");
                    }
                }
                LOG.info("Loaded {} players from {}", players.size(), playerFile);
            } else {
                LOG.info("Loaded no players, no player file at {}", playerFile);
            }
        }

        LOG.info("Starting with {} frontend(s)", frontends.length);

        // Initialize this in the event loop, so that nothing may disrupt the initialization
        eventLoop.execute(() -> {
            for (Frontend frontend : frontends) {
                LOG.info("Initializing frontend: {}", frontend.getClass().getSimpleName());
                frontend.initialize(this);
            }

            for (Frontend frontend : frontends) {
                frontend.begin();
            }

            LOG.info("Initialization done");
        });
    }

    /** Posts given runnable into event loop to be run in given amount of time */
    public void schedule(Runnable runnable, long delay, TimeUnit unit) {
        eventLoop.schedule(runnable, delay, unit);
    }

    /** Creates a whole new Player, with given name.
     * Call initNewPlayer when ready to start playing.
     * @return new player or null if the name is already taken. */
    public Player createNewPlayer(String name) {
        assert name != null;
        long id = 1; // Sequential player IDs for now
        //Check if name is not taken
        for (Player player : players.values()) {
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

        LOG.info("Created new player named {} with id {}", name, id);
        final Player player = new Player(this, id, name, attributes);
        players.put(id, player);
        return player;
    }

    public void initNewPlayer(Player player) {
        assert player.currentActivity == null;
        assert player.location == null;

        player.setEquipment(worldItems.get(1));//Give player dull knife
        changePlayerLocation(player, worldLocations.get(STARTING_LOCATION_ID));
        changePlayerActivityToDefault(player);
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
    private void changePlayerActivityNoCheck(Player player, ActivityBase newActivity) {
        assert player != null;
        assert newActivity != null;

        if (player.currentActivity != null) {
            player.currentActivity.endActivity(player);
            player.currentActivity.engagedPlayers.remove(player);
        }

        player.currentActivity = newActivity;
        player.currentActivity.engagedPlayers.add(player);
        newActivity.beginActivity(player);

        notifyPlayerActivityChanged(player);
    }

    public void changePlayerActivity(Player player, ActivityBase customActivity) {
        assert customActivity != null;
        assert activityCache.getActivityType(customActivity.getClass()) == ActivityType.CUSTOM_ACTIVITY;

        activityCache.ensureInitialized(customActivity);
        changePlayerActivityNoCheck(player, customActivity);
    }

    /** Change player's activity into singleton activity */
    public void changePlayerActivity(Player player, Class<? extends ActivityBase> singletonActivityType) {
        changePlayerActivityNoCheck(player, activityCache.getSingletonActivity(singletonActivityType));
    }

    /** Change player's activity into per-location activity */
    public void changePlayerActivity(Player player, Class<? extends ActivityBase> locationActivityType, Location location) {
        changePlayerActivityNoCheck(player, activityCache.getLocationActivity(locationActivityType, location));
    }

    /** Changes the player's activity to default activity (LocationActivity, usually) */
    public void changePlayerActivityToDefault(Player player) {
        changePlayerActivity(player, LocationActivity.class, player.getLocation());
    }

    /** Called when player has new actions to choose from.
     * Delegates this notification to frontends.
     * Called automatically when changing activity. */
    public void notifyPlayerActivityChanged(Player player) {
        for (Frontend frontend : frontends) {
            frontend.playerActivityChanged(player);
        }
    }

    /** Called by {@link ActivityBase} when player has witnessed an Event.
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
            changePlayerActivity(player, LevelUpActivity.class);
        } else {
            notifyPlayerEventHappened(player, new Event("You have gained "+experiencePoints+" xp ("+(player.experience * 100 / xpToNextLevel)+"% to next level)"));
        }
    }

    /** @return Player previously created with given ID, or null if no such player exists. */
    public Player findPlayer(long playerId) {
        return players.get(playerId);
    }

    public Location findLocation(long locationId) {
        return worldLocations.get(locationId);
    }

    public Item findItem(long itemId) {
        return worldItems.get(itemId);
    }

    public Enemy findEnemy(long enemyId) {
        return worldEnemies.get(enemyId);
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

        { // Save players
            try {
                final File playerFile = new File(stateFolder, PLAYER_FILE_NAME);
                PersistenceUtil.saveJsonSecurely(playerFile, json -> {
                    json.writeArrayStart();
                    for (Player player : players.values()) {
                        Player.write(json, player);
                    }
                    json.writeArrayEnd();
                });
                LOG.info("{} players saved to {}", players.size(), playerFile);
            } catch (PersistenceUtil.PersistenceException e) {
                LOG.error("Failed to save players file", e);
            }
        }

        // Save activities
        activityCache.save();
    }
}

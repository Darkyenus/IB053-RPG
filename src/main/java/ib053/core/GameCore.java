package ib053.core;

import ib053.core.activities.DefaultActivity;
import ib053.frontend.Frontend;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

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

    private final List<Player> players = new ArrayList<>();
    private final List<Place> places = new ArrayList<>();
    private Place startingPlace;

    /** Creates the game core and starts the event loop, starting the game.
     * Handles the initialization of front-ends. */
    public GameCore(Frontend...frontends) {
        this.frontends = frontends;

        // Temporary world creation
        final Place newbieHill = new Place(this, 0, "Newbie Hill", "Smell of freshly cut grass and vistas of fields, " +
                "mountains on the horizon and a small village in a nearby vale.");
        final Place wensleyvale = new Place(this, 1, "Wensleyvale Village", "Small village fortified with a rotten palisade.");
        final Place wensleyvaleForest = new Place(this, 2, "Wensleyvale Forest", "Lush forest, just beyond the Wensleyvale creek. Mostly safe, but still be careful.");

        newbieHill.directions.put("Downhill towards the village", wensleyvale);
        wensleyvale.directions.put("Up the Newbie hill", newbieHill);
        wensleyvale.directions.put("Over the creek into the forest", wensleyvaleForest);
        wensleyvaleForest.directions.put("Over the creek back to the village", wensleyvale);

        places.add(newbieHill);
        places.add(wensleyvale);
        places.add(wensleyvaleForest);

        startingPlace = newbieHill;

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

        final Player player = new Player(this, id, name);
        players.add(player);
        return player;
    }

    public void initNewPlayer(Player player) {
        assert player.currentActivity == null;
        assert player.location == null;
        movePlayer(player, startingPlace);
        changePlayerActivity(player, new DefaultActivity(startingPlace));
    }

    public void movePlayer(Player player, Place toPlace) {
        assert player != null;
        assert toPlace != null;

        if (player.location != null) {
            player.location.presentPlayers.remove(player);
        }
        player.location = toPlace;
        toPlace.presentPlayers.add(player);
    }

    /**
     * Change activity that player is engaged in.
     * This is the only way to change player's activity.
     *
     * Calls respective Activity.begin/endActivity() and {@link #notifyPlayerActionsChanged(Player)}.
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

        notifyPlayerActionsChanged(player);
    }

    /** Called when player has new actions to choose from.
     * Delegates this notification to frontends.
     * Called automatically when changing activity. */
    private void notifyPlayerActionsChanged(Player player) {
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

    public void setActivityActions(Activity activity, List<Action> actions) {
        activity.actions.clear();
        activity.actions.addAll(actions);
        for (Player engagedPlayer : activity.engagedPlayers) {
            notifyPlayerActionsChanged(engagedPlayer);
        }
    }

    public void addActivityAction(Activity activity, Action action) {
        activity.actions.add(action);
        for (Player engagedPlayer : activity.engagedPlayers) {
            notifyPlayerActionsChanged(engagedPlayer);
        }
    }

    public boolean removeActivityAction(Activity activity, Action action) {
        if(activity.actions.remove(action)) {
            for (Player engagedPlayer : activity.engagedPlayers) {
                notifyPlayerActionsChanged(engagedPlayer);
            }
            return true;
        }
        return false;
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

}

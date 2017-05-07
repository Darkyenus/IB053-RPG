package ib053.core.activities;

import ib053.core.*;

import java.util.function.ObjLongConsumer;

/**
 *
 */
public final class LocationActivity extends Activity {

    private final Location location;

    public LocationActivity(Location location) {
        this.location = location;
    }

    @Override
    public void initialize() {
        final GameCore core = getCore();
        core.addActivityAction(this, new Action(this, null, "Look around") {

            @Override
            protected void performAction(Player player) {
                player.getCore().notifyPlayerEventHappened(player, new Event("You see absolutely nothing interesting."));
            }
        });
        location.directions.forEach((ObjLongConsumer<? super String>) (message, place) -> {
            core.addActivityAction(this, new TravelAction(message, core.worldLocations.get(place)));
        });
    }

    @Override
    public void beginActivity(Player player) {

    }

    @Override
    public String getDescription(Player player) {
        final Location location = player.getLocation();
        return "You are in "+location.name+": "+location.description;
    }

    @Override
    public void endActivity(Player player) {

    }


    private final class TravelAction extends Action {

        private final Location to;

        private TravelAction(String travelMessage, Location to) {
            super(LocationActivity.this, "Travel", travelMessage);
            this.to = to;
        }

        @Override
        protected void performAction(Player player) {
            final GameCore core = player.getCore();
            core.movePlayer(player, to);
            core.changePlayerActivity(player, new LocationActivity(to));
        }
    }
}

package ib053.core.activities;

import ib053.core.*;

/**
 *
 */
public final class DefaultActivity extends Activity {

    private final Place place;

    public DefaultActivity(Place place) {
        this.place = place;
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
        place.getDirections().forEach((message, place) -> {
            core.addActivityAction(this, new TravelAction(message, place));
        });
    }

    @Override
    public void beginActivity(Player player) {

    }

    @Override
    public String getDescription(Player player) {
        final Place location = player.getLocation();
        return "You are in "+location.getName()+": "+location.getFlavorText();
    }

    @Override
    public void endActivity(Player player) {

    }


    private final class TravelAction extends Action {

        private final Place to;

        private TravelAction(String travelMessage, Place to) {
            super(DefaultActivity.this, "Travel", travelMessage);
            this.to = to;
        }

        @Override
        protected void performAction(Player player) {
            final GameCore core = player.getCore();
            core.movePlayer(player, to);
            core.changePlayerActivity(player, new DefaultActivity(to));
        }
    }
}

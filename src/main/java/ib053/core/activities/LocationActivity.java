package ib053.core.activities;

import ib053.core.*;

import java.util.function.ObjLongConsumer;

/**
 * Default activity for a location.
 */
@Activity(ActivityType.PER_LOCATION_ACTIVITY)
public final class LocationActivity extends ActivityBase {

    private final Location location;

    private LocationActivity(Location location) {
        this.location = location;
    }

    @Override
    public void initialize() {
        final GameCore core = getCore();
        core.addActivityAction(this, new Action(this, null, "Myself") {

            @Override
            protected void performAction(Player player) {
                final StringBuilder sb = new StringBuilder();
                sb.append("You are ").append(player.getName()).append("\n");
                sb.append("LVL: ").append(player.get(Attribute.LEVEL)).append(" EXP: [");
                final int xpToNextLevel = player.getXpToNextLevel();
                final int expBarUnits = 20;
                final int expBarUnitsFilled = Math.round(player.getExperience() * expBarUnits / xpToNextLevel);
                for (int i = 0; i < expBarUnits; i++) {
                    if (i < expBarUnitsFilled) {
                        sb.append('|');
                    } else {
                        sb.append(' ');
                    }
                }
                sb.append("] ").append(player.getExperience()).append(" / ").append(xpToNextLevel).append('\n');

                final int health = player.health;
                final int maxHealth = player.getMaxHealth();
                sb.append(health).append("/").append(maxHealth).append(" HP: ");
                if (health >= maxHealth) {
                    sb.append("You are feeling great!");
                } else if (health > (maxHealth/4) * 3) {
                    sb.append("You have some scratches");
                } else if (health > (maxHealth / 2)) {
                    sb.append("You are injured");
                } else if (health > (maxHealth / 4)) {
                    sb.append("You are badly injured");
                } else {
                    sb.append("You are barely standing");
                }
                sb.append('\n');
                //Equipment
                for (Item.ItemType itemType : Item.ItemType.VALUES) {
                    if (!itemType.canEquip) continue;
                    sb.append(itemType.name).append(": ");
                    final Item equipment = player.getEquipment(itemType);
                    if (equipment != null) {
                        equipment.toString(sb, "\n\t");
                        sb.append('\n');
                    } else {
                        sb.append(" none\n");
                    }
                }

                player.getCore().notifyPlayerEventHappened(player, new Event(sb.toString()));
            }
        });
        location.directions.forEach((ObjLongConsumer<? super String>) (message, place) -> {
            core.addActivityAction(this, new TravelAction(message, core.findLocation(place)));
        });
        if (location.hasEnemies()) {
            core.addActivityAction(this, new Action(this, "Fight", "Look for something to kill") {
                @Override
                protected void performAction(Player player) {
                    getCore().changePlayerActivity(player, new FightingActivity(getCore().findEnemy(location.selectEnemyToFight()), player));
                }
            });
        }
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
            core.changePlayerLocation(player, to);
            final long enemyToFightOnEntry = to.selectEnemyToFightOnEntry();
            if (enemyToFightOnEntry != -1) {
                core.notifyPlayerEventHappened(player, new Event("Ambush!"));
                core.changePlayerActivity(player, new FightingActivity(core.findEnemy(enemyToFightOnEntry), player));
            } else {
                core.changePlayerActivityToDefault(player);
            }
        }
    }
}

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

        action("location.myself", "Myself" , player -> {
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
        });

        location.directions.forEach((ObjLongConsumer<? super String>) (message, place) -> {
            action("location-"+location.id+".travel."+place,
                    "Travel", message,
                    player -> {
                final GameCore core = player.getCore();
                final Location to = core.getLocation(place);

                core.changePlayerLocation(player, to);
                final long enemyToFightOnEntry = to.selectEnemyToFightOnEntry();
                if (enemyToFightOnEntry != -1) {
                    core.notifyPlayerEventHappened(player, new Event("Ambush!"));
                    core.changePlayerActivity(player, new FightingActivity(core.getEnemy(enemyToFightOnEntry), player));
                } else {
                    core.changePlayerActivityToDefault(player);
                }
            });
        });

        if (location.hasEnemies()) {
            action("location-"+location.id+".fight.look-for-enemies",
                    "Fight", "Look for something to kill",
                    player -> {
                        core().changePlayerActivity(player, new FightingActivity(core().getEnemy(location.selectEnemyToFight()), player));
                    });
        }
    }

    @Override
    public String getDescription(Player player) {
        final Location location = player.getLocation();
        return "You are in "+location.name+": "+location.description;
    }
}

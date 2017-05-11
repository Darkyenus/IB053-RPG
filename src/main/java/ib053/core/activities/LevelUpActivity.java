package ib053.core.activities;

import ib053.core.*;

/**
 * Manages level up
 */
public class LevelUpActivity extends Activity {

    public static final LevelUpActivity INSTANCE = new LevelUpActivity();

    private static final int VIRTUE_POINTS_PER_LEVEL = 2;

    private LevelUpActivity() {
    }

    @Override
    public void initialize() {
        for (Attribute attribute : Attribute.VALUES) {
            if (attribute.type != Attribute.AttributeType.VIRTUE || attribute == Attribute.LUCK) continue;

            final Action addPoint = new Action(this, "Add virtue point to", attribute.shortName) {

                @Override
                protected void performAction(Player player) {
                    if (player.virtuePoints > 0) {//Just in case
                        player.attributes.add(attribute, 1);
                        player.virtuePoints -= 1;
                    }

                    if (player.virtuePoints <= 0) {
                        getCore().changePlayerActivityToDefault(player);
                    } else {
                        getCore().notifyPlayerActivityChanged(player);
                    }
                }
            };
            getCore().addActivityAction(this, addPoint);
        }
    }

    @Override
    public void beginActivity(Player player) {
        final int xpToNextLevel = player.getXpToNextLevel();
        if (player.experience < xpToNextLevel) {
            player.getCore().changePlayerActivityToDefault(player);
        } else {
            player.experience -= xpToNextLevel;
            final int level = player.attributes.add(Attribute.LEVEL, 1);
            player.virtuePoints += VIRTUE_POINTS_PER_LEVEL;
            player.getCore().notifyPlayerEventHappened(player, new Event("🎉 You have reached level "+level+"!"));
        }
    }

    @Override
    public String getDescription(Player player) {
        final StringBuilder sb = new StringBuilder();
        sb.append("You have ").append(player.virtuePoints).append(" virtue points and:\n");
        for (Attribute attribute : Attribute.VALUES) {
            if (attribute.type != Attribute.AttributeType.VIRTUE || attribute == Attribute.LUCK) continue;
            sb.append(attribute.shortName).append(": ").append(player.attributes.get(attribute)).append('\n');
        }
        return sb.toString();
    }

    @Override
    public void endActivity(Player player) {

    }
}

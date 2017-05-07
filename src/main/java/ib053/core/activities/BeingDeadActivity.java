package ib053.core.activities;

import com.koloboke.collect.map.LongLongMap;
import com.koloboke.collect.map.hash.HashLongLongMaps;
import ib053.core.Action;
import ib053.core.Activity;
import ib053.core.Event;
import ib053.core.Player;

/**
 * Activity for people that are dead.
 */
public class BeingDeadActivity extends Activity {

    public static final BeingDeadActivity INSTANCE = new BeingDeadActivity();

    private final LongLongMap playerEnterTime = HashLongLongMaps.newMutableMap();
    // Eternity time in MS (1 minute for now, but make bigger)
    private static final long ETERNITY_MS = 1000L * 60L;

    @Override
    public void initialize() {
        //TODO Fix this when XP implemented
        getCore().addActivityAction(this, new Action(this, "Limbo", "Sell your soul (lose HALF_OF_PROGRESS_TO_NEXT_LEVEL XP)") {
            @Override
            protected void performAction(Player player) {
                //TODO Lose XP
                resurrect(player);
            }
        });

        getCore().addActivityAction(this, new Action(this, "Limbo", "Wait for an eternity") {
            @Override
            protected void performAction(Player player) {
                final long freedIn = (playerEnterTime.get(player.getId()) + ETERNITY_MS) - System.currentTimeMillis();
                if (freedIn < 0) {
                    getCore().notifyPlayerEventHappened(player, new Event("At last, after an eternity, you are free"));
                    resurrect(player);
                } else {
                    final long remainingMinutes = (freedIn / 60000)+1;
                    final StringBuilder sb = new StringBuilder("No, you still have to wait for an eternity! (which is estimated to be ");
                    if (remainingMinutes <= 1) {
                        sb.append("very soon)");
                    } else {
                        sb.append("in around ").append(remainingMinutes).append(" minutes)");
                    }
                    getCore().notifyPlayerEventHappened(player, new Event(sb.toString()));
                }
            }
        });
    }

    private void resurrect(Player player) {
        player.health = Math.max(1, player.getMaxHealth() / 4);
        getCore().changePlayerLocation(player, getCore().worldLocations.get(player.getLocation().graveyardId));
        getCore().changePlayerActivityToDefault(player);
    }

    @Override
    public void beginActivity(Player player) {
        playerEnterTime.put(player.getId(), System.currentTimeMillis());
    }

    @Override
    public String getDescription(Player player) {
        return "You are dead. Sell your soul or wait for an eternity";
    }

    @Override
    public void endActivity(Player player) {
        playerEnterTime.remove(player.getId());
    }
}

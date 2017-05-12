package ib053.core.activities;

import com.esotericsoftware.jsonbeans.Json;
import com.esotericsoftware.jsonbeans.JsonValue;
import com.koloboke.collect.map.LongLongMap;
import com.koloboke.collect.map.hash.HashLongLongMaps;
import com.koloboke.function.LongLongConsumer;
import ib053.core.*;
import ib053.util.SerializationConstructor;

/**
 * Activity for people that are dead.
 */
@Activity(ActivityType.SINGLETON_ACTIVITY)
public final class BeingDeadActivity extends ActivityBase implements ActivityBase.Serializable {

    private final LongLongMap playerEnterTime = HashLongLongMaps.newMutableMap();
    // Eternity time in MS (1 minute for now, but make bigger)
    private static final long ETERNITY_MS = 1000L * 60L;

    @SerializationConstructor
    private BeingDeadActivity() {
        action("being-dead.limbo.sell-your-soul",
                "Limbo", "Sell your soul for all your experience! (on this level)" ,
                player -> {
                    if (player.experience <= 0) {
                        core().notifyPlayerEventHappened(player, new Event("FOOL! YOU DON'T HAVE ANY EXPERIENCE! YOU HAVE NOTHING TO OFFER!"));
                    } else {
                        player.experience = 0;
                        resurrect(player);
                    }
                });

        action("being-dead.limbo.wait-for-eternity",
                "Limbo", "Wait for an eternity", player -> {
                    final long freedIn = (playerEnterTime.get(player.getId()) + ETERNITY_MS) - System.currentTimeMillis();
                    if (freedIn < 0) {
                        core().notifyPlayerEventHappened(player, new Event("At last, after an eternity, you are free"));
                        resurrect(player);
                    } else {
                        final long remainingMinutes = (freedIn / 60000)+1;
                        final StringBuilder sb = new StringBuilder("No, you still have to wait for an eternity! (which is estimated to be ");
                        if (remainingMinutes <= 1) {
                            sb.append("very soon)");
                        } else {
                            sb.append("in around ").append(remainingMinutes).append(" minutes)");
                        }
                        core().notifyPlayerEventHappened(player, new Event(sb.toString()));
                    }
                });
    }

    private void resurrect(Player player) {
        player.health = Math.max(1, player.getMaxHealth() / 4);
        core().changePlayerLocation(player, core().getLocation(player.getLocation().graveyardId));
        core().changePlayerActivityToDefault(player);
    }

    @Override
    public void beginActivity(Player player) {
        playerEnterTime.put(player.getId(), System.currentTimeMillis());
    }

    @Override
    public String getDescription(Player player) {
        return "You are dead. Sell your soul (if you have any) or wait for an eternity";
    }

    @Override
    public void endActivity(Player player) {
        playerEnterTime.remove(player.getId());
    }

    @Override
    public void write(Json json) {
        json.writeObjectStart("playerEnterTime");
        playerEnterTime.forEach((LongLongConsumer) (player, time) -> {
            json.writeValue(Long.toString(player), time, long.class);
        });
        json.writeObjectEnd();
    }

    @Override
    public void read(GameCore core, JsonValue json) {
        for (JsonValue enterTime : json.get("playerEnterTime")) {
            playerEnterTime.put(Long.parseLong(enterTime.name()), enterTime.asLong());
        }
    }
}

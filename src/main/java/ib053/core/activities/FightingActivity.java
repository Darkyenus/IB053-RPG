package ib053.core.activities;

import com.esotericsoftware.jsonbeans.Json;
import com.esotericsoftware.jsonbeans.JsonValue;
import ib053.core.*;
import ib053.util.SerializationConstructor;

import java.util.concurrent.TimeUnit;

/**
 * Activity of someone that is fighting an enemy.
 */
@Activity(ActivityType.CUSTOM_ACTIVITY)
public final class FightingActivity extends ActivityBase implements ActivityBase.Serializable {

    private Enemy enemy;
    private int enemyHealth;
    private int enemyInitiative;

    private Player player;
    private int playerInitiative;

    @SerializationConstructor
    private FightingActivity() {
        action("fighting.combat.attack", "Combat", "Attack!", player -> {
            allActionsSetEnabled(false);
            enemyInitiative += getEnemyInitiative();

            final int playerAttackDamage = resolveAttack(player, enemy);
            if (playerAttackDamage > 0) {
                enemyHealth -= playerAttackDamage;
                core().notifyPlayerEventHappened(player, new Event("You attack for "+playerAttackDamage+"!"));
            } else {
                core().notifyPlayerEventHappened(player, new Event("You attack, but miss!"));
            }

            if (enemyHealth <= 0) {
                // Game over
                final int killExperience = enemy.killExperience;
                core().notifyPlayerEventHappened(player, new Event(enemy.name+" lies defeated!"));

                core().changePlayerActivityToDefault(player);
                core().giveExperience(player, killExperience);
            } else {
                nextTurn();
            }
        }).setEnabled(false);

        action("fighting.combat.flee", "Combat", "Run away!", player -> {
            if (RandomUtil.chooseFirst(player.get(Attribute.LUCK), enemy.get(Attribute.LUCK))) {
                // Flee successful
                core().notifyPlayerEventHappened(player, new Event("You flee to safety!"));
                core().changePlayerActivityToDefault(player);
            } else {
                core().notifyPlayerEventHappened(player, new Event("Can't run away!"));
                enemyInitiative += getEnemyInitiative();
                nextTurn();
            }
        }).setEnabled(false);
    }

    @Override
    public void write(Json json) {
        json.writeValue("enemyId", enemy.id, long.class);
        json.writeValue("enemyHealth", enemyHealth, int.class);
        json.writeValue("enemyInitiative", enemyInitiative, int.class);
        json.writeValue("playerId", player.getId(), long.class);
        json.writeValue("playerInitiative", playerInitiative, int.class);
    }

    @Override
    public void read(GameCore core, JsonValue json) {
        enemy = core.getEnemy(json.getLong("enemyId"));
        enemyHealth = json.getInt("enemyHealth");
        enemyInitiative = json.getInt("enemyInitiative");
        player = core.getPlayer(json.getInt("playerId"));
        playerInitiative = json.getInt("playerInitiative");

        tryEnemyTurn();//TODO Proper task serialization!!!
    }

    FightingActivity(Enemy enemy, Player player) {
        this();
        this.enemy = enemy;
        this.player = player;
    }

    private void nextTurn() {
        core().schedule(FightingActivity.this::tryEnemyTurn, 1, TimeUnit.SECONDS);
    }

    private int getEnemyInitiative() {
        int initiative = enemy.get(Attribute.AGILITY);
        if (enemy.luckCheck(player)) {
            initiative += initiative / 4;
        }
        return initiative;
    }

    private int getPlayerInitiative() {
        int initiative = player.get(Attribute.AGILITY);
        if (player.luckCheck(enemy)) {
            initiative += initiative / 4;
        }
        return initiative;
    }

    private int resolveAttack(AttributeHolder attacker, AttributeHolder defender) {
        final int attackerDex = attacker.get(Attribute.DEXTERITY);
        boolean hit = RandomUtil.chooseFirst(attackerDex + attackerDex / 3, defender.get(Attribute.DEXTERITY));
        boolean critical = false;

        if (attacker.luckCheck(defender)) {
            if (hit) {
                critical = true;
            } else {
                hit = true;
            }
        }

        if (!hit) {
            return 0;
        }

        final int baseWeaponDamage = attacker.get(Attribute.DAMAGE);
        final int weaponDamageSpread = attacker.get(Attribute.DAMAGE_SPREAD);
        final float weaponDamage = Math.max(
                1f,
                baseWeaponDamage + RandomUtil.clamp((float)RandomUtil.RANDOM.nextGaussian() / 1.5f, -1f, 1f) * weaponDamageSpread);
        final float strengthModifier = (float) (Math.log10(attacker.get(Attribute.STRENGTH) + 1.0) + 1.0);
        final float totalDamage = weaponDamage * strengthModifier;

        if (critical) {
            return Math.round(totalDamage * 2f);
        } else {
            return Math.round(totalDamage);
        }
    }

    private void tryEnemyTurn() {
        if (enemyInitiative > playerInitiative
                || (enemyInitiative == playerInitiative && RandomUtil.chooseFirst(
                enemy.get(Attribute.LUCK), player.get(Attribute.LUCK)))) {

            allActionsSetEnabled(false);
            playerInitiative += getPlayerInitiative();

            // Enemy turn
            final int enemyAttackDamage = resolveAttack(enemy, player);
            if (enemyAttackDamage > 0) {
                player.health -= enemyAttackDamage;
                core().notifyPlayerEventHappened(player, new Event(enemy.name+" attacks for "+enemyAttackDamage+"!"));
                //if (player.health > 0) getCore().notifyPlayerEventHappened(player, new Event("You are down to "+player.health+"/"+player.getMaxHealth()+" HP!"));
            } else {
                core().notifyPlayerEventHappened(player, new Event(enemy.name+" attacks, but misses!"));
            }

            if (player.health <= 0) {
                // Game over
                core().notifyPlayerEventHappened(player, new Event("💀 You died"));
                core().changePlayerActivity(player, BeingDeadActivity.class);
            }

            nextTurn();
        } else {
            // Player turn
            allActionsSetEnabled(true);
        }
    }

    @Override
    public void beginActivity(Player player) {
        if (player != this.player) throw new IllegalArgumentException("Only "+this.player+" can participate in this fight!");

        this.enemyHealth = enemy.getMaxHealth();
        enemyInitiative = getEnemyInitiative();
        playerInitiative = getPlayerInitiative();

        core().notifyPlayerEventHappened(player, new Event("A fight with "+enemy.name+"!\n"+enemy.description));
        tryEnemyTurn();
    }

    @Override
    public String getDescription(Player player) {
        return "A fight against "+enemy.name+"!\nYou: "+player.health+"/"+player.getMaxHealth()+" HP\n"+enemy.name+" "+enemyHealth+"/"+enemy.getMaxHealth()+" HP";
    }
}

package ib053.core.activities;

import ib053.core.*;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public final class FightingActivity extends Activity {

    private final Enemy enemy;
    private int enemyHealth;
    private int enemyInitiative;

    private final Player player;
    private int playerInitiative;

    private final Action attackAction, fleeAction;
    private final Runnable tryEnemyTurn;

    FightingActivity(Enemy enemy, Player player) {
        this.enemy = enemy;
        this.player = player;

        attackAction = new Action(this, "Combat", "Attack!") {
            @Override
            protected void performAction(Player player) {
                getCore().clearActivityActions(FightingActivity.this);
                enemyInitiative += getEnemyInitiative();

                final int playerAttackDamage = resolveAttack(player, enemy);
                if (playerAttackDamage > 0) {
                    enemyHealth -= playerAttackDamage;
                    getCore().notifyPlayerEventHappened(player, new Event("You attack for "+playerAttackDamage+"!"));
                    //if (enemyHealth > 0) getCore().notifyPlayerEventHappened(player, new Event(enemy.name+" is down to "+enemyHealth+"/"+enemy.getMaxHealth()+" HP!"));
                } else {
                    getCore().notifyPlayerEventHappened(player, new Event("You attack, but miss!"));
                }

                if (enemyHealth <= 0) {
                    // Game over
                    final int killExperience = enemy.killExperience;
                    getCore().notifyPlayerEventHappened(player, new Event(enemy.name+" lies defeated!"));

                    getCore().changePlayerActivityToDefault(player);
                    getCore().giveExperience(player, killExperience);
                } else {
                    nextTurn();
                }
            }
        };

        fleeAction = new Action(this, "Combat", "Run away!") {
            @Override
            protected void performAction(Player player) {
                if (RandomUtil.chooseFirst(player.get(Attribute.LUCK), enemy.get(Attribute.LUCK))) {
                    // Flee successful
                    getCore().notifyPlayerEventHappened(player, new Event("You flee to safety!"));
                    getCore().changePlayerActivityToDefault(player);
                } else {
                    getCore().notifyPlayerEventHappened(player, new Event("Can't run away!"));
                    enemyInitiative += getEnemyInitiative();
                    nextTurn();
                }
            }
        };

        tryEnemyTurn = () -> {
            if (enemyInitiative > playerInitiative
                    || (enemyInitiative == playerInitiative && RandomUtil.chooseFirst(
                    enemy.get(Attribute.LUCK), player.get(Attribute.LUCK)))) {

                getCore().clearActivityActions(this);
                playerInitiative += getPlayerInitiative();

                // Enemy turn
                final int enemyAttackDamage = resolveAttack(enemy, player);
                if (enemyAttackDamage > 0) {
                    player.health -= enemyAttackDamage;
                    getCore().notifyPlayerEventHappened(player, new Event(enemy.name+" attacks for "+enemyAttackDamage+"!"));
                    //if (player.health > 0) getCore().notifyPlayerEventHappened(player, new Event("You are down to "+player.health+"/"+player.getMaxHealth()+" HP!"));
                } else {
                    getCore().notifyPlayerEventHappened(player, new Event(enemy.name+" attacks, but misses!"));
                }

                if (player.health <= 0) {
                    // Game over
                    getCore().notifyPlayerEventHappened(player, new Event("💀 You died"));
                    getCore().changePlayerActivity(player, BeingDeadActivity.INSTANCE);
                }

                nextTurn();
            } else {
                // Player turn
                getCore().setActivityActions(this, attackAction, fleeAction);
            }
        };
    }

    private void nextTurn() {
        getCore().eventLoop.schedule(FightingActivity.this.tryEnemyTurn, 1, TimeUnit.SECONDS);
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

    @Override
    public void initialize() {
        this.enemyHealth = enemy.getMaxHealth();
        enemyInitiative = getEnemyInitiative();
        playerInitiative = getPlayerInitiative();

        getCore().notifyPlayerEventHappened(player, new Event("A fight with "+enemy.name+"!\n"+enemy.description));
        tryEnemyTurn.run();
    }

    @Override
    public void beginActivity(Player player) {
        if (player != this.player) throw new IllegalArgumentException("Only "+this.player+" can participate in this fight!");
    }

    @Override
    public String getDescription(Player player) {
        return "A fight against "+enemy.name+"!\nYou: "+player.health+"/"+player.getMaxHealth()+" HP\n"+enemy.name+" "+enemyHealth+"/"+enemy.getMaxHealth()+" HP";
    }

    @Override
    public void endActivity(Player player) {
        if (player != this.player) throw new IllegalArgumentException("Only "+this.player+" can participate in this fight!");
    }
}

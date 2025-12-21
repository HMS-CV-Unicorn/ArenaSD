package com.saratoga.snd;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configuration holder for SearchAndDestroy.
 */
public class Config {

    private final SearchAndDestroy plugin;

    // Round settings
    private int maxRounds;
    private int roundsToWin;
    private int swapSidesAfter;
    private int roundTimeLimit;

    // Bomb settings
    private int plantTime;
    private int defuseTime;
    private int explosionTime;
    private Material bombItem;

    // Transition settings
    private int lobbyWaitTime;
    private int countdownTime;

    // Team names
    private String redTeamName;
    private String blueTeamName;

    public Config(SearchAndDestroy plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // Round settings
        this.maxRounds = config.getInt("rounds.max", 12);
        this.roundsToWin = config.getInt("rounds.rounds-to-win", 7);
        this.swapSidesAfter = config.getInt("rounds.swap-sides-after", 6);
        this.roundTimeLimit = config.getInt("rounds.time-limit-seconds", 120);

        // Bomb settings
        this.plantTime = config.getInt("bomb.plant-time-seconds", 3);
        this.defuseTime = config.getInt("bomb.defuse-time-seconds", 5);
        this.explosionTime = config.getInt("bomb.explosion-time-seconds", 40);
        this.bombItem = Material.matchMaterial(config.getString("bomb.item", "TNT"));
        if (bombItem == null)
            bombItem = Material.TNT;

        // Transition settings
        this.lobbyWaitTime = config.getInt("transition.lobby-wait-seconds", 5);
        this.countdownTime = config.getInt("transition.countdown-seconds", 3);

        // Team names
        this.redTeamName = config.getString("teams.red", "Red Team");
        this.blueTeamName = config.getString("teams.blue", "Blue Team");
    }

    public int getMaxRounds() {
        return maxRounds;
    }

    public int getRoundsToWin() {
        return roundsToWin;
    }

    public int getSwapSidesAfter() {
        return swapSidesAfter;
    }

    public int getRoundTimeLimit() {
        return roundTimeLimit;
    }

    public int getPlantTime() {
        return plantTime;
    }

    public int getDefuseTime() {
        return defuseTime;
    }

    public int getExplosionTime() {
        return explosionTime;
    }

    public Material getBombItem() {
        return bombItem;
    }

    public int getLobbyWaitTime() {
        return lobbyWaitTime;
    }

    public int getCountdownTime() {
        return countdownTime;
    }

    public String getRedTeamName() {
        return redTeamName;
    }

    public String getBlueTeamName() {
        return blueTeamName;
    }
}

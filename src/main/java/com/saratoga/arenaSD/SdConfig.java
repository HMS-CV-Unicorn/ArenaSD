package com.saratoga.arenaSD;

import org.battleplugins.arena.config.ArenaOption;

import java.time.Duration;

/**
 * Configuration class for Search and Destroy game mode.
 */
public class SdConfig {

    @ArenaOption(name = "plant-time", description = "How long it takes to plant the bomb.", required = true)
    private Duration plantTime;

    @ArenaOption(name = "detonation-time", description = "How long until the bomb detonates after being planted.", required = true)
    private Duration detonationTime;

    @ArenaOption(name = "defuse-time", description = "How long it takes to defuse the bomb.", required = true)
    private Duration defuseTime;

    @ArenaOption(name = "round-time", description = "Maximum time per round.", required = true)
    private Duration roundTime;

    @ArenaOption(name = "rounds-to-win", description = "Number of rounds needed to win the match.", required = true)
    private int roundsToWin;

    @ArenaOption(name = "swap-sides-after", description = "Swap attackers/defenders after this many rounds.", required = true)
    private int swapSidesAfter;

    @ArenaOption(name = "bomb-spawn-mode", description = "How the bomb spawns: DROP or RANDOM_PLAYER.", required = true)
    private String bombSpawnMode;

    @ArenaOption(name = "bomb-item", description = "The item type used for the bomb.", required = true)
    private String bombItem;

    public Duration getPlantTime() {
        return this.plantTime;
    }

    public Duration getDetonationTime() {
        return this.detonationTime;
    }

    public Duration getDefuseTime() {
        return this.defuseTime;
    }

    public Duration getRoundTime() {
        return this.roundTime;
    }

    public int getRoundsToWin() {
        return this.roundsToWin;
    }

    public int getSwapSidesAfter() {
        return this.swapSidesAfter;
    }

    public String getBombSpawnMode() {
        return this.bombSpawnMode;
    }

    public String getBombItem() {
        return this.bombItem;
    }

    public boolean isDropMode() {
        return "DROP".equalsIgnoreCase(this.bombSpawnMode);
    }

    public boolean isRandomPlayerMode() {
        return "RANDOM_PLAYER".equalsIgnoreCase(this.bombSpawnMode);
    }
}

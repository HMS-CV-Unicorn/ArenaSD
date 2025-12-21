package com.saratoga.snd.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Per-player data during a game.
 */
public class PlayerData {

    private final UUID uuid;
    private Team team;
    private boolean alive = true;
    private boolean hasBomb = false;

    // Spectating
    private UUID spectatingTarget;

    public PlayerData(Player player) {
        this.uuid = player.getUniqueId();
    }

    public UUID getUuid() {
        return uuid;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean hasBomb() {
        return hasBomb;
    }

    public void setHasBomb(boolean hasBomb) {
        this.hasBomb = hasBomb;
    }

    public UUID getSpectatingTarget() {
        return spectatingTarget;
    }

    public void setSpectatingTarget(UUID spectatingTarget) {
        this.spectatingTarget = spectatingTarget;
    }

    /**
     * Reset player data for a new round.
     */
    public void resetForRound() {
        this.alive = true;
        this.hasBomb = false;
        this.spectatingTarget = null;
    }
}

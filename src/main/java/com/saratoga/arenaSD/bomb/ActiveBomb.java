package com.saratoga.arenaSD.bomb;

import org.battleplugins.arena.ArenaPlayer;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the active bomb in a round.
 */
public class ActiveBomb {
    private BombState state;
    private Location location;
    @Nullable
    private ArenaPlayer carrier;
    @Nullable
    private String plantedSite;
    private long stateChangeTime;
    private long dropTime;

    public ActiveBomb(Location spawnLocation) {
        this.state = BombState.SPAWNED;
        this.location = spawnLocation;
        this.carrier = null;
        this.plantedSite = null;
        this.stateChangeTime = System.currentTimeMillis();
        this.dropTime = -1;
    }

    public BombState getState() {
        return this.state;
    }

    public void setState(BombState state) {
        this.state = state;
        this.stateChangeTime = System.currentTimeMillis();
    }

    public Location getLocation() {
        return this.location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Nullable
    public ArenaPlayer getCarrier() {
        return this.carrier;
    }

    public void setCarrier(@Nullable ArenaPlayer carrier) {
        this.carrier = carrier;
        if (carrier != null) {
            this.state = BombState.CARRIED;
            this.location = carrier.getPlayer().getLocation();
        }
        this.stateChangeTime = System.currentTimeMillis();
    }

    @Nullable
    public String getPlantedSite() {
        return this.plantedSite;
    }

    public void setPlantedSite(@Nullable String site) {
        this.plantedSite = site;
    }

    public long getStateChangeTime() {
        return this.stateChangeTime;
    }

    public long getDropTime() {
        return this.dropTime;
    }

    public void setDropTime(long dropTime) {
        this.dropTime = dropTime;
    }

    public void drop(Location dropLocation) {
        this.carrier = null;
        this.location = dropLocation;
        this.state = BombState.DROPPED;
        this.dropTime = System.currentTimeMillis();
        this.stateChangeTime = System.currentTimeMillis();
    }

    public void pickup(ArenaPlayer player) {
        this.carrier = player;
        this.state = BombState.CARRIED;
        this.location = player.getPlayer().getLocation();
        this.dropTime = -1;
        this.stateChangeTime = System.currentTimeMillis();
    }

    public boolean isPlanted() {
        return this.state == BombState.PLANTED || this.state == BombState.DEFUSING;
    }

    public boolean isCarried() {
        return this.state == BombState.CARRIED;
    }

    public boolean isDropped() {
        return this.state == BombState.DROPPED;
    }

    public boolean canBePickedUp() {
        return this.state == BombState.SPAWNED || this.state == BombState.DROPPED;
    }
}

package com.saratoga.arenaSD.bomb;

/**
 * Represents the current state of the bomb in a round.
 */
public enum BombState {
    /**
     * Bomb is on the ground at spawn point (not picked up yet).
     */
    SPAWNED,

    /**
     * Bomb is being carried by an attacker.
     */
    CARRIED,

    /**
     * Bomb has been dropped (carrier died or dropped it).
     */
    DROPPED,

    /**
     * Bomb is currently being planted.
     */
    PLANTING,

    /**
     * Bomb has been planted and countdown is active.
     */
    PLANTED,

    /**
     * Bomb is currently being defused.
     */
    DEFUSING,

    /**
     * Bomb has been successfully defused.
     */
    DEFUSED,

    /**
     * Bomb has detonated.
     */
    DETONATED
}

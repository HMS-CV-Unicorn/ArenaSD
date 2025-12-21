package com.saratoga.snd.game;

/**
 * Represents a team in Search and Destroy.
 */
public enum Team {
    RED,
    BLUE;

    /**
     * Get the opposite team.
     */
    public Team opposite() {
        return this == RED ? BLUE : RED;
    }
}

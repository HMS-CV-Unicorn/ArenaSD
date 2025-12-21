package com.saratoga.snd.arena;

/**
 * Represents the state of an arena.
 */
public enum ArenaState {
    /**
     * Arena is waiting for players to join.
     */
    WAITING,

    /**
     * Countdown before round starts.
     */
    COUNTDOWN,

    /**
     * Round is in progress.
     */
    PLAYING,

    /**
     * Round/match is ending.
     */
    ENDING,

    /**
     * Between rounds - players in lobby.
     */
    INTERMISSION
}

package com.saratoga.arenaSD.arena;

import com.saratoga.arenaSD.SdExecutor;
import org.battleplugins.arena.Arena;
import org.battleplugins.arena.command.ArenaCommandExecutor;
import org.battleplugins.arena.competition.map.MapFactory;
import org.battleplugins.arena.competition.phase.CompetitionPhaseType;
import org.battleplugins.arena.event.ArenaEventHandler;
import org.battleplugins.arena.event.arena.ArenaPhaseCompleteEvent;
import org.battleplugins.arena.event.arena.ArenaPhaseStartEvent;
import org.battleplugins.arena.event.player.ArenaDeathEvent;
import org.battleplugins.arena.event.player.ArenaLeaveEvent;

/**
 * Arena class for Search and Destroy game mode.
 * Handles lifecycle events and delegates to SdCompetition.
 */
public class SdArena extends Arena {

    @Override
    public ArenaCommandExecutor createCommandExecutor() {
        return new SdExecutor(this);
    }

    @Override
    public MapFactory getMapFactory() {
        return SdMap.FACTORY;
    }

    @ArenaEventHandler
    public void onPhaseStart(ArenaPhaseStartEvent event) {
        if (!CompetitionPhaseType.INGAME.equals(event.getPhase().getType())) {
            return;
        }

        if (event.getCompetition() instanceof SdCompetition sdCompetition) {
            sdCompetition.onIngameStart();
        }
    }

    @ArenaEventHandler
    public void onPhaseComplete(ArenaPhaseCompleteEvent event) {
        if (!CompetitionPhaseType.INGAME.equals(event.getPhase().getType())) {
            return;
        }

        if (event.getCompetition() instanceof SdCompetition sdCompetition) {
            sdCompetition.onIngameComplete();
        }
    }

    @ArenaEventHandler
    public void onDeath(ArenaDeathEvent event) {
        if (event.getCompetition() instanceof SdCompetition sdCompetition) {
            sdCompetition.onPlayerDeath(event.getArenaPlayer());
        }
    }

    @ArenaEventHandler
    public void onLeave(ArenaLeaveEvent event) {
        if (event.getCompetition() instanceof SdCompetition sdCompetition) {
            sdCompetition.onPlayerLeave(event.getArenaPlayer());
        }
    }
}

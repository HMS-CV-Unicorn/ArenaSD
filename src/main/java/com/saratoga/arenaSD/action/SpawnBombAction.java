package com.saratoga.arenaSD.action;

import com.saratoga.arenaSD.arena.SdCompetition;
import org.battleplugins.arena.ArenaPlayer;
import org.battleplugins.arena.event.action.EventAction;
import org.battleplugins.arena.resolver.Resolvable;

import java.util.Map;

/**
 * Action to spawn the bomb at the start of a round.
 * Used in arena YAML: spawn-bomb
 */
public class SpawnBombAction extends EventAction {

    public SpawnBombAction(Map<String, String> params) {
        super(params);
    }

    @Override
    public void call(ArenaPlayer arenaPlayer, Resolvable resolvable) {
        if (arenaPlayer.getCompetition() instanceof SdCompetition sdCompetition) {
            sdCompetition.spawnBomb();
        }
    }
}

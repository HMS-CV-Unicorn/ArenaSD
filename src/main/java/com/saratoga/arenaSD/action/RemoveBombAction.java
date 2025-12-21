package com.saratoga.arenaSD.action;

import com.saratoga.arenaSD.arena.SdCompetition;
import org.battleplugins.arena.ArenaPlayer;
import org.battleplugins.arena.event.action.EventAction;
import org.battleplugins.arena.resolver.Resolvable;
import org.bukkit.Material;

import java.util.Map;

/**
 * Action to remove the bomb at the end of a round.
 * Used in arena YAML: remove-bomb
 */
public class RemoveBombAction extends EventAction {

    public RemoveBombAction(Map<String, String> params) {
        super(params);
    }

    @Override
    public void call(ArenaPlayer arenaPlayer, Resolvable resolvable) {
        // Remove bomb item from inventory
        arenaPlayer.getPlayer().getInventory().remove(Material.TNT);
        arenaPlayer.getPlayer().getInventory().setHelmet(null);
    }
}

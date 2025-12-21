package com.saratoga.snd.listener;

import com.saratoga.snd.SearchAndDestroy;
import com.saratoga.snd.arena.SndArena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

/**
 * Protection listener for arena players.
 */
public class ProtectionListener implements Listener {

    private final SearchAndDestroy plugin;

    public ProtectionListener(SearchAndDestroy plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (plugin.getArenaManager().isInArena(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (plugin.getArenaManager().isInArena(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHungerChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;
        if (plugin.getArenaManager().isInArena(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim))
            return;
        if (!(event.getDamager() instanceof Player attacker))
            return;

        SndArena victimArena = plugin.getArenaManager().getPlayerArena(victim);
        SndArena attackerArena = plugin.getArenaManager().getPlayerArena(attacker);

        // Both must be in same arena
        if (victimArena == null || attackerArena == null)
            return;
        if (victimArena != attackerArena) {
            event.setCancelled(true);
            return;
        }

        // Check friendly fire
        var victimData = victimArena.getPlayerData(victim);
        var attackerData = attackerArena.getPlayerData(attacker);
        if (victimData != null && attackerData != null) {
            if (victimData.getTeam() == attackerData.getTeam()) {
                event.setCancelled(true); // No friendly fire
            }
        }
    }
}

package com.saratoga.snd.listener;

import com.saratoga.snd.Messages;
import com.saratoga.snd.SearchAndDestroy;
import com.saratoga.snd.arena.ArenaState;
import com.saratoga.snd.arena.SndArena;
import com.saratoga.snd.game.GameManager;
import com.saratoga.snd.game.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Handles player events in SND.
 */
public class PlayerListener implements Listener {

    private final SearchAndDestroy plugin;

    public PlayerListener(SearchAndDestroy plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        SndArena arena = plugin.getArenaManager().getPlayerArena(player);
        if (arena == null)
            return;

        // Suppress death message
        event.deathMessage(null);

        // Keep inventory (we manage it ourselves)
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        event.setDroppedExp(0);

        // Notify game manager
        GameManager game = arena.getGameManager();
        if (game != null) {
            game.onPlayerDeath(player);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        SndArena arena = plugin.getArenaManager().getPlayerArena(player);
        if (arena == null)
            return;

        // Respawn at lobby
        if (arena.getMap().getLobbySpawn() != null) {
            event.setRespawnLocation(arena.getMap().getLobbySpawn());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getArenaManager().isInArena(player)) {
            plugin.getArenaManager().leaveArena(player);
        }
    }
}

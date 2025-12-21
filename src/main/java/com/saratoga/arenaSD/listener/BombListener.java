package com.saratoga.arenaSD.listener;

import com.saratoga.arenaSD.ArenaSD;
import com.saratoga.arenaSD.arena.SdCompetition;
import com.saratoga.arenaSD.arena.SdMap;
import com.saratoga.arenaSD.bomb.ActiveBomb;
import com.saratoga.arenaSD.bomb.BombState;
import org.battleplugins.arena.ArenaPlayer;
import org.battleplugins.arena.competition.Competition;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for bomb-related events (pickup, drop, plant, defuse).
 */
public class BombListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Item item = event.getItem();
        ItemStack stack = item.getItemStack();

        if (stack.getType() != Material.TNT) {
            return;
        }

        ArenaPlayer arenaPlayer = ArenaPlayer.getArenaPlayer(player);
        if (arenaPlayer == null) {
            return;
        }

        Competition<?> competition = arenaPlayer.getCompetition();
        if (!(competition instanceof SdCompetition sdCompetition)) {
            return;
        }

        // Check if player is attacker
        if (!sdCompetition.isAttacker(arenaPlayer)) {
            event.setCancelled(true);
            return;
        }

        // Handle pickup
        event.setCancelled(true); // Cancel default pickup
        item.remove();
        sdCompetition.onBombPickup(arenaPlayer);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDropItem(PlayerDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();

        if (stack.getType() != Material.TNT) {
            return;
        }

        Player player = event.getPlayer();
        ArenaPlayer arenaPlayer = ArenaPlayer.getArenaPlayer(player);
        if (arenaPlayer == null) {
            return;
        }

        Competition<?> competition = arenaPlayer.getCompetition();
        if (!(competition instanceof SdCompetition sdCompetition)) {
            return;
        }

        // Handle drop via competition
        event.setCancelled(true);
        sdCompetition.dropBomb(arenaPlayer);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        Player player = event.getPlayer();
        ArenaPlayer arenaPlayer = ArenaPlayer.getArenaPlayer(player);
        if (arenaPlayer == null) {
            return;
        }

        Competition<?> competition = arenaPlayer.getCompetition();
        if (!(competition instanceof SdCompetition sdCompetition)) {
            return;
        }

        ActiveBomb bomb = sdCompetition.getActiveBomb();
        if (bomb == null) {
            return;
        }

        SdMap map = sdCompetition.getSdMap();

        // Check for planting
        if (sdCompetition.isAttacker(arenaPlayer) && bomb.isCarried() && bomb.getCarrier() == arenaPlayer) {
            // Check if at a bomb site
            for (var entry : map.getBombSites().entrySet()) {
                if (entry.getValue().isInside(player.getLocation(), map.getWorld())) {
                    event.setCancelled(true);
                    sdCompetition.startPlanting(arenaPlayer, entry.getKey());
                    return;
                }
            }
        }

        // Check for defusing
        if (sdCompetition.isDefender(arenaPlayer) && bomb.isPlanted()) {
            if (player.getLocation().distanceSquared(bomb.getLocation()) <= 9) {
                event.setCancelled(true);
                sdCompetition.startDefusing(arenaPlayer);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        // Cancel plant/defuse if player moves too much
        if (!event.hasChangedBlock()) {
            return;
        }

        Player player = event.getPlayer();
        ArenaPlayer arenaPlayer = ArenaPlayer.getArenaPlayer(player);
        if (arenaPlayer == null) {
            return;
        }

        Competition<?> competition = arenaPlayer.getCompetition();
        if (!(competition instanceof SdCompetition sdCompetition)) {
            return;
        }

        ActiveBomb bomb = sdCompetition.getActiveBomb();
        if (bomb == null) {
            return;
        }

        // Cancel if planting and moved
        if (bomb.getState() == BombState.PLANTING && bomb.getCarrier() == arenaPlayer) {
            sdCompetition.cancelPlanting();
        }

        // Cancel if defusing and moved
        if (bomb.getState() == BombState.DEFUSING && sdCompetition.isDefender(arenaPlayer)) {
            sdCompetition.cancelDefusing();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSneak(PlayerToggleSneakEvent event) {
        // Stop sneaking cancels plant/defuse
        if (event.isSneaking()) {
            return; // Starting to sneak, not stopping
        }

        Player player = event.getPlayer();
        ArenaPlayer arenaPlayer = ArenaPlayer.getArenaPlayer(player);
        if (arenaPlayer == null) {
            return;
        }

        Competition<?> competition = arenaPlayer.getCompetition();
        if (!(competition instanceof SdCompetition sdCompetition)) {
            return;
        }

        ActiveBomb bomb = sdCompetition.getActiveBomb();
        if (bomb == null) {
            return;
        }

        // Cancel plant/defuse if releasing sneak
        if (bomb.getState() == BombState.PLANTING) {
            sdCompetition.cancelPlanting();
        }
        if (bomb.getState() == BombState.DEFUSING) {
            sdCompetition.cancelDefusing();
        }
    }
}

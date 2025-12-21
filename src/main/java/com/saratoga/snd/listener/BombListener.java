package com.saratoga.snd.listener;

import com.saratoga.snd.Messages;
import com.saratoga.snd.SearchAndDestroy;
import com.saratoga.snd.arena.ArenaState;
import com.saratoga.snd.arena.SndArena;
import com.saratoga.snd.arena.SndMap;
import com.saratoga.snd.game.Bomb;
import com.saratoga.snd.game.GameManager;
import com.saratoga.snd.game.PlayerData;
import com.saratoga.snd.game.Role;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles bomb interaction events.
 */
public class BombListener implements Listener {

    private final SearchAndDestroy plugin;

    public BombListener(SearchAndDestroy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;

        SndArena arena = plugin.getArenaManager().getPlayerArena(player);
        if (arena == null)
            return;

        GameManager game = arena.getGameManager();
        if (game == null)
            return;

        Bomb bomb = game.getBomb();
        if (bomb == null)
            return;

        Item item = event.getItem();
        if (item.equals(bomb.getDroppedItem())) {
            PlayerData data = arena.getPlayerData(player);
            if (data == null)
                return;

            // Only attackers can pick up bomb
            if (game.getRoleForTeam(data.getTeam()) != Role.ATTACKERS) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true); // We handle it manually
            bomb.pickup(player.getUniqueId());
            data.setHasBomb(true);

            // Give bomb item to player
            ItemStack bombItem = new ItemStack(plugin.getMainConfig().getBombItem());
            var meta = bombItem.getItemMeta();
            meta.displayName(net.kyori.adventure.text.Component.text("爆弾",
                    net.kyori.adventure.text.format.NamedTextColor.RED,
                    net.kyori.adventure.text.format.TextDecoration.BOLD));
            bombItem.setItemMeta(meta);
            player.getInventory().addItem(bombItem);

            Messages.send(player, Messages.BOMB_PICKED_UP);

            // Make carrier glow for teammates only
            // Note: Full implementation would require PacketEvents for team-specific
            // glowing
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        SndArena arena = plugin.getArenaManager().getPlayerArena(player);
        if (arena == null)
            return;

        PlayerData data = arena.getPlayerData(player);
        if (data == null || !data.hasBomb())
            return;

        // Check if dropping bomb item
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (dropped.getType() == plugin.getMainConfig().getBombItem()) {
            event.setCancelled(true);
            // Don't allow manual dropping - only on death
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick())
            return;

        Player player = event.getPlayer();
        SndArena arena = plugin.getArenaManager().getPlayerArena(player);
        if (arena == null)
            return;
        if (arena.getState() != ArenaState.PLAYING)
            return;

        GameManager game = arena.getGameManager();
        if (game == null)
            return;

        Bomb bomb = game.getBomb();
        if (bomb == null)
            return;

        PlayerData data = arena.getPlayerData(player);
        if (data == null || !data.isAlive())
            return;

        Role role = game.getRoleForTeam(data.getTeam());

        // Attackers plant bomb - only if holding bomb item in hand
        if (role == Role.ATTACKERS && data.hasBomb() && !bomb.isPlanted()) {
            // Check if actually holding bomb in main hand
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand.getType() == plugin.getMainConfig().getBombItem()) {
                handlePlanting(player, arena, game, bomb, data);
            }
        }
        // Defenders defuse bomb
        else if (role == Role.DEFENDERS && bomb.isPlanted()) {
            handleDefusing(player, arena, game, bomb);
        }
    }

    private void handlePlanting(Player player, SndArena arena, GameManager game, Bomb bomb, PlayerData data) {
        // Check if in bomb site
        SndMap map = arena.getMap();
        Location loc = player.getLocation();

        String siteName = null;
        for (var entry : map.getBombSites().entrySet()) {
            if (entry.getValue().isInside(loc)) {
                siteName = entry.getKey();
                break;
            }
        }

        if (siteName == null) {
            Messages.send(player, Messages.NOT_IN_BOMB_SITE);
            return;
        }

        if (bomb.isBeingPlanted()) {
            // Already planting
            return;
        }

        final String site = siteName;
        Messages.send(player, Messages.PLANTING_BOMB);

        bomb.startPlanting(player.getUniqueId(), () -> {
            // Planting complete
            data.setHasBomb(false);

            // Remove bomb from inventory
            player.getInventory().remove(plugin.getMainConfig().getBombItem());

            game.onBombPlanted(site, player.getLocation());
        });
    }

    private void handleDefusing(Player player, SndArena arena, GameManager game, Bomb bomb) {
        // Must be near planted bomb
        Location bombLoc = bomb.getLocation();
        if (player.getLocation().distanceSquared(bombLoc) > 9) { // 3 block radius
            return;
        }

        if (bomb.isBeingDefused()) {
            return;
        }

        Messages.send(player, Messages.DEFUSING_BOMB);

        bomb.startDefusing(player.getUniqueId(), () -> {
            // Defusing complete
            game.onBombDefused();
        });
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Cancel planting/defusing if player moves
        if (!event.hasChangedBlock())
            return;

        Player player = event.getPlayer();
        SndArena arena = plugin.getArenaManager().getPlayerArena(player);
        if (arena == null)
            return;

        GameManager game = arena.getGameManager();
        if (game == null)
            return;

        Bomb bomb = game.getBomb();
        if (bomb == null)
            return;

        // Check if this player is planting or defusing
        if (player.getUniqueId().equals(bomb.getActionPlayer())) {
            bomb.cancelAction();
        }
    }
}

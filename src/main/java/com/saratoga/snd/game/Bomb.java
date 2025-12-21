package com.saratoga.snd.game;

import com.saratoga.snd.SearchAndDestroy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * Represents the bomb during a round.
 */
public class Bomb {

    public enum State {
        NOT_SPAWNED,
        DROPPED, // On ground
        CARRIED, // Player has it
        PLANTING, // Being planted
        PLANTED, // Planted and ticking
        DEFUSING, // Being defused
        EXPLODED,
        DEFUSED
    }

    private final SearchAndDestroy plugin;
    private State state = State.NOT_SPAWNED;

    // Location
    private Location location;
    private Item droppedItem;

    // Carrier
    private UUID carrier;

    // Planted info
    private String plantedSite; // "A" or "B"
    private int explosionTimer;
    private BukkitTask explosionTask;

    // Action progress
    private UUID actionPlayer; // Player planting or defusing
    private int actionProgress; // Ticks remaining
    private BukkitTask actionTask;

    public Bomb(SearchAndDestroy plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawn bomb as dropped item.
     */
    public void spawn(Location location) {
        this.location = location;
        this.state = State.DROPPED;

        ItemStack bombStack = new ItemStack(plugin.getMainConfig().getBombItem());
        var meta = bombStack.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text("爆弾",
                net.kyori.adventure.text.format.NamedTextColor.RED,
                net.kyori.adventure.text.format.TextDecoration.BOLD));
        meta.lore(java.util.List.of(
                net.kyori.adventure.text.Component.text("爆弾サイトで右クリックで設置",
                        net.kyori.adventure.text.format.NamedTextColor.GRAY)));
        bombStack.setItemMeta(meta);

        this.droppedItem = location.getWorld().dropItem(location, bombStack);
        this.droppedItem.setCustomName("§c§l爆弾");
        this.droppedItem.setCustomNameVisible(true);
        this.droppedItem.setGlowing(true);
    }

    /**
     * Player picks up bomb.
     */
    public void pickup(UUID playerId) {
        if (droppedItem != null) {
            droppedItem.remove();
            droppedItem = null;
        }
        this.carrier = playerId;
        this.state = State.CARRIED;
    }

    /**
     * Drop bomb at location.
     */
    public void drop(Location loc) {
        this.carrier = null;
        this.location = loc;
        this.state = State.DROPPED;

        ItemStack bombStack = new ItemStack(plugin.getMainConfig().getBombItem());
        var meta = bombStack.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text("爆弾",
                net.kyori.adventure.text.format.NamedTextColor.RED,
                net.kyori.adventure.text.format.TextDecoration.BOLD));
        bombStack.setItemMeta(meta);

        this.droppedItem = loc.getWorld().dropItem(loc, bombStack);
        this.droppedItem.setCustomName("§c§l爆弾");
        this.droppedItem.setCustomNameVisible(true);
        this.droppedItem.setGlowing(true);
    }

    /**
     * Start planting process.
     */
    public void startPlanting(UUID playerId, Runnable onComplete) {
        this.state = State.PLANTING;
        this.actionPlayer = playerId;
        int totalTicks = plugin.getMainConfig().getPlantTime() * 20;
        this.actionProgress = totalTicks;

        this.actionTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            actionProgress--;

            // Show progress bar to player
            Player player = Bukkit.getPlayer(actionPlayer);
            if (player != null) {
                player.sendActionBar(createProgressBar("爆弾設置中", actionProgress, totalTicks, NamedTextColor.RED));
            }

            if (actionProgress <= 0) {
                actionTask.cancel();
                actionTask = null;
                if (player != null) {
                    player.sendActionBar(Component.text("設置完了！", NamedTextColor.GOLD));
                }
                onComplete.run();
            }
        }, 0L, 1L);
    }

    /**
     * Complete planting.
     */
    public void plant(String siteName, Location loc, Runnable onExplode) {
        cancelAction();

        this.state = State.PLANTED;
        this.plantedSite = siteName;
        this.location = loc;
        this.carrier = null;
        this.explosionTimer = plugin.getMainConfig().getExplosionTime();

        // Start explosion countdown
        this.explosionTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            explosionTimer--;
            if (explosionTimer <= 0) {
                explosionTask.cancel();
                explosionTask = null;
                this.state = State.EXPLODED;
                onExplode.run();
            }
        }, 20L, 20L);
    }

    /**
     * Start defusing process.
     */
    public void startDefusing(UUID playerId, Runnable onComplete) {
        this.state = State.DEFUSING;
        this.actionPlayer = playerId;
        int totalTicks = plugin.getMainConfig().getDefuseTime() * 20;
        this.actionProgress = totalTicks;

        this.actionTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            actionProgress--;

            // Show progress bar to player
            Player player = Bukkit.getPlayer(actionPlayer);
            if (player != null) {
                player.sendActionBar(createProgressBar("爆弾解除中", actionProgress, totalTicks, NamedTextColor.GREEN));
            }

            if (actionProgress <= 0) {
                actionTask.cancel();
                actionTask = null;
                if (player != null) {
                    player.sendActionBar(Component.text("解除完了！", NamedTextColor.GREEN));
                }
                onComplete.run();
            }
        }, 0L, 1L);
    }

    /**
     * Create a progress bar component.
     */
    private Component createProgressBar(String label, int remaining, int total, NamedTextColor color) {
        int barLength = 20;
        int filled = (int) ((double) (total - remaining) / total * barLength);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < barLength; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        int percent = (int) ((double) (total - remaining) / total * 100);
        return Component.text(label + " ", NamedTextColor.WHITE)
                .append(Component.text("[", NamedTextColor.GRAY))
                .append(Component.text(bar.toString(), color))
                .append(Component.text("] ", NamedTextColor.GRAY))
                .append(Component.text(percent + "%", NamedTextColor.WHITE));
    }

    /**
     * Complete defusing.
     */
    public void defuse() {
        cancelAction();
        cancelExplosion();
        this.state = State.DEFUSED;
    }

    /**
     * Cancel current action (plant/defuse).
     */
    public void cancelAction() {
        if (actionTask != null) {
            actionTask.cancel();
            actionTask = null;
        }
        actionPlayer = null;
        actionProgress = 0;

        // Revert state
        if (state == State.PLANTING) {
            state = State.CARRIED;
        } else if (state == State.DEFUSING) {
            state = State.PLANTED;
        }
    }

    /**
     * Cancel explosion timer.
     */
    public void cancelExplosion() {
        if (explosionTask != null) {
            explosionTask.cancel();
            explosionTask = null;
        }
    }

    /**
     * Clean up all tasks and entities.
     */
    public void cleanup() {
        cancelAction();
        cancelExplosion();
        if (droppedItem != null) {
            droppedItem.remove();
            droppedItem = null;
        }
        state = State.NOT_SPAWNED;
    }

    // Getters
    public State getState() {
        return state;
    }

    public Location getLocation() {
        return location;
    }

    public UUID getCarrier() {
        return carrier;
    }

    public String getPlantedSite() {
        return plantedSite;
    }

    public int getExplosionTimer() {
        return explosionTimer;
    }

    public UUID getActionPlayer() {
        return actionPlayer;
    }

    public int getActionProgress() {
        return actionProgress;
    }

    public Item getDroppedItem() {
        return droppedItem;
    }

    public boolean isBeingPlanted() {
        return state == State.PLANTING;
    }

    public boolean isBeingDefused() {
        return state == State.DEFUSING;
    }

    public boolean isPlanted() {
        return state == State.PLANTED || state == State.DEFUSING;
    }
}

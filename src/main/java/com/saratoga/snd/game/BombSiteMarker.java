package com.saratoga.snd.game;

import com.saratoga.snd.SearchAndDestroy;
import com.saratoga.snd.arena.SndArena;
import com.saratoga.snd.arena.SndMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages armor stand markers for bomb sites.
 * Creates glowing armor stands with site names during matches.
 */
public class BombSiteMarker {

    private final SearchAndDestroy plugin;
    private final SndArena arena;
    private final Map<String, ArmorStand> markers = new HashMap<>();

    // Scoreboard for glow color
    private Scoreboard glowScoreboard;
    private org.bukkit.scoreboard.Team glowTeamA;
    private org.bukkit.scoreboard.Team glowTeamB;
    private org.bukkit.scoreboard.Team glowTeamPlanted;

    public BombSiteMarker(SearchAndDestroy plugin, SndArena arena) {
        this.plugin = plugin;
        this.arena = arena;
        setupGlowTeams();
    }

    /**
     * Set up glow teams for coloring.
     */
    private void setupGlowTeams() {
        glowScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        glowTeamA = glowScoreboard.registerNewTeam("siteA");
        glowTeamA.color(NamedTextColor.YELLOW);

        glowTeamB = glowScoreboard.registerNewTeam("siteB");
        glowTeamB.color(NamedTextColor.AQUA);

        glowTeamPlanted = glowScoreboard.registerNewTeam("planted");
        glowTeamPlanted.color(NamedTextColor.RED);
    }

    /**
     * Spawn markers for all bomb sites.
     */
    public void spawnMarkers() {
        SndMap map = arena.getMap();

        for (var entry : map.getBombSites().entrySet()) {
            String siteName = entry.getKey();
            SndMap.BombSite site = entry.getValue();

            // Spawn at center of bomb site
            Location loc = site.center().clone();
            loc.add(0, 1.5, 0); // Raise to be visible

            // IMPORTANT: Remove any existing armor stands at this location first
            // This cleans up any leftover markers from previous games/rounds
            cleanupExistingMarkers(loc);

            ArmorStand marker = spawnMarker(loc, siteName, false);
            markers.put(siteName, marker);

            // Add to glow team for colored glow
            org.bukkit.scoreboard.Team team = siteName.equalsIgnoreCase("A") ? glowTeamA : glowTeamB;
            team.addEntity(marker);
        }
    }

    /**
     * Remove any existing invisible armor stands near the given location.
     * This ensures no leftover markers from previous games.
     */
    private void cleanupExistingMarkers(Location location) {
        if (location.getWorld() == null)
            return;

        // Search for armor stands within 1 block radius
        for (Entity entity : location.getWorld().getNearbyEntities(location, 1, 2, 1)) {
            if (entity instanceof ArmorStand stand) {
                // Only remove invisible armor stands (our markers)
                if (!stand.isVisible() && stand.isCustomNameVisible()) {
                    plugin.getLogger().info("[BombSiteMarker] Cleaning up old marker: " + stand.getName());
                    stand.remove();
                }
            }
        }
    }

    /**
     * Spawn a single marker armor stand.
     */
    private ArmorStand spawnMarker(Location location, String siteName, boolean isPlanted) {
        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);

        // Configure armor stand
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setMarker(true); // No collision
        stand.setCustomNameVisible(true);
        stand.setGlowing(true);

        // Set name with color based on state
        updateMarkerName(stand, siteName, isPlanted);

        return stand;
    }

    /**
     * Update marker name and color.
     */
    private void updateMarkerName(ArmorStand stand, String siteName, boolean isPlanted) {
        Component name;
        if (isPlanted) {
            // Planted site - red flashing effect
            name = Component.text("💣 ", NamedTextColor.RED)
                    .append(Component.text("サイト " + siteName, NamedTextColor.RED, TextDecoration.BOLD))
                    .append(Component.text(" 💣", NamedTextColor.RED));
        } else {
            // Normal site marker
            NamedTextColor color = siteName.equalsIgnoreCase("A") ? NamedTextColor.YELLOW : NamedTextColor.AQUA;
            name = Component.text("【 サイト " + siteName + " 】", color, TextDecoration.BOLD);
        }
        stand.customName(name);
    }

    /**
     * Mark a site as planted - removes non-planted site markers and updates
     * the planted site marker to show bomb icon with red glow.
     */
    public void setPlantedSite(String siteName) {
        plugin.getLogger().info("[BombSiteMarker] setPlantedSite called with: " + siteName);

        String plantedSiteKey = null;

        // Find the planted site key (case-insensitive)
        for (String key : markers.keySet()) {
            if (key.equalsIgnoreCase(siteName)) {
                plantedSiteKey = key;
                break;
            }
        }

        // Remove markers for sites that are NOT the planted site
        var iterator = markers.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            String key = entry.getKey();
            ArmorStand stand = entry.getValue();

            if (!key.equalsIgnoreCase(siteName)) {
                // This is NOT the planted site - remove the marker
                if (stand != null && !stand.isDead()) {
                    plugin.getLogger().info("[BombSiteMarker] Removing non-planted marker: " + key);
                    stand.remove();
                }
                iterator.remove();
            }
        }

        // Update the planted site marker
        if (plantedSiteKey != null) {
            ArmorStand plantedMarker = markers.get(plantedSiteKey);
            if (plantedMarker != null && !plantedMarker.isDead()) {
                plugin.getLogger().info("[BombSiteMarker] Updating marker for planted site: " + plantedSiteKey);

                // Update the name to show bomb icon
                updateMarkerName(plantedMarker, plantedSiteKey, true);

                // Change glow team to red (planted)
                glowTeamA.removeEntity(plantedMarker);
                glowTeamB.removeEntity(plantedMarker);
                glowTeamPlanted.addEntity(plantedMarker);

                plugin.getLogger().info("[BombSiteMarker] Updated planted marker at: " + plantedMarker.getLocation());
            } else {
                plugin.getLogger()
                        .warning("[BombSiteMarker] Planted marker was null or dead for site: " + plantedSiteKey);
            }
        } else {
            plugin.getLogger().warning("[BombSiteMarker] Could not find marker for site: " + siteName);
        }
    }

    /**
     * Remove all markers.
     */
    public void removeMarkers() {
        plugin.getLogger().info("[BombSiteMarker] removeMarkers called, removing " + markers.size() + " markers");
        for (ArmorStand stand : markers.values()) {
            if (stand != null && !stand.isDead()) {
                plugin.getLogger()
                        .info("[BombSiteMarker] Removing marker: " + stand.getName() + " at " + stand.getLocation());
                stand.remove();
            }
        }
        markers.clear();
    }
}

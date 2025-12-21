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

            ArmorStand marker = spawnMarker(loc, siteName, false);
            markers.put(siteName, marker);

            // Add to glow team for colored glow
            org.bukkit.scoreboard.Team team = siteName.equalsIgnoreCase("A") ? glowTeamA : glowTeamB;
            team.addEntity(marker);
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
     * Mark a site as planted (highlight it and remove other sites).
     */
    public void setPlantedSite(String siteName) {
        // Remove all non-planted site markers
        for (var entry : new HashMap<>(markers).entrySet()) {
            String name = entry.getKey();
            ArmorStand stand = entry.getValue();

            if (!name.equals(siteName)) {
                // Remove non-planted site
                if (stand != null && !stand.isDead()) {
                    stand.remove();
                }
                markers.remove(name);
            } else {
                // Update planted site
                updateMarkerName(stand, name, true);

                // Change glow color to red
                glowTeamA.removeEntity(stand);
                glowTeamB.removeEntity(stand);
                glowTeamPlanted.addEntity(stand);
            }
        }
    }

    /**
     * Remove all markers.
     */
    public void removeMarkers() {
        for (ArmorStand stand : markers.values()) {
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        markers.clear();
    }
}

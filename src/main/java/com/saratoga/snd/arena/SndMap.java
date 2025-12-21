package com.saratoga.snd.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a map configuration for Search and Destroy.
 */
public class SndMap {

    private final String name;
    private final File file;
    private String worldName;

    // Spawns
    private Location lobbySpawn;
    private Location attackerSpawn;
    private Location defenderSpawn;

    // Bomb sites
    private final Map<String, BombSite> bombSites = new HashMap<>();

    // Settings
    private int minPlayers = 2;
    private int maxPlayers = 10;

    public SndMap(String name, File file) {
        this.name = name;
        this.file = file;
    }

    /**
     * Load map from YAML file.
     */
    public static SndMap load(File file) {
        String name = file.getName().replace(".yml", "");
        SndMap map = new SndMap(name, file);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        map.worldName = config.getString("world", "world");
        World world = Bukkit.getWorld(map.worldName);
        if (world == null) {
            return map; // World not loaded yet, locations will be null
        }

        // Load lobby spawn
        ConfigurationSection lobbySection = config.getConfigurationSection("lobby");
        if (lobbySection != null) {
            map.lobbySpawn = loadLocation(world, lobbySection);
        }

        // Load spawns
        ConfigurationSection spawnsSection = config.getConfigurationSection("spawns");
        if (spawnsSection != null) {
            ConfigurationSection attackersSection = spawnsSection.getConfigurationSection("attackers");
            if (attackersSection != null) {
                map.attackerSpawn = loadLocation(world, attackersSection);
            }
            ConfigurationSection defendersSection = spawnsSection.getConfigurationSection("defenders");
            if (defendersSection != null) {
                map.defenderSpawn = loadLocation(world, defendersSection);
            }
        }

        // Load bomb sites
        ConfigurationSection sitesSection = config.getConfigurationSection("bomb-sites");
        if (sitesSection != null) {
            for (String siteKey : sitesSection.getKeys(false)) {
                ConfigurationSection siteSection = sitesSection.getConfigurationSection(siteKey);
                if (siteSection != null) {
                    double x = siteSection.getDouble("x");
                    double y = siteSection.getDouble("y");
                    double z = siteSection.getDouble("z");
                    double radius = siteSection.getDouble("radius", 5.0);
                    map.bombSites.put(siteKey.toUpperCase(), new BombSite(new Location(world, x, y, z), radius));
                }
            }
        }

        // Load settings
        map.minPlayers = config.getInt("min-players", 2);
        map.maxPlayers = config.getInt("max-players", 10);

        return map;
    }

    private static Location loadLocation(World world, ConfigurationSection section) {
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Save map to YAML file.
     */
    public void save() throws IOException {
        YamlConfiguration config = new YamlConfiguration();

        config.set("name", name);
        config.set("world", worldName);

        // Save lobby
        if (lobbySpawn != null) {
            saveLocation(config.createSection("lobby"), lobbySpawn);
        }

        // Save spawns
        ConfigurationSection spawnsSection = config.createSection("spawns");
        if (attackerSpawn != null) {
            saveLocation(spawnsSection.createSection("attackers"), attackerSpawn);
        }
        if (defenderSpawn != null) {
            saveLocation(spawnsSection.createSection("defenders"), defenderSpawn);
        }

        // Save bomb sites
        ConfigurationSection sitesSection = config.createSection("bomb-sites");
        for (Map.Entry<String, BombSite> entry : bombSites.entrySet()) {
            ConfigurationSection siteSection = sitesSection.createSection(entry.getKey());
            BombSite site = entry.getValue();
            siteSection.set("x", site.center().getX());
            siteSection.set("y", site.center().getY());
            siteSection.set("z", site.center().getZ());
            siteSection.set("radius", site.radius());
        }

        // Save settings
        config.set("min-players", minPlayers);
        config.set("max-players", maxPlayers);

        config.save(file);
    }

    private void saveLocation(ConfigurationSection section, Location loc) {
        section.set("x", loc.getX());
        section.set("y", loc.getY());
        section.set("z", loc.getZ());
        section.set("yaw", loc.getYaw());
        section.set("pitch", loc.getPitch());
    }

    /**
     * Check if map setup is complete.
     */
    public boolean isReady() {
        return lobbySpawn != null
                && attackerSpawn != null
                && defenderSpawn != null
                && bombSites.containsKey("A")
                && bombSites.containsKey("B");
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public Location getLobbySpawn() {
        return lobbySpawn;
    }

    public void setLobbySpawn(Location lobbySpawn) {
        this.lobbySpawn = lobbySpawn;
    }

    public Location getAttackerSpawn() {
        return attackerSpawn;
    }

    public void setAttackerSpawn(Location attackerSpawn) {
        this.attackerSpawn = attackerSpawn;
    }

    public Location getDefenderSpawn() {
        return defenderSpawn;
    }

    public void setDefenderSpawn(Location defenderSpawn) {
        this.defenderSpawn = defenderSpawn;
    }

    public Map<String, BombSite> getBombSites() {
        return bombSites;
    }

    public BombSite getBombSite(String name) {
        return bombSites.get(name.toUpperCase());
    }

    public void setBombSite(String name, BombSite site) {
        bombSites.put(name.toUpperCase(), site);
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    /**
     * Represents a bomb site with center and radius.
     */
    public record BombSite(Location center, double radius) {
        public boolean isInside(Location loc) {
            if (!loc.getWorld().equals(center.getWorld()))
                return false;
            return loc.distanceSquared(center) <= radius * radius;
        }
    }
}

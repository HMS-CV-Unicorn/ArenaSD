package com.saratoga.snd.arena;

import com.saratoga.snd.SearchAndDestroy;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages all arenas and maps.
 */
public class ArenaManager {

    private final SearchAndDestroy plugin;
    private final File mapsFolder;

    private final Map<String, SndMap> maps = new HashMap<>();
    private final Map<String, SndArena> arenas = new HashMap<>();

    // Track which arena each player is in
    private final Map<UUID, SndArena> playerArenas = new HashMap<>();

    public ArenaManager(SearchAndDestroy plugin) {
        this.plugin = plugin;
        this.mapsFolder = new File(plugin.getDataFolder(), "maps");
        if (!mapsFolder.exists()) {
            mapsFolder.mkdirs();
        }
    }

    /**
     * Load all maps from disk.
     */
    public void loadMaps() {
        maps.clear();

        File[] files = mapsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null)
            return;

        for (File file : files) {
            SndMap map = SndMap.load(file);
            maps.put(map.getName().toLowerCase(), map);
            plugin.getSLF4JLogger().info("Loaded map: {}", map.getName());
        }
    }

    /**
     * Create a new map.
     */
    public SndMap createMap(String name, String worldName) throws IOException {
        String key = name.toLowerCase();
        if (maps.containsKey(key)) {
            return null; // Already exists
        }

        File file = new File(mapsFolder, key + ".yml");
        SndMap map = new SndMap(name, file);
        map.setWorldName(worldName);
        map.save();

        maps.put(key, map);
        return map;
    }

    /**
     * Delete a map.
     */
    public boolean deleteMap(String name) {
        String key = name.toLowerCase();
        SndMap map = maps.remove(key);
        if (map == null)
            return false;

        // End any arena using this map
        SndArena arena = arenas.remove(key);
        if (arena != null) {
            arena.endGame();
        }

        // Delete file
        File file = new File(mapsFolder, key + ".yml");
        return file.delete();
    }

    /**
     * Get a map by name.
     */
    public SndMap getMap(String name) {
        return maps.get(name.toLowerCase());
    }

    /**
     * Get all maps.
     */
    public Collection<SndMap> getMaps() {
        return maps.values();
    }

    /**
     * Get or create arena for a map.
     */
    public SndArena getOrCreateArena(SndMap map) {
        String key = map.getName().toLowerCase();
        return arenas.computeIfAbsent(key, k -> new SndArena(plugin, map));
    }

    /**
     * Get arena a player is in.
     */
    public SndArena getPlayerArena(Player player) {
        return playerArenas.get(player.getUniqueId());
    }

    /**
     * Player joins an arena.
     */
    public boolean joinArena(Player player, String mapName) {
        // Check if already in arena
        if (playerArenas.containsKey(player.getUniqueId())) {
            return false;
        }

        SndMap map = getMap(mapName);
        if (map == null)
            return false;

        if (!map.isReady()) {
            return false;
        }

        SndArena arena = getOrCreateArena(map);
        if (arena.join(player)) {
            playerArenas.put(player.getUniqueId(), arena);
            return true;
        }
        return false;
    }

    /**
     * Player leaves their arena.
     */
    public void leaveArena(Player player) {
        SndArena arena = playerArenas.remove(player.getUniqueId());
        if (arena != null) {
            arena.leave(player);
        }
    }

    /**
     * Remove player from arena tracking (called when game ends).
     * Does NOT call arena.leave() - player is already removed.
     */
    public void removePlayerFromArenaTracking(UUID playerId) {
        playerArenas.remove(playerId);
    }

    /**
     * Remove all players from arena tracking for a specific arena.
     */
    public void clearArenaPlayers(SndArena arena) {
        playerArenas.entrySet().removeIf(entry -> entry.getValue() == arena);
    }

    /**
     * Shutdown all arenas.
     */
    public void shutdown() {
        for (SndArena arena : arenas.values()) {
            arena.endGame();
        }
        arenas.clear();
        playerArenas.clear();
    }

    /**
     * Get all map names.
     */
    public List<String> getMapNames() {
        return new ArrayList<>(maps.keySet());
    }

    /**
     * Check if player is in any arena.
     */
    public boolean isInArena(Player player) {
        return playerArenas.containsKey(player.getUniqueId());
    }

    /**
     * Find a WAITING arena that has room for more players.
     */
    public SndArena findWaitingArena() {
        for (SndArena arena : arenas.values()) {
            if (arena.getState() == ArenaState.WAITING
                    && arena.getPlayerCount() < arena.getMap().getMaxPlayers()) {
                return arena;
            }
        }
        return null;
    }

    /**
     * Find an available map (not currently in use or empty WAITING).
     */
    public SndMap findAvailableMap() {
        List<SndMap> availableMaps = new ArrayList<>();
        for (SndMap map : maps.values()) {
            if (map.isReady()) {
                SndArena arena = arenas.get(map.getName().toLowerCase());
                // Available if no arena exists or arena is in WAITING with 0 players
                if (arena == null || (arena.getState() == ArenaState.WAITING && arena.getPlayerCount() == 0)) {
                    availableMaps.add(map);
                }
            }
        }
        if (availableMaps.isEmpty())
            return null;
        // Random selection
        return availableMaps.get(new Random().nextInt(availableMaps.size()));
    }

    /**
     * Auto-join logic: find waiting arena or create new one.
     */
    public JoinResult autoJoin(Player player) {
        // 1. Try to find WAITING arena with players already in it
        SndArena waitingArena = findWaitingArena();
        if (waitingArena != null) {
            if (joinArena(player, waitingArena.getMap().getName())) {
                return new JoinResult(true, waitingArena.getMap().getName());
            }
            // Retry with another WAITING arena (in case first one filled up)
            waitingArena = findWaitingArena();
            if (waitingArena != null && joinArena(player, waitingArena.getMap().getName())) {
                return new JoinResult(true, waitingArena.getMap().getName());
            }
        }

        // 2. Create new arena on available map
        SndMap availableMap = findAvailableMap();
        if (availableMap != null) {
            if (joinArena(player, availableMap.getName())) {
                return new JoinResult(true, availableMap.getName());
            }
        }

        return new JoinResult(false, null);
    }

    /**
     * Result of an auto-join attempt.
     */
    public record JoinResult(boolean success, String mapName) {
    }
}

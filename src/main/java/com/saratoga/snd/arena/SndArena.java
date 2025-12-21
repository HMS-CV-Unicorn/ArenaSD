package com.saratoga.snd.arena;

import com.saratoga.snd.Messages;
import com.saratoga.snd.SearchAndDestroy;
import com.saratoga.snd.game.Bomb;
import com.saratoga.snd.game.GameManager;
import com.saratoga.snd.game.PlayerData;
import com.saratoga.snd.game.Team;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Represents an active arena instance.
 */
public class SndArena {

    private final SearchAndDestroy plugin;
    private final SndMap map;
    private ArenaState state = ArenaState.WAITING;

    // Players
    private final Map<UUID, PlayerData> players = new HashMap<>();
    private final Map<UUID, SavedPlayerState> savedStates = new HashMap<>();

    // Game manager (created when game starts)
    private GameManager gameManager;

    public SndArena(SearchAndDestroy plugin, SndMap map) {
        this.plugin = plugin;
        this.map = map;
    }

    /**
     * Player joins this arena.
     */
    public boolean join(Player player) {
        if (players.size() >= map.getMaxPlayers()) {
            Messages.send(player, Messages.GAME_FULL);
            return false;
        }

        // Allow joining during WAITING and COUNTDOWN (before first round starts)
        // Block during PLAYING, INTERMISSION (between rounds), and ENDING
        if (state != ArenaState.WAITING && state != ArenaState.COUNTDOWN) {
            Messages.send(player, Messages.PREFIX.append(
                    net.kyori.adventure.text.Component.text("試合が進行中です。終了までお待ちください。",
                            net.kyori.adventure.text.format.NamedTextColor.RED)));
            return false;
        }

        // Save player state
        savedStates.put(player.getUniqueId(), new SavedPlayerState(player));

        // Clear inventory and set gamemode
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);

        // Teleport to lobby
        if (map.getLobbySpawn() != null) {
            player.teleport(map.getLobbySpawn());
        }

        // Create player data
        PlayerData data = new PlayerData(player);
        players.put(player.getUniqueId(), data);

        // Broadcast join message
        broadcast(Messages.PREFIX.append(
                net.kyori.adventure.text.Component.text(
                        player.getName() + " が参加しました (" + players.size() + "/" + map.getMaxPlayers() + ")",
                        net.kyori.adventure.text.format.NamedTextColor.GREEN)));

        // Auto-assign team
        assignTeam(data);

        // Check if we can start
        checkStart();

        return true;
    }

    /**
     * Player leaves this arena.
     */
    public void leave(Player player) {
        PlayerData data = players.get(player.getUniqueId());
        if (data == null)
            return;

        Team leavingTeam = data.getTeam();
        boolean hasBomb = data.hasBomb();

        // Notify game manager BEFORE removing (so it can drop bomb etc)
        if (gameManager != null && (state == ArenaState.PLAYING || state == ArenaState.INTERMISSION)) {
            gameManager.onPlayerLeavePreRemove(player, hasBomb);
        }

        // NOW remove from map
        players.remove(player.getUniqueId());

        // Restore player state
        SavedPlayerState saved = savedStates.remove(player.getUniqueId());
        if (saved != null) {
            saved.restore(player);
        }

        broadcast(Messages.PREFIX.append(
                net.kyori.adventure.text.Component.text(player.getName() + " が退出しました",
                        net.kyori.adventure.text.format.NamedTextColor.YELLOW)));

        // Check team count AFTER removing
        if (state == ArenaState.PLAYING || state == ArenaState.INTERMISSION) {
            checkTeamCountAfterLeave(leavingTeam);
        }
    }

    /**
     * Check if a team has no players after someone leaves.
     */
    private void checkTeamCountAfterLeave(Team leavingTeam) {
        List<PlayerData> redPlayers = getPlayersOnTeam(Team.RED);
        List<PlayerData> bluePlayers = getPlayersOnTeam(Team.BLUE);

        if (redPlayers.isEmpty() && bluePlayers.isEmpty()) {
            // Everyone left
            forceEndGame();
        } else if (redPlayers.isEmpty()) {
            // Red team empty - Blue wins
            broadcast(Messages.PREFIX.append(
                    net.kyori.adventure.text.Component.text(
                            plugin.getMainConfig().getRedTeamName() + "が退出。" + plugin.getMainConfig().getBlueTeamName()
                                    + "の勝利！",
                            net.kyori.adventure.text.format.NamedTextColor.GOLD)));
            forceEndGameWithWinner(Team.BLUE);
        } else if (bluePlayers.isEmpty()) {
            // Blue team empty - Red wins
            broadcast(Messages.PREFIX.append(
                    net.kyori.adventure.text.Component.text(
                            plugin.getMainConfig().getBlueTeamName() + "が退出。" + plugin.getMainConfig().getRedTeamName()
                                    + "の勝利！",
                            net.kyori.adventure.text.format.NamedTextColor.GOLD)));
            forceEndGameWithWinner(Team.RED);
        }
    }

    /**
     * Force end the game immediately without delay.
     */
    public void forceEndGame() {
        state = ArenaState.ENDING;

        // Cleanup game manager
        if (gameManager != null) {
            gameManager.cleanup();
        }

        // Reset immediately
        reset();
    }

    /**
     * Force end the game with a winner.
     */
    public void forceEndGameWithWinner(Team winner) {
        state = ArenaState.ENDING;

        // Cleanup game manager
        if (gameManager != null) {
            gameManager.cleanup();
        }

        String winnerName = winner == Team.RED ? plugin.getMainConfig().getRedTeamName()
                : plugin.getMainConfig().getBlueTeamName();
        broadcast(Messages.matchWin(winnerName));

        // Return all players after a short delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (java.util.UUID uuid : new java.util.HashSet<>(players.keySet())) {
                Player p = plugin.getServer().getPlayer(uuid);
                if (p != null) {
                    // Restore and remove
                    SavedPlayerState saved = savedStates.remove(uuid);
                    if (saved != null) {
                        saved.restore(p);
                    }
                }
            }
            players.clear();
            reset();
        }, 100L); // 5 seconds
    }

    /**
     * Assign player to a team (balances teams).
     */
    private void assignTeam(PlayerData data) {
        long redCount = players.values().stream().filter(p -> p.getTeam() == Team.RED).count();
        long blueCount = players.values().stream().filter(p -> p.getTeam() == Team.BLUE).count();

        if (redCount <= blueCount) {
            data.setTeam(Team.RED);
        } else {
            data.setTeam(Team.BLUE);
        }
    }

    /**
     * Check if we have enough players to start.
     */
    private void checkStart() {
        if (state != ArenaState.WAITING)
            return;
        if (players.size() >= map.getMinPlayers()) {
            startCountdown();
        }
    }

    /**
     * Start countdown to game.
     */
    private void startCountdown() {
        state = ArenaState.COUNTDOWN;
        int countdown = plugin.getMainConfig().getCountdownTime();

        broadcast(Messages.PREFIX.append(
                net.kyori.adventure.text.Component.text("試合開始まで " + countdown + " 秒！",
                        net.kyori.adventure.text.format.NamedTextColor.GREEN)));

        // Countdown task
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (state == ArenaState.COUNTDOWN) {
                startGame();
            }
        }, countdown * 20L);
    }

    /**
     * Start the game.
     */
    public void startGame() {
        if (players.size() < map.getMinPlayers()) {
            state = ArenaState.WAITING;
            broadcast(Messages.PREFIX.append(
                    net.kyori.adventure.text.Component.text("プレイヤー不足のため試合を開始できません。",
                            net.kyori.adventure.text.format.NamedTextColor.RED)));
            return;
        }

        gameManager = new GameManager(this);
        gameManager.startMatch();
    }

    /**
     * End the game and reset arena.
     */
    public void endGame() {
        state = ArenaState.ENDING;

        // Return all players to lobby after a delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Kick all players (restore their state)
            for (UUID uuid : new HashSet<>(players.keySet())) {
                Player player = plugin.getServer().getPlayer(uuid);
                if (player != null) {
                    leave(player);
                }
            }
            reset();
        }, 100L); // 5 seconds
    }

    /**
     * Reset arena to waiting state.
     */
    public void reset() {
        state = ArenaState.WAITING;
        gameManager = null;
        // Note: players map is cleared via leave()
    }

    /**
     * Broadcast message to all players in arena.
     */
    public void broadcast(net.kyori.adventure.text.Component message) {
        for (UUID uuid : players.keySet()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * Teleport all players to their spawn points based on role.
     */
    public void teleportToSpawns() {
        if (gameManager == null)
            return;

        for (PlayerData data : players.values()) {
            Player player = data.getPlayer();
            if (player == null || !player.isOnline())
                continue;

            Location spawn = gameManager.getSpawnForPlayer(data);
            if (spawn != null) {
                player.teleport(spawn);
            }
        }
    }

    /**
     * Teleport all players to lobby.
     */
    public void teleportToLobby() {
        Location lobby = map.getLobbySpawn();
        if (lobby == null)
            return;

        for (PlayerData data : players.values()) {
            Player player = data.getPlayer();
            if (player != null && player.isOnline()) {
                player.teleport(lobby);
            }
        }
    }

    // Getters
    public SearchAndDestroy getPlugin() {
        return plugin;
    }

    public SndMap getMap() {
        return map;
    }

    public ArenaState getState() {
        return state;
    }

    public void setState(ArenaState state) {
        this.state = state;
    }

    public Map<UUID, PlayerData> getPlayers() {
        return players;
    }

    public PlayerData getPlayerData(Player player) {
        return players.get(player.getUniqueId());
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public List<PlayerData> getPlayersOnTeam(Team team) {
        return players.values().stream()
                .filter(p -> p.getTeam() == team)
                .toList();
    }

    public List<PlayerData> getAlivePlayers() {
        return players.values().stream()
                .filter(PlayerData::isAlive)
                .toList();
    }

    public List<PlayerData> getAlivePlayersOnTeam(Team team) {
        return players.values().stream()
                .filter(p -> p.getTeam() == team && p.isAlive())
                .toList();
    }

    /**
     * Saved player state for restoration on leave.
     */
    private record SavedPlayerState(
            Location location,
            ItemStack[] inventory,
            ItemStack[] armor,
            GameMode gameMode,
            double health,
            int foodLevel,
            float exp,
            int level) {
        SavedPlayerState(Player player) {
            this(
                    player.getLocation().clone(),
                    player.getInventory().getContents().clone(),
                    player.getInventory().getArmorContents().clone(),
                    player.getGameMode(),
                    player.getHealth(),
                    player.getFoodLevel(),
                    player.getExp(),
                    player.getLevel());
        }

        void restore(Player player) {
            player.teleport(location);
            player.getInventory().setContents(inventory);
            player.getInventory().setArmorContents(armor);
            player.setGameMode(gameMode);
            player.setHealth(Math.min(health, player.getMaxHealth()));
            player.setFoodLevel(foodLevel);
            player.setExp(exp);
            player.setLevel(level);
        }
    }
}

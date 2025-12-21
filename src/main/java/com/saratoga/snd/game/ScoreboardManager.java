package com.saratoga.snd.game;

import com.saratoga.snd.Config;
import com.saratoga.snd.SearchAndDestroy;
import com.saratoga.snd.arena.ArenaState;
import com.saratoga.snd.arena.SndArena;
import com.saratoga.snd.arena.SndMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages scoreboards for arena players.
 */
public class ScoreboardManager {

    private final SearchAndDestroy plugin;
    private final SndArena arena;
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private BukkitTask updateTask;

    public ScoreboardManager(SearchAndDestroy plugin, SndArena arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    /**
     * Start updating scoreboards.
     */
    public void start() {
        // Create scoreboards for all players (each with just basic setup)
        for (PlayerData data : arena.getPlayers().values()) {
            Player player = data.getPlayer();
            if (player != null) {
                createScoreboardBasic(player);
            }
        }

        // Now sync ALL players to ALL scoreboards (this ensures everyone can see
        // everyone)
        syncAllPlayersToAllScoreboards();

        // Update every second
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 20L, 20L);
    }

    /**
     * Create a basic scoreboard without team assignments.
     * Sets up team visibility based on THIS player's team.
     */
    private void createScoreboardBasic(Player player) {
        PlayerData playerData = arena.getPlayerData(player);
        Team playerTeam = playerData != null ? playerData.getTeam() : Team.RED;

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("snd", Criteria.DUMMY,
                Component.text("Search & Destroy", NamedTextColor.GOLD, TextDecoration.BOLD));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Set up teams with visibility based on THIS player's team
        setupTeamsForPlayer(scoreboard, playerTeam);

        playerScoreboards.put(player.getUniqueId(), scoreboard);
        player.setScoreboard(scoreboard);
    }

    /**
     * Sync ALL players to ALL scoreboards.
     * This ensures every player is on the correct team on every other player's
     * scoreboard.
     */
    private void syncAllPlayersToAllScoreboards() {
        for (PlayerData data : arena.getPlayers().values()) {
            Player player = data.getPlayer();
            if (player == null)
                continue;

            // Add this player to ALL scoreboards (including their own)
            for (Scoreboard sb : playerScoreboards.values()) {
                assignToTeam(player, data.getTeam(), sb);
            }
        }
    }

    /**
     * Stop updating and clean up.
     */
    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        // Reset all scoreboards
        for (UUID uuid : playerScoreboards.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
        playerScoreboards.clear();
    }

    /**
     * Create scoreboard for a player (for dynamic adding).
     */
    public void createScoreboard(Player player, PlayerData data) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("snd", Criteria.DUMMY,
                Component.text("Search & Destroy", NamedTextColor.GOLD, TextDecoration.BOLD));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Set up teams with visibility based on THIS player's team
        setupTeamsForPlayer(scoreboard, data.getTeam());

        playerScoreboards.put(player.getUniqueId(), scoreboard);
        player.setScoreboard(scoreboard);

        // Assign player to team on this scoreboard
        assignToTeam(player, data.getTeam(), scoreboard);

        // Assign all other players too
        for (PlayerData otherData : arena.getPlayers().values()) {
            if (!otherData.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                assignToTeam(otherData.getPlayer(), otherData.getTeam(), scoreboard);
            }
        }
    }

    /**
     * Set up teams on scoreboard with visibility based on the viewing player's
     * team.
     */
    private void setupTeamsForPlayer(Scoreboard scoreboard, Team viewerTeam) {
        // Red team
        org.bukkit.scoreboard.Team redTeam = scoreboard.registerNewTeam("red");
        redTeam.color(NamedTextColor.RED);

        // Blue team
        org.bukkit.scoreboard.Team blueTeam = scoreboard.registerNewTeam("blue");
        blueTeam.color(NamedTextColor.BLUE);

        // Set visibility based on viewer's team
        // Own team: ALWAYS visible, Enemy team: NEVER visible
        if (viewerTeam == Team.RED) {
            redTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY,
                    org.bukkit.scoreboard.Team.OptionStatus.ALWAYS);
            blueTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY,
                    org.bukkit.scoreboard.Team.OptionStatus.NEVER);
        } else {
            blueTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY,
                    org.bukkit.scoreboard.Team.OptionStatus.ALWAYS);
            redTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY,
                    org.bukkit.scoreboard.Team.OptionStatus.NEVER);
        }
    }

    /**
     * Assign player to team on scoreboard.
     */
    private void assignToTeam(Player player, Team gameTeam, Scoreboard scoreboard) {
        String teamName = gameTeam == Team.RED ? "red" : "blue";
        org.bukkit.scoreboard.Team sbTeam = scoreboard.getTeam(teamName);
        if (sbTeam != null) {
            sbTeam.addPlayer(player);
        }
    }

    /**
     * Update all scoreboards.
     */
    private void updateAll() {
        GameManager game = arena.getGameManager();
        if (game == null)
            return;

        for (Map.Entry<UUID, Scoreboard> entry : playerScoreboards.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null)
                continue;

            PlayerData data = arena.getPlayerData(player);
            if (data == null)
                continue;

            updateScoreboard(player, data, entry.getValue(), game);
        }
    }

    /**
     * Update a single scoreboard.
     */
    private void updateScoreboard(Player player, PlayerData data, Scoreboard scoreboard, GameManager game) {
        Objective objective = scoreboard.getObjective("snd");
        if (objective == null)
            return;

        // Clear old scores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        Config config = plugin.getMainConfig();
        int line = 15;

        // Round
        objective.getScore("§e§lラウンド " + game.getCurrentRound() + "/" + config.getMaxRounds()).setScore(line--);
        objective.getScore("§7").setScore(line--);

        // Score
        objective.getScore("§c" + config.getRedTeamName() + ": §f" + game.getRedScore()).setScore(line--);
        objective.getScore("§9" + config.getBlueTeamName() + ": §f" + game.getBlueScore()).setScore(line--);
        objective.getScore("§7 ").setScore(line--);

        // Your role
        Role role = game.getRoleForTeam(data.getTeam());
        String roleStr = role == Role.ATTACKERS ? "§c攻撃側" : "§a防衛側";
        objective.getScore("§f役割: " + roleStr).setScore(line--);
        objective.getScore("§7  ").setScore(line--);

        // Bomb status
        Bomb bomb = game.getBomb();
        if (bomb != null) {
            Bomb.State bombState = bomb.getState();
            String bombStatus;

            switch (bombState) {
                case DROPPED -> bombStatus = "§6落ちている";
                case CARRIED -> {
                    // Only show carrier name to attackers
                    if (role == Role.ATTACKERS) {
                        Player carrier = Bukkit.getPlayer(bomb.getCarrier());
                        bombStatus = "§e所持: " + (carrier != null ? carrier.getName() : "???");
                    } else {
                        bombStatus = "§e攻撃側が所持";
                    }
                }
                case PLANTING -> bombStatus = "§c設置中...";
                case PLANTED, DEFUSING -> {
                    bombStatus = "§c§l設置済み [" + bomb.getPlantedSite() + "]";
                    // Show timer
                    int timer = bomb.getExplosionTimer();
                    objective.getScore("§c爆発まで: §f" + timer + "秒").setScore(line--);
                }
                case DEFUSED -> bombStatus = "§a解除済み";
                case EXPLODED -> bombStatus = "§4爆発";
                default -> bombStatus = "§7待機中";
            }
            objective.getScore("§f爆弾: " + bombStatus).setScore(line--);
        }
        objective.getScore("§7   ").setScore(line--);

        // Alive players
        int redAlive = arena.getAlivePlayersOnTeam(Team.RED).size();
        int blueAlive = arena.getAlivePlayersOnTeam(Team.BLUE).size();
        objective.getScore("§c生存: " + redAlive + " §f| §9" + blueAlive).setScore(line--);

        // Round time
        if (arena.getState() == ArenaState.PLAYING) {
            int time = game.getRoundTimeRemaining();
            String timeStr = String.format("%d:%02d", time / 60, time % 60);
            objective.getScore("§f残り時間: §e" + timeStr).setScore(line--);
        }
    }

    /**
     * Add player to scoreboards.
     * This creates their scoreboard and adds them to all other players'
     * scoreboards.
     */
    public void addPlayer(Player player, PlayerData data) {
        // First, add this player to all EXISTING scoreboards (before creating their
        // own)
        for (Map.Entry<UUID, Scoreboard> entry : playerScoreboards.entrySet()) {
            assignToTeam(player, data.getTeam(), entry.getValue());
        }

        // Now create scoreboard for this player
        createScoreboard(player, data);
    }

    /**
     * Remove player from ALL scoreboards.
     */
    public void removePlayer(Player player) {
        // Remove from all other players' scoreboards
        for (Map.Entry<UUID, Scoreboard> entry : playerScoreboards.entrySet()) {
            Scoreboard sb = entry.getValue();

            // Remove from both teams (we don't know which they were on)
            org.bukkit.scoreboard.Team redTeam = sb.getTeam("red");
            org.bukkit.scoreboard.Team blueTeam = sb.getTeam("blue");

            if (redTeam != null)
                redTeam.removePlayer(player);
            if (blueTeam != null)
                blueTeam.removePlayer(player);
        }

        // Remove their own scoreboard
        Scoreboard sb = playerScoreboards.remove(player.getUniqueId());
        if (sb != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }
}

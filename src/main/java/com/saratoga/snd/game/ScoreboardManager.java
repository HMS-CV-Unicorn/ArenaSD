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
        // Create scoreboards for all players
        for (PlayerData data : arena.getPlayers().values()) {
            Player player = data.getPlayer();
            if (player != null) {
                createScoreboard(player, data);
            }
        }

        // Update every second
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 20L, 20L);
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
     * Create scoreboard for a player.
     */
    public void createScoreboard(Player player, PlayerData data) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("snd", Criteria.DUMMY,
                Component.text("Search & Destroy", NamedTextColor.GOLD, TextDecoration.BOLD));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Set up teams for colored name tags
        setupTeams(scoreboard);

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
     * Set up teams on scoreboard.
     */
    private void setupTeams(Scoreboard scoreboard) {
        Config config = plugin.getMainConfig();

        // Red team
        org.bukkit.scoreboard.Team redTeam = scoreboard.registerNewTeam("red");
        redTeam.color(NamedTextColor.RED);
        redTeam.prefix(Component.text("[R] ", NamedTextColor.RED));
        redTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY,
                org.bukkit.scoreboard.Team.OptionStatus.FOR_OWN_TEAM);

        // Blue team
        org.bukkit.scoreboard.Team blueTeam = scoreboard.registerNewTeam("blue");
        blueTeam.color(NamedTextColor.BLUE);
        blueTeam.prefix(Component.text("[B] ", NamedTextColor.BLUE));
        blueTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY,
                org.bukkit.scoreboard.Team.OptionStatus.FOR_OWN_TEAM);
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
     */
    public void addPlayer(Player player, PlayerData data) {
        createScoreboard(player, data);

        // Add this player to all other scoreboards
        for (Map.Entry<UUID, Scoreboard> entry : playerScoreboards.entrySet()) {
            if (!entry.getKey().equals(player.getUniqueId())) {
                assignToTeam(player, data.getTeam(), entry.getValue());
            }
        }
    }

    /**
     * Remove player from scoreboards.
     */
    public void removePlayer(Player player) {
        Scoreboard sb = playerScoreboards.remove(player.getUniqueId());
        if (sb != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }
}

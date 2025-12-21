package com.saratoga.snd.game;

import com.saratoga.snd.Config;
import com.saratoga.snd.Messages;
import com.saratoga.snd.SearchAndDestroy;
import com.saratoga.snd.arena.ArenaState;
import com.saratoga.snd.arena.SndArena;
import com.saratoga.snd.arena.SndMap;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/**
 * Manages the game logic for a match.
 */
public class GameManager {

    private final SndArena arena;
    private final SearchAndDestroy plugin;
    private final Config config;

    // Scores
    private int redScore = 0;
    private int blueScore = 0;

    // Round info
    private int currentRound = 0;
    private Team attackingTeam = Team.RED; // RED starts as attackers

    // Bomb
    private Bomb bomb;

    // Round timer
    private int roundTimeRemaining;
    private BukkitTask roundTimerTask;

    public GameManager(SndArena arena) {
        this.arena = arena;
        this.plugin = arena.getPlugin();
        this.config = plugin.getMainConfig();
    }

    /**
     * Start the match.
     */
    public void startMatch() {
        this.redScore = 0;
        this.blueScore = 0;
        this.currentRound = 0;
        this.attackingTeam = Team.RED;

        startNextRound();
    }

    /**
     * Start the next round.
     */
    public void startNextRound() {
        currentRound++;

        // Check for side swap
        if (currentRound == config.getSwapSidesAfter() + 1) {
            swapSides();
        }

        arena.broadcast(Messages.roundStart(currentRound));
        arena.broadcast(Messages.score(redScore, blueScore));

        // Reset all players for new round
        for (PlayerData data : arena.getPlayers().values()) {
            data.resetForRound();
            Player player = data.getPlayer();
            if (player != null && player.isOnline()) {
                player.setGameMode(GameMode.ADVENTURE);
                player.getInventory().clear();
                player.setHealth(player.getMaxHealth());
            }
        }

        // Teleport to lobby first
        arena.teleportToLobby();
        arena.setState(ArenaState.INTERMISSION);

        // Wait in lobby, then start round
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            arena.setState(ArenaState.PLAYING);

            // Teleport to spawns
            arena.teleportToSpawns();

            // Spawn bomb
            spawnBomb();

            // Start round timer
            startRoundTimer();
        }, config.getLobbyWaitTime() * 20L);
    }

    /**
     * Spawn bomb at attacker spawn.
     */
    private void spawnBomb() {
        if (bomb != null) {
            bomb.cleanup();
        }

        bomb = new Bomb(plugin);
        Location attackerSpawn = arena.getMap().getAttackerSpawn();
        if (attackerSpawn != null) {
            bomb.spawn(attackerSpawn.clone().add(0, 1, 0));
        }
    }

    /**
     * Start round timer.
     */
    private void startRoundTimer() {
        roundTimeRemaining = config.getRoundTimeLimit();

        if (roundTimerTask != null) {
            roundTimerTask.cancel();
        }

        roundTimerTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            roundTimeRemaining--;

            // Time announcements
            if (roundTimeRemaining == 30 || roundTimeRemaining == 10 || roundTimeRemaining <= 5) {
                arena.broadcast(Messages.timeRemaining(roundTimeRemaining));
            }

            // Time's up
            if (roundTimeRemaining <= 0) {
                stopRoundTimer();

                // If bomb not planted, defenders win
                if (bomb == null || !bomb.isPlanted()) {
                    endRound(getDefendingTeam());
                }
                // If bomb is planted, it will explode via its own timer
            }
        }, 20L, 20L);
    }

    /**
     * Stop round timer.
     */
    private void stopRoundTimer() {
        if (roundTimerTask != null) {
            roundTimerTask.cancel();
            roundTimerTask = null;
        }
    }

    /**
     * Swap attacking and defending sides.
     */
    private void swapSides() {
        attackingTeam = attackingTeam.opposite();
        arena.broadcast(Messages.SIDES_SWAPPED);
    }

    /**
     * End the current round.
     */
    public void endRound(Team winner) {
        stopRoundTimer();
        if (bomb != null) {
            bomb.cleanup();
            bomb = null;
        }

        // Add score
        if (winner == Team.RED) {
            redScore++;
        } else {
            blueScore++;
        }

        String winnerName = winner == Team.RED ? config.getRedTeamName() : config.getBlueTeamName();
        arena.broadcast(Messages.roundWin(winnerName));
        arena.broadcast(Messages.score(redScore, blueScore));

        // Check for match win
        if (redScore >= config.getRoundsToWin() || blueScore >= config.getRoundsToWin()) {
            endMatch(winner);
            return;
        }

        // Check max rounds
        if (currentRound >= config.getMaxRounds()) {
            // Tie or whoever has more
            Team matchWinner = redScore > blueScore ? Team.RED : Team.BLUE;
            endMatch(matchWinner);
            return;
        }

        // Start next round
        startNextRound();
    }

    /**
     * End the match.
     */
    private void endMatch(Team winner) {
        String winnerName = winner == Team.RED ? config.getRedTeamName() : config.getBlueTeamName();
        arena.broadcast(Messages.matchWin(winnerName));

        arena.endGame();
    }

    /**
     * Handle player death.
     */
    public void onPlayerDeath(Player player) {
        PlayerData data = arena.getPlayerData(player);
        if (data == null)
            return;

        data.setAlive(false);

        // Drop bomb if carrying
        if (data.hasBomb() && bomb != null) {
            data.setHasBomb(false);
            bomb.drop(player.getLocation());
            arena.broadcast(Messages.BOMB_DROPPED);
        }

        // Find teammate to spectate
        List<PlayerData> aliveTeammates = arena.getAlivePlayersOnTeam(data.getTeam());
        if (!aliveTeammates.isEmpty()) {
            // Spectate a teammate
            Player target = aliveTeammates.get(0).getPlayer();
            if (target != null) {
                player.setGameMode(GameMode.SPECTATOR);
                player.setSpectatorTarget(target);
                data.setSpectatingTarget(target.getUniqueId());
                Messages.send(player, Messages.nowSpectating(target.getName()));
            }
        } else {
            // No teammates alive - go to lobby in adventure mode (not spectator)
            player.setGameMode(GameMode.ADVENTURE);
            Location lobby = arena.getMap().getLobbySpawn();
            if (lobby != null) {
                player.teleport(lobby);
            }
            Messages.send(player, Messages.PREFIX.append(
                    net.kyori.adventure.text.Component.text("待機部屋でラウンド終了をお待ちください。",
                            net.kyori.adventure.text.format.NamedTextColor.GRAY)));
        }

        // Check for team elimination
        checkTeamElimination();
    }

    /**
     * Handle player leave.
     */
    public void onPlayerLeave(Player player) {
        PlayerData data = arena.getPlayerData(player);
        if (data == null)
            return;

        Team leavingTeam = data.getTeam();

        // Drop bomb if carrying
        if (data.hasBomb() && bomb != null) {
            bomb.drop(player.getLocation());
            arena.broadcast(Messages.BOMB_DROPPED);
        }

        // Check if team now has zero players (they already left the players map)
        // We need to check after the player is actually removed
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            checkTeamMembersAfterLeave(leavingTeam);
        });
    }

    /**
     * Check if a team has no members after a player leaves.
     */
    private void checkTeamMembersAfterLeave(Team leavingTeam) {
        if (arena.getState() != ArenaState.PLAYING && arena.getState() != ArenaState.INTERMISSION)
            return;

        // Count all players on each team (not just alive ones)
        List<PlayerData> redPlayers = arena.getPlayersOnTeam(Team.RED);
        List<PlayerData> bluePlayers = arena.getPlayersOnTeam(Team.BLUE);

        if (redPlayers.isEmpty() && bluePlayers.isEmpty()) {
            // Everyone left - force end game
            arena.forceEndGame();
        } else if (redPlayers.isEmpty()) {
            // Red team has no players - Blue wins the match
            arena.broadcast(Messages.PREFIX.append(
                    net.kyori.adventure.text.Component.text(
                            config.getRedTeamName() + "が退出しました。" + config.getBlueTeamName() + "の勝利！",
                            net.kyori.adventure.text.format.NamedTextColor.GOLD)));
            forceEndMatch(Team.BLUE);
        } else if (bluePlayers.isEmpty()) {
            // Blue team has no players - Red wins the match
            arena.broadcast(Messages.PREFIX.append(
                    net.kyori.adventure.text.Component.text(
                            config.getBlueTeamName() + "が退出しました。" + config.getRedTeamName() + "の勝利！",
                            net.kyori.adventure.text.format.NamedTextColor.GOLD)));
            forceEndMatch(Team.RED);
        } else {
            // Both teams still have players - check alive status
            checkTeamElimination();
        }
    }

    /**
     * Force end the match immediately.
     */
    private void forceEndMatch(Team winner) {
        stopRoundTimer();
        if (bomb != null) {
            bomb.cleanup();
            bomb = null;
        }
        String winnerName = winner == Team.RED ? config.getRedTeamName() : config.getBlueTeamName();
        arena.broadcast(Messages.matchWin(winnerName));
        arena.endGame();
    }

    /**
     * Check if a team is eliminated.
     */
    private void checkTeamElimination() {
        if (arena.getState() != ArenaState.PLAYING)
            return;

        List<PlayerData> aliveRed = arena.getAlivePlayersOnTeam(Team.RED);
        List<PlayerData> aliveBlue = arena.getAlivePlayersOnTeam(Team.BLUE);

        if (aliveRed.isEmpty() && aliveBlue.isEmpty()) {
            // Somehow both teams dead - defenders win
            endRound(getDefendingTeam());
        } else if (aliveRed.isEmpty()) {
            // Red eliminated
            arena.broadcast(Messages.teamEliminated(config.getRedTeamName()));

            // If bomb is planted and red was attacking, bomb still ticks
            if (bomb != null && bomb.isPlanted() && attackingTeam == Team.RED) {
                // Bomb continues, defenders must defuse
            } else {
                endRound(Team.BLUE);
            }
        } else if (aliveBlue.isEmpty()) {
            // Blue eliminated
            arena.broadcast(Messages.teamEliminated(config.getBlueTeamName()));

            // If bomb is planted and blue was attacking, bomb still ticks
            if (bomb != null && bomb.isPlanted() && attackingTeam == Team.BLUE) {
                // Bomb continues, defenders must defuse
            } else {
                endRound(Team.RED);
            }
        }
    }

    /**
     * Handle bomb planted.
     */
    public void onBombPlanted(String siteName, Location location) {
        arena.broadcast(Messages.BOMB_PLANTED);

        // Stop round timer - bomb timer takes over
        stopRoundTimer();

        bomb.plant(siteName, location, () -> {
            // Bomb exploded
            arena.broadcast(Messages.BOMB_EXPLODED);
            endRound(attackingTeam);
        });
    }

    /**
     * Handle bomb defused.
     */
    public void onBombDefused() {
        arena.broadcast(Messages.BOMB_DEFUSED);
        bomb.defuse();
        endRound(getDefendingTeam());
    }

    /**
     * Get spawn location for a player based on their team and current roles.
     */
    public Location getSpawnForPlayer(PlayerData data) {
        SndMap map = arena.getMap();
        Role role = getRoleForTeam(data.getTeam());

        if (role == Role.ATTACKERS) {
            return map.getAttackerSpawn();
        } else {
            return map.getDefenderSpawn();
        }
    }

    /**
     * Get role for a team.
     */
    public Role getRoleForTeam(Team team) {
        if (team == attackingTeam) {
            return Role.ATTACKERS;
        } else {
            return Role.DEFENDERS;
        }
    }

    /**
     * Get defending team.
     */
    public Team getDefendingTeam() {
        return attackingTeam.opposite();
    }

    // Getters
    public int getRedScore() {
        return redScore;
    }

    public int getBlueScore() {
        return blueScore;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public Team getAttackingTeam() {
        return attackingTeam;
    }

    public Bomb getBomb() {
        return bomb;
    }

    public int getRoundTimeRemaining() {
        return roundTimeRemaining;
    }
}

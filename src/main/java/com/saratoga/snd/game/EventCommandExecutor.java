package com.saratoga.snd.game;

import com.saratoga.snd.SearchAndDestroy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Executes configured commands on game events.
 */
public class EventCommandExecutor {

    private final SearchAndDestroy plugin;
    private final Logger logger;

    public EventCommandExecutor(SearchAndDestroy plugin) {
        this.plugin = plugin;
        this.logger = plugin.getSLF4JLogger();
    }

    /**
     * Execute commands for an event with a single target player.
     * 
     * @param commands     List of command strings with [console]/[player]/[op]
     *                     prefixes
     * @param placeholders Map of placeholder -> replacement values
     * @param targetPlayer Player context (for [player]/[op] commands), can be null
     */
    public void executeCommands(List<String> commands, Map<String, String> placeholders, Player targetPlayer) {
        if (commands == null || commands.isEmpty()) {
            return;
        }

        for (String cmd : commands) {
            // Replace placeholders
            String finalCmd = cmd;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                finalCmd = finalCmd.replace(entry.getKey(), entry.getValue());
            }

            executeCommand(finalCmd, targetPlayer);
        }
    }

    /**
     * Execute commands for an event, applying [player]/[op] commands to ALL
     * specified players.
     * Used for broadcast events (game-start, round-start, game-end) where
     * commands should run for every player in the arena.
     * 
     * @param commands     List of command strings with [console]/[player]/[op]
     *                     prefixes
     * @param placeholders Map of placeholder -> replacement values
     * @param allPlayers   Collection of players to execute [player]/[op] commands
     *                     for
     */
    public void executeCommandsForAll(List<String> commands, Map<String, String> placeholders,
            Collection<Player> allPlayers) {
        if (commands == null || commands.isEmpty()) {
            return;
        }

        for (String cmd : commands) {
            // Replace placeholders (excluding <player> which is per-player)
            String baseCmd = cmd;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                if (!entry.getKey().equals("<player>")) {
                    baseCmd = baseCmd.replace(entry.getKey(), entry.getValue());
                }
            }

            if (baseCmd.startsWith("[console] ")) {
                // Console commands: if <player> placeholder exists, run once per player
                if (baseCmd.contains("<player>")) {
                    for (Player p : allPlayers) {
                        if (p != null && p.isOnline()) {
                            String playerCmd = baseCmd.replace("<player>", p.getName());
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), playerCmd.substring(10));
                        }
                    }
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), baseCmd.substring(10));
                }
            } else if (baseCmd.startsWith("[player] ") || baseCmd.startsWith("[op] ")) {
                // Player/Op commands: run for each player
                for (Player p : allPlayers) {
                    if (p != null && p.isOnline()) {
                        String playerCmd = baseCmd.replace("<player>", p.getName());
                        executeCommand(playerCmd, p);
                    }
                }
            } else {
                // Default: console execution
                if (baseCmd.contains("<player>")) {
                    for (Player p : allPlayers) {
                        if (p != null && p.isOnline()) {
                            String playerCmd = baseCmd.replace("<player>", p.getName());
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), playerCmd);
                        }
                    }
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), baseCmd);
                }
            }
        }
    }

    private void executeCommand(String command, Player targetPlayer) {
        if (command.startsWith("[console] ")) {
            String cmd = command.substring(10);
            logger.info("[EventCmd] Executing console command: {}", cmd);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } else if (command.startsWith("[player] ")) {
            String cmd = command.substring(9);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                logger.info("[EventCmd] Player {} executing: {}", targetPlayer.getName(), cmd);
                targetPlayer.performCommand(cmd);
            } else {
                logger.warn("[EventCmd] Skipped [player] command (no target): {}", cmd);
            }
        } else if (command.startsWith("[op] ")) {
            String cmd = command.substring(5);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                logger.info("[EventCmd] Player {} executing as OP: {}", targetPlayer.getName(), cmd);
                executeAsOp(targetPlayer, cmd);
            } else {
                logger.warn("[EventCmd] Skipped [op] command (no target): {}", cmd);
            }
        } else {
            // Default: console execution
            logger.info("[EventCmd] Executing (no prefix) as console: {}", command);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    /**
     * Execute command as if player had OP permissions.
     * Temporarily grants OP, executes, then revokes if they weren't OP before.
     */
    private void executeAsOp(Player player, String command) {
        boolean wasOp = player.isOp();
        try {
            if (!wasOp) {
                player.setOp(true);
            }
            player.performCommand(command);
        } finally {
            if (!wasOp) {
                player.setOp(false);
            }
        }
    }
}

package com.saratoga.snd.game;

import com.saratoga.snd.SearchAndDestroy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Executes configured commands on game events.
 */
public class EventCommandExecutor {

    private final SearchAndDestroy plugin;

    public EventCommandExecutor(SearchAndDestroy plugin) {
        this.plugin = plugin;
    }

    /**
     * Execute commands for an event.
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

    private void executeCommand(String command, Player targetPlayer) {
        if (command.startsWith("[console] ")) {
            String cmd = command.substring(10);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } else if (command.startsWith("[player] ")) {
            String cmd = command.substring(9);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.performCommand(cmd);
            }
        } else if (command.startsWith("[op] ")) {
            String cmd = command.substring(5);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                executeAsOp(targetPlayer, cmd);
            }
        } else {
            // Default: console execution
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

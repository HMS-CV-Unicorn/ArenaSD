package com.saratoga.snd.command;

import com.saratoga.snd.Messages;
import com.saratoga.snd.SearchAndDestroy;
import com.saratoga.snd.arena.SndArena;
import com.saratoga.snd.arena.SndMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main command executor for /snd.
 */
public class SndCommand implements CommandExecutor, TabCompleter {

    private final SearchAndDestroy plugin;

    public SndCommand(SearchAndDestroy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "join" -> handleJoin(sender, args);
            case "leave" -> handleLeave(sender);
            case "list" -> handleList(sender);
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "setup" -> handleSetup(sender, args);
            case "reload" -> handleReload(sender);
            case "info" -> handleInfo(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Search and Destroy ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/snd join [map]", NamedTextColor.YELLOW)
                .append(Component.text(" - 自動マッチ or マップ指定で参加", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/snd leave", NamedTextColor.YELLOW)
                .append(Component.text(" - 退出", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/snd list", NamedTextColor.YELLOW)
                .append(Component.text(" - マップ一覧", NamedTextColor.GRAY)));

        if (sender.hasPermission("snd.admin")) {
            sender.sendMessage(Component.text("/snd create <name>", NamedTextColor.YELLOW)
                    .append(Component.text(" - マップ作成", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("/snd setup <type> [args]", NamedTextColor.YELLOW)
                    .append(Component.text(" - セットアップ", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("/snd delete <name>", NamedTextColor.YELLOW)
                    .append(Component.text(" - マップ削除", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("/snd reload", NamedTextColor.YELLOW)
                    .append(Component.text(" - リロード", NamedTextColor.GRAY)));
        }
    }

    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("プレイヤーのみ実行可能です。", NamedTextColor.RED));
            return;
        }

        if (plugin.getArenaManager().isInArena(player)) {
            Messages.send(player, Messages.ALREADY_IN_GAME);
            return;
        }

        // Explicit map specified
        if (args.length >= 2) {
            String mapName = args[1];
            SndMap map = plugin.getArenaManager().getMap(mapName);
            if (map == null) {
                Messages.send(player, Messages.ARENA_NOT_FOUND);
                return;
            }
            if (!map.isReady()) {
                Messages.send(player, Messages.ARENA_NOT_READY);
                return;
            }
            if (plugin.getArenaManager().joinArena(player, mapName)) {
                Messages.send(player, Messages.PREFIX.append(
                        Component.text(map.getName() + " に参加しました！", NamedTextColor.GREEN)));
            }
            return;
        }

        // Auto-matchmaking (no map specified)
        var result = plugin.getArenaManager().autoJoin(player);
        if (result.success()) {
            Messages.send(player, Messages.PREFIX.append(
                    Component.text(result.mapName() + " に参加しました！", NamedTextColor.GREEN)));
        } else {
            Messages.send(player, Messages.NO_AVAILABLE_GAMES);
        }
    }

    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("プレイヤーのみ実行可能です。", NamedTextColor.RED));
            return;
        }

        if (!plugin.getArenaManager().isInArena(player)) {
            Messages.send(player, Messages.NOT_IN_GAME);
            return;
        }

        plugin.getArenaManager().leaveArena(player);
        Messages.send(player, Messages.LEFT_GAME);
    }

    private void handleList(CommandSender sender) {
        var maps = plugin.getArenaManager().getMaps();
        if (maps.isEmpty()) {
            sender.sendMessage(Component.text("利用可能なマップがありません。", NamedTextColor.YELLOW));
            return;
        }

        sender.sendMessage(Component.text("=== 利用可能なマップ ===", NamedTextColor.GOLD));
        for (SndMap map : maps) {
            String status = map.isReady() ? "§a[準備完了]" : "§c[セットアップ未完了]";
            sender.sendMessage(Component.text("- " + map.getName() + " " + status, NamedTextColor.WHITE));
        }
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("snd.admin")) {
            sender.sendMessage(Component.text("権限がありません。", NamedTextColor.RED));
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("プレイヤーのみ実行可能です。", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("使用法: /snd create <name>", NamedTextColor.RED));
            return;
        }

        String name = args[1];
        String worldName = player.getWorld().getName();

        try {
            SndMap map = plugin.getArenaManager().createMap(name, worldName);
            if (map == null) {
                Messages.send(player, Messages.ARENA_ALREADY_EXISTS);
                return;
            }
            Messages.send(player, Messages.ARENA_CREATED);
            sender.sendMessage(Component.text("次に以下のセットアップを行ってください:", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("  /snd setup lobby", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /snd setup spawn attackers", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /snd setup spawn defenders", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /snd setup site A", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /snd setup site B", NamedTextColor.GRAY));
        } catch (IOException e) {
            sender.sendMessage(Component.text("マップの作成に失敗しました: " + e.getMessage(), NamedTextColor.RED));
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("snd.admin")) {
            sender.sendMessage(Component.text("権限がありません。", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("使用法: /snd delete <name>", NamedTextColor.RED));
            return;
        }

        String name = args[1];
        if (plugin.getArenaManager().deleteMap(name)) {
            sender.sendMessage(Messages.ARENA_DELETED);
        } else {
            sender.sendMessage(Messages.ARENA_NOT_FOUND);
        }
    }

    private void handleSetup(CommandSender sender, String[] args) {
        if (!sender.hasPermission("snd.admin")) {
            sender.sendMessage(Component.text("権限がありません。", NamedTextColor.RED));
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("プレイヤーのみ実行可能です。", NamedTextColor.RED));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text("使用法:", NamedTextColor.RED));
            sender.sendMessage(Component.text("  /snd setup lobby <map>", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /snd setup spawn <attackers|defenders> <map>", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  /snd setup site <A|B> <map>", NamedTextColor.GRAY));
            return;
        }

        String type = args[1].toLowerCase();
        String mapName;
        SndMap map;

        switch (type) {
            case "lobby" -> {
                mapName = args[2];
                map = plugin.getArenaManager().getMap(mapName);
                if (map == null) {
                    Messages.send(player, Messages.ARENA_NOT_FOUND);
                    return;
                }
                map.setLobbySpawn(player.getLocation());
                saveMap(player, map);
                Messages.send(player, Messages.LOBBY_SET);
            }
            case "spawn" -> {
                if (args.length < 4) {
                    sender.sendMessage(
                            Component.text("使用法: /snd setup spawn <attackers|defenders> <map>", NamedTextColor.RED));
                    return;
                }
                String spawnType = args[2].toLowerCase();
                mapName = args[3];
                map = plugin.getArenaManager().getMap(mapName);
                if (map == null) {
                    Messages.send(player, Messages.ARENA_NOT_FOUND);
                    return;
                }

                if (spawnType.equals("attackers")) {
                    map.setAttackerSpawn(player.getLocation());
                } else if (spawnType.equals("defenders")) {
                    map.setDefenderSpawn(player.getLocation());
                } else {
                    sender.sendMessage(Component.text("attackers または defenders を指定してください。", NamedTextColor.RED));
                    return;
                }
                saveMap(player, map);
                Messages.send(player, Messages.SPAWN_SET);
            }
            case "site" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("使用法: /snd setup site <A|B> <map>", NamedTextColor.RED));
                    return;
                }
                String siteName = args[2].toUpperCase();
                mapName = args[3];
                map = plugin.getArenaManager().getMap(mapName);
                if (map == null) {
                    Messages.send(player, Messages.ARENA_NOT_FOUND);
                    return;
                }

                if (!siteName.equals("A") && !siteName.equals("B")) {
                    sender.sendMessage(Component.text("A または B を指定してください。", NamedTextColor.RED));
                    return;
                }

                map.setBombSite(siteName, new SndMap.BombSite(player.getLocation(), 5.0));
                saveMap(player, map);
                Messages.send(player, Messages.BOMB_SITE_SET);
            }
            default -> sender.sendMessage(Component.text("不明なセットアップタイプです。", NamedTextColor.RED));
        }
    }

    private void saveMap(Player player, SndMap map) {
        try {
            map.save();
        } catch (IOException e) {
            player.sendMessage(Component.text("保存に失敗しました: " + e.getMessage(), NamedTextColor.RED));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("snd.admin")) {
            sender.sendMessage(Component.text("権限がありません。", NamedTextColor.RED));
            return;
        }

        plugin.getMainConfig().reload();
        plugin.getArenaManager().loadMaps();
        sender.sendMessage(Component.text("設定をリロードしました。", NamedTextColor.GREEN));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("使用法: /snd info <map>", NamedTextColor.RED));
            return;
        }

        SndMap map = plugin.getArenaManager().getMap(args[1]);
        if (map == null) {
            sender.sendMessage(Messages.ARENA_NOT_FOUND);
            return;
        }

        sender.sendMessage(Component.text("=== " + map.getName() + " ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("ワールド: " + map.getWorldName(), NamedTextColor.WHITE));
        sender.sendMessage(Component.text("ロビー: " + (map.getLobbySpawn() != null ? "設定済み" : "未設定"),
                map.getLobbySpawn() != null ? NamedTextColor.GREEN : NamedTextColor.RED));
        sender.sendMessage(Component.text("攻撃側スポーン: " + (map.getAttackerSpawn() != null ? "設定済み" : "未設定"),
                map.getAttackerSpawn() != null ? NamedTextColor.GREEN : NamedTextColor.RED));
        sender.sendMessage(Component.text("防衛側スポーン: " + (map.getDefenderSpawn() != null ? "設定済み" : "未設定"),
                map.getDefenderSpawn() != null ? NamedTextColor.GREEN : NamedTextColor.RED));
        sender.sendMessage(Component.text("サイトA: " + (map.getBombSite("A") != null ? "設定済み" : "未設定"),
                map.getBombSite("A") != null ? NamedTextColor.GREEN : NamedTextColor.RED));
        sender.sendMessage(Component.text("サイトB: " + (map.getBombSite("B") != null ? "設定済み" : "未設定"),
                map.getBombSite("B") != null ? NamedTextColor.GREEN : NamedTextColor.RED));
        sender.sendMessage(Component.text("準備状態: " + (map.isReady() ? "準備完了" : "セットアップ未完了"),
                map.isReady() ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("join", "leave", "list"));
            if (sender.hasPermission("snd.admin")) {
                subs.addAll(Arrays.asList("create", "delete", "setup", "reload", "info"));
            }
            String input = args[0].toLowerCase();
            for (String s : subs) {
                if (s.startsWith(input))
                    completions.add(s);
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("join") || sub.equals("delete") || sub.equals("info")) {
                for (String name : plugin.getArenaManager().getMapNames()) {
                    if (name.startsWith(args[1].toLowerCase()))
                        completions.add(name);
                }
            } else if (sub.equals("setup") && sender.hasPermission("snd.admin")) {
                for (String s : Arrays.asList("lobby", "spawn", "site")) {
                    if (s.startsWith(args[1].toLowerCase()))
                        completions.add(s);
                }
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            String type = args[1].toLowerCase();
            if (sub.equals("setup") && sender.hasPermission("snd.admin")) {
                if (type.equals("spawn")) {
                    for (String s : Arrays.asList("attackers", "defenders")) {
                        if (s.startsWith(args[2].toLowerCase()))
                            completions.add(s);
                    }
                } else if (type.equals("site")) {
                    for (String s : Arrays.asList("A", "B")) {
                        if (s.startsWith(args[2].toUpperCase()))
                            completions.add(s);
                    }
                } else if (type.equals("lobby")) {
                    for (String name : plugin.getArenaManager().getMapNames()) {
                        if (name.startsWith(args[2].toLowerCase()))
                            completions.add(name);
                    }
                }
            }
        } else if (args.length == 4) {
            String sub = args[0].toLowerCase();
            String type = args[1].toLowerCase();
            if (sub.equals("setup") && (type.equals("spawn") || type.equals("site"))) {
                for (String name : plugin.getArenaManager().getMapNames()) {
                    if (name.startsWith(args[3].toLowerCase()))
                        completions.add(name);
                }
            }
        }

        return completions;
    }
}

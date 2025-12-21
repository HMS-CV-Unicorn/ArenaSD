package com.saratoga.snd;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

/**
 * All messages used in SearchAndDestroy.
 */
public final class Messages {

    private Messages() {
    }

    // Prefixes
    public static final Component PREFIX = Component.text("[SND] ", NamedTextColor.GOLD);

    // Arena messages
    public static final Component ARENA_NOT_FOUND = PREFIX.append(
            Component.text("そのアリーナは存在しません。", NamedTextColor.RED));
    public static final Component ARENA_ALREADY_EXISTS = PREFIX.append(
            Component.text("そのアリーナは既に存在します。", NamedTextColor.RED));
    public static final Component ARENA_CREATED = PREFIX.append(
            Component.text("アリーナを作成しました。セットアップを完了してください。", NamedTextColor.GREEN));
    public static final Component ARENA_DELETED = PREFIX.append(
            Component.text("アリーナを削除しました。", NamedTextColor.YELLOW));
    public static final Component ARENA_NOT_READY = PREFIX.append(
            Component.text("アリーナのセットアップが完了していません。", NamedTextColor.RED));

    // Join/Leave
    public static final Component ALREADY_IN_GAME = PREFIX.append(
            Component.text("既に試合に参加しています。", NamedTextColor.RED));
    public static final Component NOT_IN_GAME = PREFIX.append(
            Component.text("試合に参加していません。", NamedTextColor.RED));
    public static final Component GAME_FULL = PREFIX.append(
            Component.text("試合が満員です。", NamedTextColor.RED));
    public static final Component LEFT_GAME = PREFIX.append(
            Component.text("試合から退出しました。", NamedTextColor.YELLOW));

    // Round messages
    public static Component roundStart(int round) {
        return PREFIX.append(Component.text("ラウンド " + round + " 開始！", NamedTextColor.GREEN, TextDecoration.BOLD));
    }

    public static Component roundWin(String team) {
        return PREFIX.append(Component.text(team + " がラウンドを獲得！", NamedTextColor.GOLD, TextDecoration.BOLD));
    }

    public static Component matchWin(String team) {
        return PREFIX.append(Component.text(team + " が試合に勝利！", NamedTextColor.GOLD, TextDecoration.BOLD));
    }

    public static Component score(int red, int blue) {
        return Component.text("スコア: ", NamedTextColor.WHITE)
                .append(Component.text("RED " + red, NamedTextColor.RED))
                .append(Component.text(" - ", NamedTextColor.WHITE))
                .append(Component.text(blue + " BLUE", NamedTextColor.BLUE));
    }

    // Bomb messages
    public static final Component BOMB_PLANTED = PREFIX.append(
            Component.text("爆弾が設置されました！", NamedTextColor.RED, TextDecoration.BOLD));
    public static final Component BOMB_DEFUSED = PREFIX.append(
            Component.text("爆弾が解除されました！", NamedTextColor.GREEN, TextDecoration.BOLD));
    public static final Component BOMB_EXPLODED = PREFIX.append(
            Component.text("爆弾が爆発しました！", NamedTextColor.DARK_RED, TextDecoration.BOLD));
    public static final Component BOMB_PICKED_UP = PREFIX.append(
            Component.text("爆弾を拾いました。", NamedTextColor.GOLD));
    public static final Component BOMB_DROPPED = PREFIX.append(
            Component.text("爆弾がドロップされました！", NamedTextColor.YELLOW));

    public static final Component PLANTING_BOMB = Component.text("爆弾設置中...", NamedTextColor.RED);
    public static final Component DEFUSING_BOMB = Component.text("爆弾解除中...", NamedTextColor.GREEN);

    public static final Component NOT_IN_BOMB_SITE = PREFIX.append(
            Component.text("爆弾サイト内でのみ設置できます。", NamedTextColor.RED));
    public static final Component ATTACKERS_ONLY = PREFIX.append(
            Component.text("攻撃側のみが爆弾を設置できます。", NamedTextColor.RED));
    public static final Component DEFENDERS_ONLY = PREFIX.append(
            Component.text("防衛側のみが爆弾を解除できます。", NamedTextColor.RED));

    // Team messages
    public static final Component SIDES_SWAPPED = PREFIX.append(
            Component.text("攻守交代！", NamedTextColor.GOLD, TextDecoration.BOLD));

    public static Component teamEliminated(String team) {
        return PREFIX.append(Component.text(team + " 全滅！", NamedTextColor.RED));
    }

    // Timer messages
    public static Component countdown(int seconds) {
        return Component.text(String.valueOf(seconds), NamedTextColor.YELLOW, TextDecoration.BOLD);
    }

    public static Component timeRemaining(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        return Component.text(String.format("%d:%02d", min, sec), NamedTextColor.WHITE);
    }

    // Setup messages
    public static final Component SPAWN_SET = PREFIX.append(
            Component.text("スポーン地点を設定しました。", NamedTextColor.GREEN));
    public static final Component LOBBY_SET = PREFIX.append(
            Component.text("ロビー地点を設定しました。", NamedTextColor.GREEN));
    public static final Component BOMB_SITE_SET = PREFIX.append(
            Component.text("爆弾サイトを設定しました。", NamedTextColor.GREEN));

    // Spectating
    public static Component nowSpectating(String playerName) {
        return PREFIX.append(Component.text(playerName + " を観戦中", NamedTextColor.GRAY));
    }

    // Utility
    public static void send(Player player, Component message) {
        player.sendMessage(message);
    }
}

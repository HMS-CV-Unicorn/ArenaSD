package com.saratoga.arenaSD;

import com.saratoga.arenaSD.arena.SdMap;
import com.saratoga.arenaSD.editor.SdEditorWizards;
import org.battleplugins.arena.Arena;
import org.battleplugins.arena.command.ArenaCommand;
import org.battleplugins.arena.command.ArenaCommandExecutor;
import org.battleplugins.arena.competition.map.CompetitionMap;
import org.bukkit.entity.Player;

/**
 * Command executor for Search and Destroy arena commands.
 * Provides setup commands for bomb spawn and bomb sites.
 */
public class SdExecutor extends ArenaCommandExecutor {

    public SdExecutor(Arena arena) {
        super(arena);
    }

    // ===== Basic test command (no map required) =====
    @ArenaCommand(commands = "test", description = "Test command to verify executor works.", permissionNode = "test")
    public void test(Player player) {
        player.sendMessage("§a[ArenaSD] コマンド動作確認中...  Executor is working");
        player.sendMessage("§7利用可能な設定コマンド:");
        player.sendMessage("§e  /snd bomb spawn <map>  §7- 爆弾スポーン位置");
        player.sendMessage("§e  /snd site a <map>      §7- サイトA位置");
        player.sendMessage("§e  /snd site b <map>      §7- サイトB位置");
        player.sendMessage("§e  /snd info <map>        §7- マップ情報");
    }

    @ArenaCommand(commands = "bomb", subCommands = "spawn", description = "Sets the bomb spawn location.", permissionNode = "bomb.spawn")
    public void setBombSpawn(Player player, CompetitionMap map) {
        if (!(map instanceof SdMap sdMap)) {
            SdMessages.NOT_SD_MAP.send(player);
            return;
        }

        SdEditorWizards.SET_BOMB_SPAWN.openWizard(player, this.arena, ctx -> {
            ctx.setMap(sdMap);
        });
    }

    @ArenaCommand(commands = "site", subCommands = "a", description = "Sets bomb site A location.", permissionNode = "site.set")
    public void setSiteA(Player player, CompetitionMap map) {
        setSite(player, map, "A");
    }

    @ArenaCommand(commands = "site", subCommands = "b", description = "Sets bomb site B location.", permissionNode = "site.set")
    public void setSiteB(Player player, CompetitionMap map) {
        setSite(player, map, "B");
    }

    private void setSite(Player player, CompetitionMap map, String siteName) {
        if (!(map instanceof SdMap sdMap)) {
            SdMessages.NOT_SD_MAP.send(player);
            return;
        }

        SdEditorWizards.ADD_BOMB_SITE.openWizard(player, this.arena, ctx -> {
            ctx.setMap(sdMap);
            ctx.setSiteName(siteName);
            ctx.setRadius(3.0); // Default radius
        });
    }

    @ArenaCommand(commands = "site", subCommands = "radius", description = "Sets bomb site radius. Usage: site radius <A|B> <radius>", permissionNode = "site.set")
    public void setSiteRadius(Player player, CompetitionMap map, String siteName, double radius) {
        if (!(map instanceof SdMap sdMap)) {
            SdMessages.NOT_SD_MAP.send(player);
            return;
        }

        SdMap.BombSite existingSite = sdMap.getBombSite(siteName);
        if (existingSite == null) {
            SdMessages.SITE_NOT_FOUND.send(player, siteName);
            return;
        }

        SdMap.BombSite updated = new SdMap.BombSite(existingSite.getPosition(), radius);
        sdMap.addBombSite(siteName, updated);

        try {
            sdMap.save();
            SdMessages.SITE_RADIUS_SET.send(player, siteName, String.valueOf(radius));
        } catch (Exception e) {
            player.sendMessage("Failed to save map: " + e.getMessage());
        }
    }

    @ArenaCommand(commands = "info", description = "Shows current map configuration.", permissionNode = "info")
    public void info(Player player, CompetitionMap map) {
        if (!(map instanceof SdMap sdMap)) {
            SdMessages.NOT_SD_MAP.send(player);
            return;
        }

        player.sendMessage("§6=== S&D Map Info: " + sdMap.getName() + " ===");

        if (sdMap.getBombSpawn() != null) {
            player.sendMessage("§aBomb Spawn: §f" + formatPosition(sdMap.getBombSpawn()));
        } else {
            player.sendMessage("§cBomb Spawn: §7Not set");
        }

        for (var entry : sdMap.getBombSites().entrySet()) {
            SdMap.BombSite site = entry.getValue();
            player.sendMessage("§aSite " + entry.getKey() + ": §f" + formatPosition(site.getPosition()) + " §7(radius: "
                    + site.getRadius() + ")");
        }

        if (sdMap.getBombSites().isEmpty()) {
            player.sendMessage("§cBomb Sites: §7None set (use /snd site a and /snd site b)");
        }
    }

    private String formatPosition(org.battleplugins.arena.util.PositionWithRotation pos) {
        return String.format("%.1f, %.1f, %.1f", pos.getX(), pos.getY(), pos.getZ());
    }
}

package com.saratoga.arenaSD.editor;

import com.saratoga.arenaSD.SdMessages;
import com.saratoga.arenaSD.arena.SdMap;
import org.battleplugins.arena.BattleArena;
import org.battleplugins.arena.config.ParseException;
import org.battleplugins.arena.editor.ArenaEditorWizard;
import org.battleplugins.arena.editor.ArenaEditorWizards;
import org.battleplugins.arena.editor.stage.SpawnInputStage;
import org.battleplugins.arena.messages.Messages;

import java.io.IOException;

/**
 * Editor wizards for setting up bomb sites and spawn locations.
 */
public final class SdEditorWizards {

    /**
     * Wizard for setting the bomb spawn location.
     */
    public static final ArenaEditorWizard<BombSpawnContext> SET_BOMB_SPAWN = ArenaEditorWizards
            .createWizard(BombSpawnContext::new)
            .addStage(BombSiteOption.BOMB_SPAWN, new SpawnInputStage<>(
                    SdMessages.EDITOR_SET_BOMB_SPAWN,
                    "bomb",
                    ctx -> ctx::setBombSpawn))
            .onCreationComplete(ctx -> {
                SdMap map = ctx.getMap();
                map.setBombSpawn(ctx.getBombSpawn());

                try {
                    map.save();
                } catch (ParseException | IOException e) {
                    BattleArena.getInstance().error("Failed to save map file for arena {}", ctx.getArena().getName(),
                            e);
                    Messages.MAP_FAILED_TO_SAVE.send(ctx.getPlayer(), map.getName());
                    return;
                }

                SdMessages.BOMB_SPAWN_SET.send(ctx.getPlayer());
            });

    /**
     * Wizard for adding a bomb site (A or B).
     */
    public static final ArenaEditorWizard<BombSiteContext> ADD_BOMB_SITE = ArenaEditorWizards
            .createWizard(BombSiteContext::new)
            .addStage(BombSiteOption.SITE_POSITION, new SpawnInputStage<>(
                    SdMessages.EDITOR_SET_SITE_POSITION,
                    "site",
                    ctx -> ctx::setSitePosition))
            .onCreationComplete(ctx -> {
                SdMap map = ctx.getMap();
                SdMap.BombSite site = new SdMap.BombSite(ctx.getSitePosition(), ctx.getRadius());

                map.addBombSite(ctx.getSiteName(), site);

                try {
                    map.save();
                } catch (ParseException | IOException e) {
                    BattleArena.getInstance().error("Failed to save map file for arena {}", ctx.getArena().getName(),
                            e);
                    Messages.MAP_FAILED_TO_SAVE.send(ctx.getPlayer(), map.getName());
                    return;
                }

                SdMessages.BOMB_SITE_SET.send(ctx.getPlayer(), ctx.getSiteName());
            });

    private SdEditorWizards() {
    }
}

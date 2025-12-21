package com.saratoga.arenaSD.editor;

import com.saratoga.arenaSD.arena.SdMap;
import org.battleplugins.arena.Arena;
import org.battleplugins.arena.editor.ArenaEditorWizard;
import org.battleplugins.arena.editor.EditorContext;
import org.battleplugins.arena.util.PositionWithRotation;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Editor context for setting the bomb spawn location.
 */
public class BombSpawnContext extends EditorContext<BombSpawnContext> {
    private SdMap map;
    private PositionWithRotation bombSpawn;

    public BombSpawnContext(ArenaEditorWizard<BombSpawnContext> wizard, Arena arena, Player player) {
        super(wizard, arena, player);
    }

    public SdMap getMap() {
        return this.map;
    }

    public void setMap(SdMap map) {
        this.map = map;
    }

    public PositionWithRotation getBombSpawn() {
        return this.bombSpawn;
    }

    public void setBombSpawn(Location bombSpawn) {
        this.bombSpawn = new PositionWithRotation(bombSpawn);
    }

    @Override
    public boolean isComplete() {
        return this.map != null && this.bombSpawn != null;
    }
}

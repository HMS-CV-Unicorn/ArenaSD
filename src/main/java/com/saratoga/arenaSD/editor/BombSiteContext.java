package com.saratoga.arenaSD.editor;

import com.saratoga.arenaSD.arena.SdMap;
import org.battleplugins.arena.Arena;
import org.battleplugins.arena.editor.ArenaEditorWizard;
import org.battleplugins.arena.editor.EditorContext;
import org.battleplugins.arena.util.PositionWithRotation;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Editor context for adding a bomb site (A or B).
 */
public class BombSiteContext extends EditorContext<BombSiteContext> {
    private SdMap map;
    private String siteName;
    private PositionWithRotation sitePosition;
    private double radius = 3.0;

    public BombSiteContext(ArenaEditorWizard<BombSiteContext> wizard, Arena arena, Player player) {
        super(wizard, arena, player);
    }

    public SdMap getMap() {
        return this.map;
    }

    public void setMap(SdMap map) {
        this.map = map;
    }

    public String getSiteName() {
        return this.siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public PositionWithRotation getSitePosition() {
        return this.sitePosition;
    }

    public void setSitePosition(Location sitePosition) {
        this.sitePosition = new PositionWithRotation(sitePosition);
    }

    public double getRadius() {
        return this.radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    @Override
    public boolean isComplete() {
        return this.map != null && this.siteName != null && this.sitePosition != null;
    }
}

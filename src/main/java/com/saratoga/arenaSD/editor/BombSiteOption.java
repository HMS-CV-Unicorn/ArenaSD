package com.saratoga.arenaSD.editor;

import org.battleplugins.arena.editor.type.EditorKey;

/**
 * Editor keys for bomb site configuration options.
 */
public enum BombSiteOption implements EditorKey {
    BOMB_SPAWN("bombSpawn"),
    SITE_POSITION("sitePosition"),
    SITE_RADIUS("siteRadius");

    private final String key;

    BombSiteOption(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return this.key;
    }
}

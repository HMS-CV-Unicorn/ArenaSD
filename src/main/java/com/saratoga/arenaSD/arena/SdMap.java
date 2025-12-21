package com.saratoga.arenaSD.arena;

import org.battleplugins.arena.Arena;
import org.battleplugins.arena.competition.LiveCompetition;
import org.battleplugins.arena.competition.map.LiveCompetitionMap;
import org.battleplugins.arena.competition.map.MapFactory;
import org.battleplugins.arena.competition.map.MapType;
import org.battleplugins.arena.competition.map.options.Bounds;
import org.battleplugins.arena.competition.map.options.Spawns;
import org.battleplugins.arena.config.ArenaOption;
import org.battleplugins.arena.util.PositionWithRotation;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Map configuration for Search and Destroy arenas.
 * Includes bomb spawn and bomb sites A/B.
 */
public class SdMap extends LiveCompetitionMap {

    static final MapFactory FACTORY = MapFactory.create(SdMap.class, SdMap::new);

    @ArenaOption(name = "bomb-spawn", description = "Location where the bomb spawns at the start of each round.")
    private PositionWithRotation bombSpawn;

    @ArenaOption(name = "bomb-sites", description = "Map of bomb sites (A and B) for planting the bomb.")
    private Map<String, BombSite> bombSites;

    public SdMap() {
    }

    public SdMap(String name, Arena arena, MapType type, String world, @Nullable Bounds bounds,
            @Nullable Spawns spawns) {
        super(name, arena, type, world, bounds, spawns);
        this.bombSites = new HashMap<>();
    }

    @Override
    public LiveCompetition<SdCompetition> createCompetition(Arena arena) {
        if (!(arena instanceof SdArena sdArena)) {
            throw new IllegalArgumentException("Arena must be an SdArena!");
        }

        return new SdCompetition(sdArena, arena.getType(), this);
    }

    @Nullable
    public PositionWithRotation getBombSpawn() {
        return this.bombSpawn;
    }

    public void setBombSpawn(PositionWithRotation bombSpawn) {
        this.bombSpawn = bombSpawn;
    }

    public Map<String, BombSite> getBombSites() {
        return this.bombSites != null ? this.bombSites : new HashMap<>();
    }

    @Nullable
    public BombSite getBombSite(String name) {
        return this.bombSites != null ? this.bombSites.get(name) : null;
    }

    public void addBombSite(String name, BombSite site) {
        if (this.bombSites == null) {
            this.bombSites = new HashMap<>();
        }
        this.bombSites.put(name, site);
    }

    /**
     * Represents a bomb site (A or B) where the bomb can be planted.
     */
    public static class BombSite {
        @ArenaOption(name = "position", description = "The center position of the bomb site.", required = true)
        private PositionWithRotation position;

        @ArenaOption(name = "radius", description = "The radius of the bomb site for plant/defuse detection.", required = true)
        private double radius;

        public BombSite() {
        }

        public BombSite(PositionWithRotation position, double radius) {
            this.position = position;
            this.radius = radius;
        }

        public PositionWithRotation getPosition() {
            return this.position;
        }

        public double getRadius() {
            return this.radius;
        }

        public boolean isInside(Location location, World world) {
            Location siteLocation = this.position.toLocation(world);
            return siteLocation.distanceSquared(location) <= (this.radius * this.radius);
        }
    }
}

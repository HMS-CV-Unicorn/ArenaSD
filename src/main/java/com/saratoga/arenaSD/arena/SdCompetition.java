package com.saratoga.arenaSD.arena;

import com.saratoga.arenaSD.ArenaSD;
import com.saratoga.arenaSD.SdConfig;
import com.saratoga.arenaSD.SdMessages;
import com.saratoga.arenaSD.bomb.ActiveBomb;
import com.saratoga.arenaSD.bomb.BombState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.battleplugins.arena.ArenaPlayer;
import org.battleplugins.arena.competition.CompetitionType;
import org.battleplugins.arena.competition.LiveCompetition;
import org.battleplugins.arena.competition.PlayerRole;
import org.battleplugins.arena.competition.phase.CompetitionPhaseType;
import org.battleplugins.arena.competition.phase.phases.VictoryPhase;
import org.battleplugins.arena.messages.Message;
import org.battleplugins.arena.team.ArenaTeam;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Main competition class for Search and Destroy.
 * Manages rounds, bomb state, team roles, and victory conditions.
 */
public class SdCompetition extends LiveCompetition<SdCompetition> {
    private final SdArena arena;
    private final SdMap map;
    private final SdConfig config;
    private final Random random = new Random();

    // Team state
    private ArenaTeam attackers;
    private ArenaTeam defenders;
    private boolean sidesSwapped = false;

    // Round state
    private int currentRound = 0;
    private int attackerWins = 0;
    private int defenderWins = 0;

    // Bomb state
    @Nullable
    private ActiveBomb activeBomb;

    // Timers
    @Nullable
    private BukkitTask tickTask;
    @Nullable
    private BukkitTask roundTimer;
    @Nullable
    private BukkitTask plantTimer;
    @Nullable
    private BukkitTask defuseTimer;
    @Nullable
    private BukkitTask detonationTimer;

    // Planting/defusing state
    @Nullable
    private ArenaPlayer plantingPlayer;
    @Nullable
    private ArenaPlayer defusingPlayer;
    private long plantStartTime;
    private long defuseStartTime;

    public SdCompetition(SdArena arena, CompetitionType type, SdMap map) {
        super(arena, type, map);
        this.arena = arena;
        this.map = map;
        this.config = ArenaSD.getInstance().getMainConfig();
    }

    // ==================== Lifecycle ====================

    public void onIngameStart() {
        this.assignTeams();
        this.startRound();
        this.startTicking();
    }

    public void onIngameComplete() {
        this.stopTicking();
        this.cancelAllTimers();
        this.cleanupBomb();
    }

    // ==================== Team Management ====================

    private void assignTeams() {
        List<ArenaTeam> teams = new ArrayList<>(this.getTeamManager().getTeams());
        if (teams.size() < 2) {
            this.arena.getPlugin().warn("S&D requires exactly 2 teams!");
            return;
        }

        // Random assignment for first half
        if (this.random.nextBoolean()) {
            this.attackers = teams.get(0);
            this.defenders = teams.get(1);
        } else {
            this.attackers = teams.get(1);
            this.defenders = teams.get(0);
        }

        // Notify teams
        for (ArenaPlayer player : this.getTeamManager().getPlayersOnTeam(this.attackers)) {
            SdMessages.YOU_ARE_ATTACKERS.send(player.getPlayer());
        }
        for (ArenaPlayer player : this.getTeamManager().getPlayersOnTeam(this.defenders)) {
            SdMessages.YOU_ARE_DEFENDERS.send(player.getPlayer());
        }
    }

    private void swapSides() {
        ArenaTeam temp = this.attackers;
        this.attackers = this.defenders;
        this.defenders = temp;
        this.sidesSwapped = !this.sidesSwapped;

        // Notify teams
        this.broadcast(SdMessages.SIDES_SWAPPED);
        for (ArenaPlayer player : this.getTeamManager().getPlayersOnTeam(this.attackers)) {
            SdMessages.YOU_ARE_ATTACKERS.send(player.getPlayer());
        }
        for (ArenaPlayer player : this.getTeamManager().getPlayersOnTeam(this.defenders)) {
            SdMessages.YOU_ARE_DEFENDERS.send(player.getPlayer());
        }
    }

    public boolean isAttacker(ArenaPlayer player) {
        return this.attackers != null && this.attackers.equals(player.getTeam());
    }

    public boolean isDefender(ArenaPlayer player) {
        return this.defenders != null && this.defenders.equals(player.getTeam());
    }

    // ==================== Round Management ====================

    private void startRound() {
        this.currentRound++;
        this.broadcast(SdMessages.ROUND_START, Component.text(this.currentRound));

        // Check for side swap
        if (this.currentRound > 1 && (this.currentRound - 1) % this.config.getSwapSidesAfter() == 0) {
            this.swapSides();
        }

        // Reset players to spawn
        this.respawnAllPlayers();

        // Spawn bomb
        this.spawnBomb();

        // Start round timer
        this.startRoundTimer();
    }

    private void endRound(boolean attackersWin) {
        this.cancelAllTimers();
        this.cleanupBomb();

        if (attackersWin) {
            this.attackerWins++;
            this.broadcast(SdMessages.ROUND_WIN_ATTACKERS);
        } else {
            this.defenderWins++;
            this.broadcast(SdMessages.ROUND_WIN_DEFENDERS);
        }

        // Show score
        this.broadcast(SdMessages.SCORE_UPDATE,
                Component.text(this.attackerWins),
                Component.text(this.defenderWins));

        // Check match end
        if (this.attackerWins >= this.config.getRoundsToWin()) {
            this.endMatchWithVictor(this.attackers);
            return;
        }
        if (this.defenderWins >= this.config.getRoundsToWin()) {
            this.endMatchWithVictor(this.defenders);
            return;
        }

        // Start next round after delay
        Bukkit.getScheduler().runTaskLater(ArenaSD.getInstance(), this::startRound, 100L); // 5 seconds
    }

    private void endMatchWithVictor(ArenaTeam victors) {
        Set<ArenaPlayer> victorPlayers = this.getTeamManager().getPlayersOnTeam(victors);
        // Advance to victory phase
        this.getPhaseManager().setPhase(CompetitionPhaseType.VICTORY);
        // Trigger victory event for the winning team
        if (this.getPhaseManager().getCurrentPhase() instanceof VictoryPhase<?> victoryPhase) {
            if (victorPlayers.isEmpty()) {
                victoryPhase.onDraw();
            } else {
                victoryPhase.onVictory(victorPlayers);
            }
        }
    }

    private void respawnAllPlayers() {
        for (ArenaPlayer player : this.getPlayers()) {
            // Reset to playing role
            if (player.getRole() == PlayerRole.SPECTATING) {
                this.changeRole(player, PlayerRole.PLAYING);
            }
            player.getPlayer().setGameMode(GameMode.ADVENTURE);
        }
    }

    // ==================== Bomb Management ====================

    public void spawnBomb() {
        if (this.map.getBombSpawn() == null) {
            this.arena.getPlugin().warn("No bomb spawn location set for map {}!", this.map.getName());
            return;
        }

        Location spawnLoc = this.map.getBombSpawn().toLocation(this.map.getWorld());

        if (this.config.isDropMode()) {
            // Drop bomb at spawn location
            this.activeBomb = new ActiveBomb(spawnLoc);
            this.map.getWorld().dropItem(spawnLoc, createBombItem());
            this.broadcast(SdMessages.BOMB_SPAWNED);
        } else {
            // Give to random attacker
            Set<ArenaPlayer> attackerPlayers = this.getTeamManager().getPlayersOnTeam(this.attackers);
            if (!attackerPlayers.isEmpty()) {
                ArenaPlayer carrier = attackerPlayers.stream()
                        .skip(this.random.nextInt(attackerPlayers.size()))
                        .findFirst()
                        .orElse(null);
                if (carrier != null) {
                    this.activeBomb = new ActiveBomb(carrier.getPlayer().getLocation());
                    this.giveBombToPlayer(carrier);
                }
            }
        }
    }

    private ItemStack createBombItem() {
        Material bombMaterial = Material.TNT;
        try {
            bombMaterial = Material.valueOf(this.config.getBombItem().toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }

        ItemStack bomb = new ItemStack(bombMaterial);
        bomb.editMeta(meta -> {
            meta.displayName(Component.text("BOMB", NamedTextColor.RED));
            meta.lore(List.of(
                    Component.text("Plant at Site A or B", NamedTextColor.YELLOW)));
        });
        return bomb;
    }

    public void giveBombToPlayer(ArenaPlayer player) {
        player.getPlayer().getInventory().addItem(createBombItem());
        player.getPlayer().getInventory().setHelmet(new ItemStack(Material.TNT));

        if (this.activeBomb != null) {
            this.activeBomb.setCarrier(player);
        }

        // Notify
        SdMessages.YOU_HAVE_BOMB.send(player.getPlayer());
        for (ArenaPlayer teammate : this.getTeamManager().getPlayersOnTeam(this.attackers)) {
            if (!teammate.equals(player)) {
                SdMessages.PROTECT_CARRIER.send(teammate.getPlayer(), Component.text(player.getPlayer().getName()));
            }
        }

        // Give glow effect visible only to teammates
        this.applyTeammateGlow(player);
    }

    private void applyTeammateGlow(ArenaPlayer carrier) {
        // Apply glowing effect
        carrier.getPlayer().addPotionEffect(new PotionEffect(
                PotionEffectType.GLOWING,
                Integer.MAX_VALUE,
                0,
                false,
                false,
                true));
    }

    public void onBombPickup(ArenaPlayer player) {
        if (this.activeBomb == null || !this.activeBomb.canBePickedUp()) {
            return;
        }

        if (!this.isAttacker(player)) {
            // Defenders cannot pick up the bomb
            return;
        }

        this.activeBomb.pickup(player);
        this.giveBombToPlayer(player);
        this.broadcast(SdMessages.BOMB_PICKED_UP, Component.text(player.getPlayer().getName()));
    }

    public void dropBomb(ArenaPlayer player) {
        if (this.activeBomb == null || this.activeBomb.getCarrier() != player) {
            return;
        }

        Location dropLoc = player.getPlayer().getLocation();
        this.activeBomb.drop(dropLoc);

        // Remove from player inventory
        player.getPlayer().getInventory().remove(Material.TNT);
        player.getPlayer().getInventory().setHelmet(null);
        player.getPlayer().removePotionEffect(PotionEffectType.GLOWING);

        // Drop item in world
        this.map.getWorld().dropItem(dropLoc, createBombItem());
        this.broadcast(SdMessages.BOMB_DROPPED);
    }

    private void cleanupBomb() {
        this.activeBomb = null;
        this.plantingPlayer = null;
        this.defusingPlayer = null;

        // Remove bomb items from all players
        for (ArenaPlayer player : this.getPlayers()) {
            player.getPlayer().getInventory().remove(Material.TNT);
            player.getPlayer().getInventory().setHelmet(null);
            player.getPlayer().removePotionEffect(PotionEffectType.GLOWING);
        }
    }

    // ==================== Plant/Defuse ====================

    public void startPlanting(ArenaPlayer player, String siteName) {
        if (this.activeBomb == null || !this.activeBomb.isCarried()) {
            return;
        }
        if (this.activeBomb.getCarrier() != player) {
            return;
        }

        SdMap.BombSite site = this.map.getBombSite(siteName);
        if (site == null) {
            return;
        }

        if (!site.isInside(player.getPlayer().getLocation(), this.map.getWorld())) {
            player.getPlayer()
                    .sendMessage(Component.text("You must be at site " + siteName + " to plant!", NamedTextColor.RED));
            return;
        }

        this.plantingPlayer = player;
        this.plantStartTime = System.currentTimeMillis();
        this.activeBomb.setState(BombState.PLANTING);
        this.activeBomb.setPlantedSite(siteName);

        SdMessages.PLANTING_BOMB.send(player.getPlayer());

        // Start plant timer
        long plantTicks = this.config.getPlantTime().toMillis() / 50;
        this.plantTimer = Bukkit.getScheduler().runTaskLater(ArenaSD.getInstance(), () -> {
            this.completePlanting();
        }, plantTicks);
    }

    public void cancelPlanting() {
        if (this.plantTimer != null) {
            this.plantTimer.cancel();
            this.plantTimer = null;
        }
        if (this.activeBomb != null && this.activeBomb.getState() == BombState.PLANTING) {
            this.activeBomb.setState(BombState.CARRIED);
            this.activeBomb.setPlantedSite(null);
        }
        if (this.plantingPlayer != null) {
            SdMessages.PLANT_CANCELLED.send(this.plantingPlayer.getPlayer());
            this.plantingPlayer = null;
        }
    }

    private void completePlanting() {
        if (this.activeBomb == null || this.plantingPlayer == null) {
            return;
        }

        String siteName = this.activeBomb.getPlantedSite();
        Location plantLoc = this.plantingPlayer.getPlayer().getLocation();

        this.activeBomb.setState(BombState.PLANTED);
        this.activeBomb.setLocation(plantLoc);
        this.activeBomb.setCarrier(null);

        // Remove bomb from carrier
        this.plantingPlayer.getPlayer().getInventory().remove(Material.TNT);
        this.plantingPlayer.getPlayer().getInventory().setHelmet(null);
        this.plantingPlayer.getPlayer().removePotionEffect(PotionEffectType.GLOWING);

        // Place TNT block
        plantLoc.getBlock().setType(Material.TNT);

        long detonationSeconds = this.config.getDetonationTime().getSeconds();
        this.broadcast(SdMessages.BOMB_PLANTED,
                Component.text(siteName),
                Component.text(detonationSeconds));

        // Play sound
        for (ArenaPlayer ap : this.getPlayers()) {
            ap.getPlayer().playSound(ap.getPlayer().getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 1.0f);
        }

        this.plantingPlayer = null;
        this.startDetonationTimer();
    }

    private void startDetonationTimer() {
        long detonationTicks = this.config.getDetonationTime().toMillis() / 50;
        this.detonationTimer = Bukkit.getScheduler().runTaskLater(ArenaSD.getInstance(), () -> {
            this.detonateBomb();
        }, detonationTicks);
    }

    private void detonateBomb() {
        if (this.activeBomb == null || !this.activeBomb.isPlanted()) {
            return;
        }

        this.activeBomb.setState(BombState.DETONATED);
        Location loc = this.activeBomb.getLocation();

        // Create explosion effect
        loc.getWorld().createExplosion(loc, 4.0f, false, false);
        loc.getBlock().setType(Material.AIR);

        this.broadcast(SdMessages.BOMB_DETONATED);

        // Attackers win!
        this.endRound(true);
    }

    public void startDefusing(ArenaPlayer player) {
        if (this.activeBomb == null || !this.activeBomb.isPlanted()) {
            return;
        }
        if (!this.isDefender(player)) {
            return;
        }

        Location bombLoc = this.activeBomb.getLocation();
        if (player.getPlayer().getLocation().distanceSquared(bombLoc) > 9) { // Within 3 blocks
            player.getPlayer()
                    .sendMessage(Component.text("You must be closer to the bomb to defuse!", NamedTextColor.RED));
            return;
        }

        this.defusingPlayer = player;
        this.defuseStartTime = System.currentTimeMillis();
        this.activeBomb.setState(BombState.DEFUSING);

        SdMessages.DEFUSING_BOMB.send(player.getPlayer());

        // Start defuse timer
        long defuseTicks = this.config.getDefuseTime().toMillis() / 50;
        this.defuseTimer = Bukkit.getScheduler().runTaskLater(ArenaSD.getInstance(), () -> {
            this.completeDefusing();
        }, defuseTicks);
    }

    public void cancelDefusing() {
        if (this.defuseTimer != null) {
            this.defuseTimer.cancel();
            this.defuseTimer = null;
        }
        if (this.activeBomb != null && this.activeBomb.getState() == BombState.DEFUSING) {
            this.activeBomb.setState(BombState.PLANTED);
        }
        if (this.defusingPlayer != null) {
            SdMessages.DEFUSE_CANCELLED.send(this.defusingPlayer.getPlayer());
            this.defusingPlayer = null;
        }
    }

    private void completeDefusing() {
        if (this.activeBomb == null || this.defusingPlayer == null) {
            return;
        }

        this.activeBomb.setState(BombState.DEFUSED);

        // Remove TNT block
        Location loc = this.activeBomb.getLocation();
        if (loc != null) {
            loc.getBlock().setType(Material.AIR);
        }

        this.broadcast(SdMessages.BOMB_DEFUSED, Component.text(this.defusingPlayer.getPlayer().getName()));

        // Play victory sound
        for (ArenaPlayer ap : this.getPlayers()) {
            ap.getPlayer().playSound(ap.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        // Cancel detonation
        if (this.detonationTimer != null) {
            this.detonationTimer.cancel();
            this.detonationTimer = null;
        }

        this.defusingPlayer = null;

        // Defenders win
        this.endRound(false);
    }

    // ==================== Player Events ====================

    public void onPlayerDeath(ArenaPlayer player) {
        // Drop bomb if carrier
        if (this.activeBomb != null && this.activeBomb.getCarrier() == player) {
            this.broadcast(SdMessages.BOMB_CARRIER_DIED, Component.text(player.getPlayer().getName()));
            this.dropBomb(player);
        }

        // Cancel plant/defuse if in progress
        if (player.equals(this.plantingPlayer)) {
            this.cancelPlanting();
        }
        if (player.equals(this.defusingPlayer)) {
            this.cancelDefusing();
        }

        // Change to spectator
        this.changeRole(player, PlayerRole.SPECTATING);
        player.getPlayer().setGameMode(GameMode.SPECTATOR);

        // Find a teammate to spectate
        this.makeSpectateTeammate(player);

        // Check if team eliminated
        this.checkTeamElimination();
    }

    public void onPlayerLeave(ArenaPlayer player) {
        // Drop bomb if carrier
        if (this.activeBomb != null && this.activeBomb.getCarrier() == player) {
            this.dropBomb(player);
        }

        // Cancel plant/defuse
        if (player.equals(this.plantingPlayer)) {
            this.cancelPlanting();
        }
        if (player.equals(this.defusingPlayer)) {
            this.cancelDefusing();
        }

        // Check team elimination
        this.checkTeamElimination();
    }

    private void makeSpectateTeammate(ArenaPlayer spectator) {
        ArenaTeam team = spectator.getTeam();
        if (team == null)
            return;

        Set<ArenaPlayer> teammates = this.getTeamManager().getPlayersOnTeam(team);
        for (ArenaPlayer teammate : teammates) {
            if (teammate.getRole() == PlayerRole.PLAYING && !teammate.getPlayer().isDead()) {
                spectator.getPlayer().setSpectatorTarget(teammate.getPlayer());
                SdMessages.NOW_SPECTATING.send(spectator.getPlayer(), Component.text(teammate.getPlayer().getName()));
                return;
            }
        }
    }

    private void checkTeamElimination() {
        Set<ArenaPlayer> aliveAttackers = this.getAlivePlayers(this.attackers);
        Set<ArenaPlayer> aliveDefenders = this.getAlivePlayers(this.defenders);

        if (aliveAttackers.isEmpty()) {
            // Defenders win by elimination (unless bomb is planted)
            if (this.activeBomb == null || !this.activeBomb.isPlanted()) {
                this.endRound(false);
            }
            // If bomb is planted, let timer continue
        } else if (aliveDefenders.isEmpty()) {
            // Attackers win by elimination
            this.endRound(true);
        }
    }

    private Set<ArenaPlayer> getAlivePlayers(ArenaTeam team) {
        Set<ArenaPlayer> alive = new HashSet<>();
        if (team == null)
            return alive;
        for (ArenaPlayer player : this.getTeamManager().getPlayersOnTeam(team)) {
            if (player.getRole() == PlayerRole.PLAYING && !player.getPlayer().isDead()) {
                alive.add(player);
            }
        }
        return alive;
    }

    // ==================== Timers ====================

    private void startRoundTimer() {
        long roundTicks = this.config.getRoundTime().toMillis() / 50;
        this.roundTimer = Bukkit.getScheduler().runTaskLater(ArenaSD.getInstance(), () -> {
            // Time expired - defenders win
            if (this.activeBomb == null || !this.activeBomb.isPlanted()) {
                this.broadcast(SdMessages.TIME_EXPIRED);
                this.endRound(false);
            }
            // If bomb planted, let detonation timer handle it
        }, roundTicks);
    }

    private void startTicking() {
        this.tickTask = Bukkit.getScheduler().runTaskTimer(ArenaSD.getInstance(), this::tick, 0, 1);
    }

    private void stopTicking() {
        if (this.tickTask != null) {
            this.tickTask.cancel();
            this.tickTask = null;
        }
    }

    private void tick() {
        // Update plant progress bar
        if (this.plantingPlayer != null && this.activeBomb != null
                && this.activeBomb.getState() == BombState.PLANTING) {
            Component progressBar = getProgressBar(this.plantStartTime, this.config.getPlantTime());
            this.plantingPlayer.getPlayer().sendActionBar(progressBar);
        }

        // Update defuse progress bar
        if (this.defusingPlayer != null && this.activeBomb != null
                && this.activeBomb.getState() == BombState.DEFUSING) {
            Component progressBar = getProgressBar(this.defuseStartTime, this.config.getDefuseTime());
            this.defusingPlayer.getPlayer().sendActionBar(progressBar);
        }

        // Particle effects for planted bomb
        if (this.activeBomb != null && this.activeBomb.isPlanted()) {
            Location loc = this.activeBomb.getLocation();
            if (loc != null) {
                loc.getWorld().spawnParticle(Particle.FLAME, loc.toCenterLocation(), 3, 0.3, 0.3, 0.3, 0.01);
            }
        }
    }

    private Component getProgressBar(long startTime, Duration totalDuration) {
        long elapsed = System.currentTimeMillis() - startTime;
        long totalMs = totalDuration.toMillis();
        double progress = Math.min(1.0, (double) elapsed / totalMs);

        int filled = (int) (progress * 20);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            bar.append(i < filled ? "█" : "░");
        }

        return Component.text(bar.toString(), progress < 1.0 ? NamedTextColor.YELLOW : NamedTextColor.GREEN);
    }

    private void cancelAllTimers() {
        if (this.roundTimer != null) {
            this.roundTimer.cancel();
            this.roundTimer = null;
        }
        if (this.plantTimer != null) {
            this.plantTimer.cancel();
            this.plantTimer = null;
        }
        if (this.defuseTimer != null) {
            this.defuseTimer.cancel();
            this.defuseTimer = null;
        }
        if (this.detonationTimer != null) {
            this.detonationTimer.cancel();
            this.detonationTimer = null;
        }
    }

    // ==================== Utilities ====================

    private void broadcast(Message message, Component... replacements) {
        for (ArenaPlayer player : this.getPlayers()) {
            message.send(player.getPlayer(), replacements);
        }
        for (ArenaPlayer player : this.getSpectators()) {
            message.send(player.getPlayer(), replacements);
        }
    }

    @Nullable
    public ActiveBomb getActiveBomb() {
        return this.activeBomb;
    }

    public SdMap getSdMap() {
        return this.map;
    }

    public ArenaTeam getAttackers() {
        return this.attackers;
    }

    public ArenaTeam getDefenders() {
        return this.defenders;
    }

    public int getCurrentRound() {
        return this.currentRound;
    }

    public int getAttackerWins() {
        return this.attackerWins;
    }

    public int getDefenderWins() {
        return this.defenderWins;
    }
}

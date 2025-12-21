package com.saratoga.arenaSD;

import com.saratoga.arenaSD.action.RemoveBombAction;
import com.saratoga.arenaSD.action.SpawnBombAction;
import com.saratoga.arenaSD.arena.SdArena;
import com.saratoga.arenaSD.listener.BombListener;
import org.battleplugins.arena.BattleArena;
import org.battleplugins.arena.config.ArenaConfigParser;
import org.battleplugins.arena.config.ParseException;
import org.battleplugins.arena.event.action.EventActionType;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Main plugin class for ArenaSD (Search and Destroy game mode).
 */
public final class ArenaSD extends JavaPlugin {

    public static final EventActionType<SpawnBombAction> SPAWN_BOMB_ACTION = EventActionType.create("spawn-bomb",
            SpawnBombAction.class, SpawnBombAction::new);
    public static final EventActionType<RemoveBombAction> REMOVE_BOMB_ACTION = EventActionType.create("remove-bomb",
            RemoveBombAction.class, RemoveBombAction::new);

    private static ArenaSD instance;
    private SdConfig config;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        this.saveDefaultConfig();

        // Load configuration
        File configFile = new File(this.getDataFolder(), "config.yml");
        Configuration config = YamlConfiguration.loadConfiguration(configFile);
        try {
            this.config = ArenaConfigParser.newInstance(configFile.toPath(), SdConfig.class, config);
        } catch (ParseException e) {
            ParseException.handle(e);

            this.getSLF4JLogger().error("Failed to load ArenaSD configuration! Disabling plugin.");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize messages
        SdMessages.init();

        // Save default arena config
        Path dataFolder = this.getDataFolder().toPath();
        Path arenasPath = dataFolder.resolve("arenas");
        if (Files.notExists(arenasPath)) {
            this.saveResource("arenas/snd.yml", false);
        }

        // Register arena with BattleArena
        BattleArena.getInstance().registerArena(this, "SND", SdArena.class, SdArena::new);

        // Register event listener
        this.getServer().getPluginManager().registerEvents(new BombListener(), this);

        // Fix: Explicitly set SdExecutor after arena is loaded
        // ArenaLoader.load() has a bug where it uses getCommand() which may not return
        // the injected command
        // causing the executor to not be set properly
        this.getServer().getScheduler().runTaskLater(this, () -> {
            org.battleplugins.arena.Arena arena = BattleArena.getInstance().getArena("SND");
            if (arena == null) {
                this.getSLF4JLogger().error("Failed to get SND arena - commands will not work!");
                return;
            }

            org.bukkit.command.Command sndCommand = org.bukkit.Bukkit.getCommandMap().getCommand("snd");
            if (sndCommand instanceof org.bukkit.command.PluginCommand pluginCmd) {
                SdExecutor executor = new SdExecutor(arena);
                pluginCmd.setExecutor(executor);
                pluginCmd.setTabCompleter(executor);
                this.getSLF4JLogger().info("Successfully registered SdExecutor for /snd command");
            } else {
                this.getSLF4JLogger().error("Failed to find /snd command in command map!");
            }
        }, 40L); // 2 seconds delay to ensure arena is fully loaded

        this.getSLF4JLogger().info("ArenaSD (Search and Destroy) has been enabled!");
    }

    @Override
    public void onDisable() {
        this.getSLF4JLogger().info("ArenaSD has been disabled.");
    }

    public SdConfig getMainConfig() {
        return this.config;
    }

    public static ArenaSD getInstance() {
        return instance;
    }
}

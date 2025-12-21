package com.saratoga.snd;

import com.saratoga.snd.arena.ArenaManager;
import com.saratoga.snd.command.SndCommand;
import com.saratoga.snd.listener.BombListener;
import com.saratoga.snd.listener.PlayerListener;
import com.saratoga.snd.listener.ProtectionListener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for Search and Destroy.
 */
public final class SearchAndDestroy extends JavaPlugin {

    private static SearchAndDestroy instance;
    private Config config;
    private ArenaManager arenaManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Load configuration
        this.config = new Config(this);

        // Initialize arena manager
        this.arenaManager = new ArenaManager(this);
        this.arenaManager.loadMaps();

        // Register commands
        SndCommand commandExecutor = new SndCommand(this);
        getCommand("snd").setExecutor(commandExecutor);
        getCommand("snd").setTabCompleter(commandExecutor);

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new BombListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);

        getSLF4JLogger().info("Search and Destroy has been enabled!");
    }

    @Override
    public void onDisable() {
        // End all active games
        if (arenaManager != null) {
            arenaManager.shutdown();
        }

        getSLF4JLogger().info("Search and Destroy has been disabled.");
    }

    public Config getMainConfig() {
        return config;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public static SearchAndDestroy getInstance() {
        return instance;
    }
}

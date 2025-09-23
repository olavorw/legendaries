package org.olavorw.legendaries;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.olavorw.legendaries.commands.IronDaggerCommand;

public final class Legendaries extends JavaPlugin {

    private IronDaggerManager ironDaggerManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.ironDaggerManager = new IronDaggerManager(this);
        // Register listeners
        Bukkit.getPluginManager().registerEvents(new IronDaggerListener(this, ironDaggerManager), this);

        // Commands
        IronDaggerCommand cmd = new IronDaggerCommand(this, ironDaggerManager);
        if (getCommand("irondagger") != null) {
            getCommand("irondagger").setExecutor(cmd);
            getCommand("irondagger").setTabCompleter(cmd);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

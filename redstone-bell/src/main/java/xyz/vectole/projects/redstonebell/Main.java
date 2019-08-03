package xyz.vectole.projects.redstonebell;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private static Main instance;
    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(new OnInteractBell(), this);
        getLogger().info("Redstone Bell plugin enabled!");
    }
    @Override
    public void onDisable() {
        getLogger().info("Redstone Bell plugin disabled!");
    }

    public static Main getInstance() {
        return instance;
    }
}
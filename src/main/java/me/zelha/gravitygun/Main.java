package me.zelha.gravitygun;

import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        getCommand("gravitygun").setExecutor(new GravityGunCommand());
    }
}

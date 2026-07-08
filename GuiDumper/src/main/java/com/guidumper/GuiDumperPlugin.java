package com.guidumper;

import org.bukkit.plugin.java.JavaPlugin;

public final class GuiDumperPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        getCommand("guidump").setExecutor(new GuiDumpCommand(this));
    }
}

package ca.masonx.backpack;

import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;

public class ConfigHelper {
    protected static ConfigSet assertConfig(JavaPlugin parent) {
        try {
            if (!parent.getDataFolder().exists()) {
            	parent.getDataFolder().mkdirs();
            }
            File file = new File(parent.getDataFolder(), "config.yml");
            if (!file.exists()) {
            	// Save default config
            	parent.saveDefaultConfig();
            }
            return loadConfig();
        } catch (Exception e) {
        	return null;
        }
    }
    
    private static ConfigSet loadConfig() {
    	return new ConfigSet();
    }
    
    protected static class ConfigSet {
    	
    }
}

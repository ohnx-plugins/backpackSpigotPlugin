package ca.masonx.backpack;

import java.io.File;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
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
            return loadConfig(parent);
        } catch (Exception e) {
        	return null;
        }
    }
    
    private static ConfigSet loadConfig(JavaPlugin parent) {
    	FileConfiguration config = parent.getConfig();
    	ConfigSet cfg = new ConfigSet();
    	
    	cfg.backpackName = config.getString("backpack-name");
    	
    	cfg.requireChestsInInv = config.getBoolean("require-chests-in-inv");
    	cfg.noChestMsg = ChatColor.translateAlternateColorCodes('&', config.getString("no-chest-msg"));
    	
    	cfg.noPermsMsg = ChatColor.translateAlternateColorCodes('&', config.getString("no-perms-msg"));
    	cfg.genericPermsErr = ChatColor.translateAlternateColorCodes('&', config.getString("generic-perms-err"));
    	
    	cfg.enableChestBackpack = config.getBoolean("enable-chest-backpack");
    	cfg.enableSignBackpack = config.getBoolean("enable-sign-backpack");
    	
    	return cfg;
    }
    
    protected static class ConfigSet {
    	String backpackName;
    	
    	boolean requireChestsInInv;
    	String noChestMsg;
    	
    	String noPermsMsg;
    	String genericPermsErr;
    	
    	boolean enableChestBackpack;
    	boolean enableSignBackpack;
    }
}

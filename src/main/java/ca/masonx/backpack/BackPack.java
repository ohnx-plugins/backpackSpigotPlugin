package ca.masonx.backpack;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import ca.masonx.backpack.ConfigHelper.ConfigSet;

import com.evilmidget38.UUIDFetcher;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class BackPack extends JavaPlugin implements Listener {
	protected Map<String, Inventory> puEnabled = new HashMap<String, Inventory>();
	protected BukkitRunnable saveTask;
	private final BackPack moi = this;
	private ConfigSet config;
	
	@Override
    public void onEnable() {
		new File(getDataFolder().toString()+"/backpacks").mkdirs();
    	this.getCommand("bp").setExecutor(this);
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
        
        config = ConfigHelper.assertConfig(this);
        
        saveTask = new BukkitRunnable () {
        	public void run() {
        		for (Entry<String, Inventory> en : puEnabled.entrySet()) {
        			String name = moi.getDataFolder().toString()+"/backpacks/"+en.getKey()+".inv";
            		try {
            			InventoryIO.write(name, InventoryToBase64.toBase64(en.getValue()));
            		} catch (Exception e) {
            			moi.getLogger().severe("Failed to save a player's backpack to \""+name+"\"!");
            			e.printStackTrace();
            		}
        		}
        	}
        };
        saveTask.runTaskTimer(moi, 6000, 6000);
    }
 
    @Override
    public void onDisable() {
    	for (Entry<String, Inventory> en : puEnabled.entrySet()) {
			String name = moi.getDataFolder().toString()+"/backpacks/"+en.getKey()+".inv";
    		try {
    			InventoryIO.write(name, InventoryToBase64.toBase64(en.getValue()));
    		} catch (Exception e) {
    			moi.getLogger().severe("Failed to save a player's backpack to \""+name+"\"!");
    			try {
    				moi.getLogger().severe("This can serve as proof of what the player had in their backpack: " + InventoryToBase64.toBase64(en.getValue()));
    			} catch (Exception f) {
    				moi.getLogger().severe("Player has corrupted backpack!");
    			}
    			e.printStackTrace();
    		}
		}
    }
    
    public void openInv(Player p, Inventory inv){
    	p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 1, 0);
        p.openInventory(inv);
    }
    
	@Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    	try {
    		if(cmd.getName().equalsIgnoreCase("bp")){
    			if(!(sender instanceof Player)) {
    				sender.sendMessage(ChatColor.RED+"You must be a player to do this!");
    				return true;
    			}
    			Player p = (Player) sender;
				if(!p.hasPermission("backpack.self.open")) {
					sender.sendMessage(ChatColor.RED+"You don't have the permission to open a backpack!");
					return true;
				}
    			Inventory i;
				if(args.length==0) {	//player was not specified, just use default one
					String pUUID = p.getUniqueId().toString();
					boolean shouldNotOpen = true;
					i = null;
					if(puEnabled.containsKey(pUUID)) {
						try {
							i = puEnabled.get(pUUID);
							InventoryIO.write(getDataFolder().toString()+"/backpacks/"+pUUID+".inv", InventoryToBase64.toBase64(i));
							shouldNotOpen = false;
						} catch (Exception e) {
							sender.sendMessage(ChatColor.RED+"Had difficulties saving your preexisting backpack!");
							getLogger().severe("Error saving player backpack to \""+getDataFolder().toString()+"/backpacks/"+pUUID+".inv"+"\"!");
							e.printStackTrace();
						}
					} else {
						try {
							if(!InventoryIO.nouveau(getDataFolder().toString()+"/backpacks/"+pUUID+".inv"))
								i = InventoryToBase64.fromBase64(InventoryIO.read(getDataFolder().toString()+"/backpacks/"+pUUID+".inv"), "Backpack");
							else
								i = (Inventory) Bukkit.getServer().createInventory(null, 54, "Backpack");
							shouldNotOpen = true;
						} catch (Exception e) {
							sender.sendMessage(ChatColor.RED+"Error opening your backpack!");
							moi.getLogger().severe("Failed to open a player's backpack from \""+getDataFolder().toString()+"/backpacks/"+pUUID+".inv"+"\"!");
	            			e.printStackTrace();
						}
					}
					if (!shouldNotOpen) openInv(p, i);
					return true;
				}
				if((args.length==1)) {
					if (args[0].equalsIgnoreCase("autofill")) {
						if (!p.hasPermission("backpack.pickup")) {
							sender.sendMessage(ChatColor.RED+"Whoops! You don't have the permission to do that.");
						}
						String pUUID = p.getUniqueId().toString();
						if (puEnabled.containsKey(pUUID)) {
							try {
								InventoryIO.write(getDataFolder().toString()+"/backpacks/"+pUUID+".inv", InventoryToBase64.toBase64(puEnabled.get(pUUID)));
								puEnabled.remove(pUUID);
								sender.sendMessage(ChatColor.DARK_RED+"No longer automatically picking up items.");
							} catch (Exception e) {
								sender.sendMessage(ChatColor.RED+"Had difficulties saving your preexisting backpack!");
								getLogger().severe("Error saving player backpack to \""+getDataFolder().toString()+"/backpacks/"+pUUID+".inv"+"\"!");
								e.printStackTrace();
							}
						} else {
							try {
								if(!InventoryIO.nouveau(getDataFolder().toString()+"/backpacks/"+pUUID+".inv"))
									i = InventoryToBase64.fromBase64(InventoryIO.read(getDataFolder().toString()+"/backpacks/"+pUUID+".inv"), "Backpack");
								else
									i = (Inventory) Bukkit.getServer().createInventory(null, 54, "Backpack");
								puEnabled.put(pUUID, i);
								sender.sendMessage(ChatColor.GREEN+"Now automatically picking up items!");
							} catch (Exception e) {
								sender.sendMessage(ChatColor.RED+"Error opening your backpack!");
								moi.getLogger().severe("Failed to open a player's backpack from \""+getDataFolder().toString()+"/backpacks/"+pUUID+".inv"+"\"!");
								sender.sendMessage(ChatColor.RED+"Error opening your backpack!");
							}
						}
						return true;
					}
					final String pName = args[0];
					final CommandSender cs = sender;
					if(!p.hasPermission("backpack.others.open")) {
						sender.sendMessage(ChatColor.RED+"You can't open another player's backpack!");
	    				return true;
					}
					Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
						public void run() {
							try {
								UUIDFetcher fetcher = new UUIDFetcher(Arrays.asList(pName));
								Map<String, UUID> response = null;
								response = fetcher.call();
				    			Player p = (Player) cs;
				    			Inventory i;
								if(!InventoryIO.nouveau(getDataFolder().toString()+"/backpacks/"+response.get(pName).toString()+".inv"))
									i = InventoryToBase64.fromBase64(InventoryIO.read(getDataFolder().toString()+"/backpacks/"+response.get(pName).toString()+".inv"), pName+"'s backpack");
								else
									i = (Inventory) Bukkit.getServer().createInventory(null, 54,  pName+"'s backpack");
								openInv(p, i);
							} catch (Exception e) {
								cs.sendMessage(ChatColor.RED+"Error fetching UUID!");
							}
						}
					});
					return true;
				}
    		}
   	    } catch (Exception e) {
   	    	getLogger().warning("Something went wrong while parsing command from player "+sender.getName()+":");
   	    	e.printStackTrace();
   	    	sender.sendMessage(ChatColor.RED+"Uhh, something went wrong.");
   	    }
    	return true; 
    }
	@EventHandler
	public void onInvInteract(InventoryInteractEvent e) {
		Player p = (Player) e.getWhoClicked();
		if((e.getInventory().getHolder() instanceof Chest)||(e.getInventory().getHolder() instanceof DoubleChest))
			return;
		if (e.getInventory().getName().contains("'s backpack")) {
			if(!p.hasPermission("backpack.edit")) {
				e.setCancelled(true);
			}
		}
	}
	@EventHandler
	public void closeInventory(InventoryCloseEvent e) {
		final Inventory i = e.getInventory();
		final Player p = (Player) e.getPlayer();
		if((e.getInventory().getHolder() instanceof Chest)||(e.getInventory().getHolder() instanceof DoubleChest))
			return;
		if(p.hasPermission("backpack.edit")) {
			if (i.getName().contains("'s backpack")) {
				final String pName = i.getName().split("'")[0];
				Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
					public void run() {
						try {
							UUIDFetcher fetcher = new UUIDFetcher(Arrays.asList(pName));
							Map<String, UUID> response = null;
							response = fetcher.call();
			    			try {
			    				InventoryIO.write(getDataFolder().toString()+"/backpacks/"+response.get(pName)+".inv", InventoryToBase64.toBase64(i));
			    				p.playSound(p.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1, 0);
			    				i.clear();
			    			} catch (Exception e) {
			    	    		p.sendMessage("Uh oh... The inventory couldn't be saved. Placing items on ground.");
			    	    		for(ItemStack is : i.getContents()){
			    	    			p.getWorld().dropItem(p.getLocation(), is);
			    	    		}
			    			}
						} catch (Exception e) {
							p.sendMessage("Error fetching UUID!");
						}
					}
				});
				
			}
		}
		if(!i.getName().equals("Backpack"))
			return;
		//Is backpack!
		if(i.getSize()!=54)
			return;
		//that isn't the correct inventory size...
    	try {
    		InventoryIO.write(getDataFolder().toString()+"/backpacks/"+p.getUniqueId().toString()+".inv", InventoryToBase64.toBase64(i));
    		i.clear();
    		p.playSound(p.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1, 0);
    	} catch (Exception f) {
    		p.sendMessage("Uh oh... The inventory couldn't be saved. Placing items on ground.");
    		for(ItemStack is : i.getContents()){
    			p.getWorld().dropItem(p.getLocation(), is);
    		}
    	}
	}
	@EventHandler
    public void onInteract(PlayerInteractEvent e){
        Player p = e.getPlayer();
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK){
            if (e.getClickedBlock().getState() instanceof Sign) {
                Sign s = (Sign) e.getClickedBlock().getState();
                if(s.getLine(0).contains("[Backpack]")){
    				if(!p.hasPermission("backpack.open.self")&&!s.getLine(1).contains("Everyone")) {
    					p.sendMessage("You don't have the permission to open a backpack!");
    					return;
    				}
        			Inventory i;
        			try {
        				if(!InventoryIO.nouveau(getDataFolder().toString()+"/backpacks/"+p.getUniqueId().toString()+".inv"))
        					i = InventoryToBase64.fromBase64(InventoryIO.read(getDataFolder().toString()+"/backpacks/"+p.getUniqueId().toString()+".inv"), "Backpack");
        				else
        					i = (Inventory) Bukkit.getServer().createInventory(null, 54, "Backpack");
        				openInv(p, i);
        			} catch (Exception erreur) {
        				p.sendMessage("There was an error opening your backpack!");
        			}
        			return;
                }
            }
        }
    }
	@EventHandler
	public void onSignChange(SignChangeEvent e) {
        Player p = e.getPlayer();
        if(e.getLine(0).equals("[Backpack]")){
        	if(p.hasPermission("backpack.sign")){
        		if(e.getLine(1).equals("Everyone")) {
        			e.setLine(1, ChatColor.GREEN+"Everyone");
        		}
        		p.sendMessage("New backpack sign created!");
        		e.setLine(0, ChatColor.DARK_BLUE+"[Backpack]");
        	} else {
           		p.sendMessage("You don't have the permission to do that!");
           		e.setCancelled(true);
        	}
        }
	}
}
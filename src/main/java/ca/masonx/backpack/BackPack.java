package ca.masonx.backpack;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import ca.masonx.backpack.ConfigHelper.ConfigSet;

import com.evilmidget38.UUIDFetcher;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class BackPack extends JavaPlugin implements Listener {
	private final BackPack moi = this;
	private ConfigSet config;
	
	@Override
    public void onEnable() {
		new File(getDataFolder().toString()+"/backpacks").mkdirs();
    	this.getCommand("bp").setExecutor(this);
    	this.getCommand("bpa").setExecutor(this);
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
        
        config = ConfigHelper.assertConfig(this);
    }
 
    @Override
    public void onDisable() {
    }
    
    public void openInv(Player p, Inventory inv){
    	p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 1, 0);
        p.openInventory(inv);
    }
    
    protected boolean playerHasBackpack(Player p) {
    	for (ItemStack is : p.getInventory().all(Material.CHEST).values()) {
    		if (is.hasItemMeta() && is.getItemMeta().hasDisplayName() && is.getItemMeta().getDisplayName().equalsIgnoreCase(config.backpackName)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    protected void openBackpack(Player p, String pUUID) {
		boolean shouldNotOpen = true;
		Inventory i = null;
		if (pUUID == null) pUUID = p.getUniqueId().toString();
		try {
			if(!InventoryIO.nouveau(getDataFolder().toString()+"/backpacks/"+pUUID+".inv"))
				i = InventoryToBase64.fromBase64(InventoryIO.read(getDataFolder().toString()+"/backpacks/"+pUUID+".inv"), "Backpack");
			else
				i = (Inventory) Bukkit.getServer().createInventory(null, 54, "Backpack");
			shouldNotOpen = false;
		} catch (Exception e) {
			p.sendMessage(ChatColor.RED+"Error opening your backpack!");
			moi.getLogger().severe("Failed to open a player's backpack from \""+getDataFolder().toString()+"/backpacks/"+pUUID+".inv"+"\"!");
			e.printStackTrace();
		}
		if (!shouldNotOpen) openInv(p, i);
    }
    
    protected void saveBackpack(String uuid, Inventory i, Player errMsgReceiver) {
    	try {
    		InventoryIO.write(getDataFolder().toString()+"/backpacks/"+uuid+".inv", InventoryToBase64.toBase64(i));
    		i.clear();
    		errMsgReceiver.playSound(errMsgReceiver.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1, 0);
    	} catch (Exception f) {
    		errMsgReceiver.sendMessage("Uh oh... The inventory couldn't be saved. Placing items on ground.");
    		for(ItemStack is : i.getContents()){
    			errMsgReceiver.getWorld().dropItem(errMsgReceiver.getLocation(), is);
    		}
    	}
    	
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
				if(!p.hasPermission("backpack.use")) {
					sender.sendMessage(config.noPermsMsg);
					return true;
				}
				if(config.requireChestsInInv && !p.hasPermission("backpack.use.no-chest") && !playerHasBackpack(p)) {
					sender.sendMessage(config.noChestMsg);
					return true;
				}
				if(args.length==0) {	//player was not specified, just use default one
					openBackpack(p, null);
					return true;
				}
				if((args.length==1)) {
					final String pName = args[0];
					final Player pIn = (Player) sender;
					if(!p.hasPermission("backpack.admin")) {
						sender.sendMessage(config.genericPermsErr);
	    				return true;
					}
					Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
						public void run() {
							try {
								UUIDFetcher fetcher = new UUIDFetcher(Arrays.asList(pName));
								Map<String, UUID> response = null;
								response = fetcher.call();
				    			openBackpack(pIn, response.get(pName).toString());
							} catch (Exception e) {
								pIn.sendMessage(ChatColor.RED+"Error fetching UUID!");
							}
						}
					});
					return true;
				}
    		} else if(cmd.getName().equalsIgnoreCase("bpa")) {
    			if(!sender.hasPermission("backpack.admin.clear")) {
					sender.sendMessage(config.genericPermsErr);
    				return true;
				}
    			if(args.length == 2 && args[0].equalsIgnoreCase("clear")) {
    				final String pName = args[1];
    				final CommandSender cs = sender;
    				Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
						public void run() {
							try {
								UUIDFetcher fetcher = new UUIDFetcher(Arrays.asList(pName));
								Map<String, UUID> response = null;
								response = fetcher.call();
								File old = new File(getDataFolder().toString()+"/backpacks/"+response.get(pName).toString()+".inv");
								File bck = new File(getDataFolder().toString()+"/backpacks/"+response.get(pName).toString()+".inv.bck");
								if(old.exists()) {
									if (bck.exists()) bck.delete();
									old.renameTo(bck);
									cs.sendMessage(ChatColor.GREEN + "Player "+pName+"'s backpack deleted!");
									getLogger().info("A player backpack was just deleted. To restore, rename the file:");
									getLogger().info(getDataFolder().toString()+"/backpacks/"+response.get(pName).toString()+".inv.bck");
									getLogger().info("to the name:");
									getLogger().info(getDataFolder().toString()+"/backpacks/"+response.get(pName).toString()+".inv");
									getLogger().info("(ie, remove the `.bak` from the file extension`");
								} else {
									cs.sendMessage(ChatColor.YELLOW + "That player didn't have a backpack!");
								}
							} catch (Exception e) {
								cs.sendMessage(ChatColor.RED+"Error fetching UUID!");
							}
						}
					});
    				return true;
    			} else if(args.length == 1 && args[0].equalsIgnoreCase("reloadconfig")) {
    				if(!sender.hasPermission("backpack.admin")) {
    					sender.sendMessage(config.genericPermsErr);
        				return true;
    				}
    				config = ConfigHelper.assertConfig(this);
    				sender.sendMessage(ChatColor.GREEN + "Config reloaded!");
    			} else {
    				sender.sendMessage(ChatColor.RED + "Unrecognized command.");
    			}
    			return true;
    		}
   	    } catch (Exception e) {
   	    	getLogger().warning("Something went wrong while parsing command from player "+sender.getName()+":");
   	    	e.printStackTrace();
   	    	sender.sendMessage(ChatColor.RED+"Oops! something went wrong.");
   	    }
    	return true; 
    }
	
	@EventHandler
	public void onInvClick(InventoryClickEvent e) {
		if((e.getInventory().getHolder() instanceof Chest)||(e.getInventory().getHolder() instanceof DoubleChest)) return;
		
		// prevent a player from putting their backpack into the backpack
		if(e.getInventory().getName().contains(config.backpackName)) {
			ItemStack is = e.getCurrentItem();
			if (is == null) return;
			if(is.hasItemMeta() && is.getItemMeta().hasDisplayName() && is.getItemMeta().getDisplayName().equalsIgnoreCase(config.backpackName)) {
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler
    public void onInteract(PlayerInteractEvent e){
        Player p = e.getPlayer();
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK){
            if (e.getClickedBlock().getState() instanceof Sign && config.enableSignBackpack) {
                Sign s = (Sign) e.getClickedBlock().getState();
                if(s.getLine(0).contains("[Backpack]")){
    				if(!p.hasPermission("backpack.use")&&!s.getLine(1).contains("Everyone")) {
    					p.sendMessage(config.noPermsMsg);
    					return;
    				}
    				openBackpack(p, null);
        			return;
                }
            } else if (e.getClickedBlock().getState() instanceof Chest && config.enableChestBackpack && p.hasPermission("backpack.use")) {
            	Chest c = (Chest) e.getClickedBlock().getState();
            	if (c.getBlockInventory().getName().equalsIgnoreCase("Backpack")) {
            		e.setCancelled(true);
            		openBackpack(p, null);
            	}
            }
        }
    }
	
	@EventHandler
	public void onSignChange(SignChangeEvent e) {
        Player p = e.getPlayer();
        if(e.getLine(0).equals("[Backpack]")) {
        	if(p.hasPermission("backpack.admin.sign")) {
        		if(e.getLine(1).equalsIgnoreCase("Everyone")) {
        			e.setLine(1, ChatColor.GREEN+"Everyone");
        		}
        		if(config.enableSignBackpack) {
        			p.sendMessage("New backpack sign created!");
            		e.setLine(0, ChatColor.DARK_BLUE+"[Backpack]");
        		} else {
        			p.sendMessage("Backpack signs are disabled.");
        			e.setCancelled(true);
        		}
        	} else {
           		p.sendMessage(config.genericPermsErr);
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
		
		if(!i.getName().equals(config.backpackName))
			return;
		
		if(i.getSize()!=54)
			return;
		
		if(p.hasPermission("backpack.admin")) {
			if (i.getName().contains("'s backpack")) {
				final String pName = i.getName().split("'")[0];
				Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
					public void run() {
						try {
							UUIDFetcher fetcher = new UUIDFetcher(Arrays.asList(pName));
							Map<String, UUID> response = null;
							response = fetcher.call();
							saveBackpack(response.get(pName).toString(), i, p);
						} catch (Exception e) {
							p.sendMessage("Error fetching UUID!");
						}
					}
				});
				return;
			}
		}
		
		saveBackpack(p.getUniqueId().toString(), i, p);
	}
}
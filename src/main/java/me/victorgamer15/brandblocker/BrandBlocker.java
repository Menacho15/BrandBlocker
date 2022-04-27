package me.victorgamer15.brandblocker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

public final class BrandBlocker extends JavaPlugin implements PluginMessageListener , Listener {

    @Override
    public void onEnable() {
        String[] div = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
        String version = div[1];
        getLogger().info("Server running version 1."+version);
        if (Integer.parseInt(version) < 13) {
            Messenger messenger = Bukkit.getMessenger();
            messenger.registerIncomingPluginChannel(this, "MC|Brand", this);
            getLogger().info("Registered 1.12- listener");
        } else {
            Messenger messenger = Bukkit.getMessenger();
            messenger.registerIncomingPluginChannel(this, "minecraft:brand", this);
            getLogger().info("Registered 1.13+ listener");
        }
        FileConfiguration config = getConfig();
        config.options().header("BrandBlocker by VictorGamer15\n\nTypes of mode:\nBlacklist: Kick players who are using this brand\nWhitelist: Kick players who are not using this brand\n\nIf you need some help, chat with me on the 'Discussion' tab of the plugin");
        config.addDefault("enable", false);
        config.addDefault("mode", "blacklist");
        ArrayList<String> brands = new ArrayList<String>();
        brands.add("lunarclient");
        brands.add("badlion");
        config.addDefault("blocked-brands", brands);
        config.addDefault("kick-message", "&cThe client that you're using, is not permitted on our server.\n&cPlease use another client.");
        config.addDefault("geyser-support", false);
        config.addDefault("geyser-prefix", "*");
        config.options().copyDefaults(true);
        saveConfig();
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        player_brands.remove(e.getPlayer().getName());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("brandblocker")) {
            if (args.length == 0) {
                sender.sendMessage("§4§m--------------------------");
                sender.sendMessage("§c§lBrandBlocker §7v"+this.getDescription().getVersion());
                sender.sendMessage("§7by VictorGamer15");
                sender.sendMessage("§7");
                sender.sendMessage("§cUsage §4»");
                sender.sendMessage("§c§l● check §7(player)");
                sender.sendMessage("§c§l● reload");
                sender.sendMessage("§4§m--------------------------");
            } else {
                if (args[0].equalsIgnoreCase("check")) {
                    if (sender.hasPermission("brandblocker.usage")) {
                        if (!(args.length > 1)) {
                            sender.sendMessage("§c§lBrandBlocker §c» §7You need to specify a §eplayer name §7to use this command.");
                        } else {
                            if (player_brands.containsKey(args[1])) {
                                sender.sendMessage("§c§lBrandBlocker §c» §e"+args[1]+"§7 entered to the server with the client brand §e"+player_brands.get(args[1]));
                            } else {
                                sender.sendMessage("§c§lBrandBlocker §c» §7The player §e"+args[1]+" §7didn't send a client brand packet, or it isn't connected.");
                            }
                        }
                    } else {
                        sender.sendMessage("§c§lBrandBlocker §c» §7You need the permission §ebrandblocker.usage §7to use this command.");
                    }
                } else if (args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("brandblocker.usage")) {
                        reloadConfig();
                        sender.sendMessage("§c§lBrandBlocker §c» §7The main configuration file was §ereloaded§7.");
                    } else {
                        sender.sendMessage("§c§lBrandBlocker §c» §7You need the permission §ebrandblocker.usage §7to use this command.");
                    }
                }
            }
            return false;
        }
        return false;
    }

    HashMap<String, String> player_brands = new HashMap<String, String>();

    @Override
    public void onPluginMessageReceived(String channel, Player p, byte[] msg) {
        String brand = new String(msg, StandardCharsets.UTF_8).substring(1);
        player_brands.put(p.getName(), brand);
        if (!getConfig().getBoolean("enable")) return;
        if (getConfig().getBoolean("geyser-support") && p.getName().contains(Objects.requireNonNull(getConfig().getString("geyser-prefix")))) return;
        if (getConfig().getString("mode", "blacklist").equals("blacklist")) {
            Iterator<String> iterator = getConfig().getStringList("blocked-brands").iterator();
            while (iterator.hasNext()) {
                String str = iterator.next();
                if (brand.toLowerCase().contains(str.toLowerCase())) {
                    String kickMsg = getConfig().getString("kick-message");
                    assert kickMsg != null;
                    p.kickPlayer(ChatColor.translateAlternateColorCodes('&', kickMsg));
                    getLogger().info(p.getName() + " was kicked for using " + brand);
                    return;
                }
            }
        } else if (getConfig().getString("mode", "whitelist").equals("whitelist")) {
            Iterator<String> iterator = getConfig().getStringList("blocked-brands").iterator();
            while (iterator.hasNext()) {
                String str = iterator.next();
                if (brand.toLowerCase().contains(str.toLowerCase()))
                    return;
            }
            String kickMsg = getConfig().getString("kick-message");
            assert kickMsg != null;
            p.kickPlayer(ChatColor.translateAlternateColorCodes('&', kickMsg));
            getLogger().info(p.getName() + " was kicked for using " + brand);
            return;
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        player_brands.clear();
    }

}

package me.victorgamer15.brandblocker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

public final class BrandBlocker extends JavaPlugin implements PluginMessageListener , Listener {

    String prefix;

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
        prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix"));
        saveDefaultConfig();
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
                            sender.sendMessage(prefix+ChatColor.translateAlternateColorCodes('&', getConfig().getString("specify-player-name")));
                        } else {
                            if (player_brands.containsKey(args[1])) {
                                sender.sendMessage(prefix+ChatColor.translateAlternateColorCodes('&', getConfig().getString("check-succesful")).replace("%player%", args[1]).replace("%brand%", player_brands.get(args[1])));
                            } else {
                                sender.sendMessage(prefix+ChatColor.translateAlternateColorCodes('&', getConfig().getString("check-failed")).replace("%player%", args[1]));
                            }
                        }
                    } else {
                        sender.sendMessage(prefix+ChatColor.translateAlternateColorCodes('&', getConfig().getString("no-permission")));
                    }
                } else if (args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("brandblocker.usage")) {
                        reloadConfig();
                        sender.sendMessage(prefix+ChatColor.translateAlternateColorCodes('&', getConfig().getString("config-reload")));
                    } else {
                        sender.sendMessage(prefix+ChatColor.translateAlternateColorCodes('&', getConfig().getString("no-permission")));
                    }
                }
            }
            return false;
        }
        return false;
    }

    HashMap<String, String> player_brands = new HashMap<>();

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
                    if(p.hasPermission("brandblocker.bypass")) return;
                    String kickMsg = getConfig().getString("kick-message");
                    assert kickMsg != null;
                    p.kickPlayer(ChatColor.translateAlternateColorCodes('&', kickMsg));
                    getLogger().info(getConfig().getString("console-log").replace("%player%", p.getName()).replace("%brand%", brand));
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
            if(p.hasPermission("brandblocker.bypass")) return;
            String kickMsg = getConfig().getString("kick-message");
            assert kickMsg != null;
            p.kickPlayer(ChatColor.translateAlternateColorCodes('&', kickMsg));
            getLogger().info(getConfig().getString("console-log").replace("%player%", p.getName()).replace("%brand%", brand));
            return;
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        player_brands.clear();
    }

}

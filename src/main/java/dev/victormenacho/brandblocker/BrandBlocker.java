package dev.victormenacho.brandblocker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

public class BrandBlocker extends JavaPlugin implements PluginMessageListener, Listener {

    public String prefix;
    public final String version = Bukkit.getBukkitVersion().split("-")[0].split("\\.")[1];
    public HashMap<String, String> player_brands = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix"));
        getLogger().info("Server running version 1."+version);

        if (Integer.parseInt(version) < 13) {
            Bukkit.getMessenger().registerIncomingPluginChannel(this, "MC|Brand", this);
            getLogger().info("Registered 1.12- listener");
        } else {
            Bukkit.getMessenger().registerIncomingPluginChannel(this, "minecraft:brand", this);
            getLogger().info("Registered 1.13+ listener");
        }

        Bukkit.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        player_brands.remove(e.getPlayer().getName());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        final Player p = e.getPlayer();

        if (!getConfig().getBoolean("enable")) return;
        if (getConfig().getBoolean("geyser-support") && p.getName().contains(Objects.requireNonNull(getConfig().getString("geyser-prefix")))) return;
        if (!player_brands.containsKey(p.getName())) return;

        final String brand = player_brands.get(p.getName());
        final Iterator<String> iterator = getConfig().getStringList("blocked-brands").iterator();

        switch (getConfig().getString("mode")) {
            case "blacklist":
                while (iterator.hasNext()) {
                    String str = iterator.next();
                    if (brand.contains(str)) {
                        if(p.hasPermission("brandblocker.bypass")) return;
                        String kickCmd = getConfig().getString("kick-command");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), kickCmd.replace("%player%", p.getName()).replace("%brand%", brand));
                        getLogger().info(getConfig().getString("console-log").replace("%player%", p.getName()).replace("%brand%", brand));
                        return;
                    }
                }
                break;
            case "whitelist":
                while (iterator.hasNext()) {
                    String str = iterator.next();
                    if (brand.contains(str))
                        return;
                }
                if(p.hasPermission("brandblocker.bypass")) return;
                String kickCmd = getConfig().getString("kick-command");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), kickCmd.replace("%player%", p.getName()).replace("%brand%", brand));
                getLogger().info(getConfig().getString("console-log").replace("%player%", p.getName()).replace("%brand%", brand));
                break;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("brandblocker")) {
            if (args.length == 0) {
                sender.sendMessage("§4§m--------------------------");
                sender.sendMessage("§c§lBrandBlocker §7v"+this.getDescription().getVersion());
                sender.sendMessage("§7by Menacho");
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
                        prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix"));
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

    @Override
    public void onPluginMessageReceived(String channel, Player p, byte[] msg) {
        final String brand = new String(msg, StandardCharsets.UTF_8).substring(1);
        player_brands.put(p.getName(), brand);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        player_brands.clear();
    }

}

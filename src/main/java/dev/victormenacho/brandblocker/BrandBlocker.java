package dev.victormenacho.brandblocker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Objects;

public class BrandBlocker extends JavaPlugin implements Listener {

    public String prefix;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("prefix"));
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        final Player p = e.getPlayer();

        if (!getConfig().getBoolean("enable")) return;
        if (getConfig().getBoolean("geyser-support") && p.getName().contains(Objects.requireNonNull(getConfig().getString("geyser-prefix")))) return;

        final String brand = getClientBrand(p);
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
                            final Player specifiedPlayer = Bukkit.getPlayer(args[1]);

                            if (specifiedPlayer == null) {
                                sender.sendMessage(prefix+ChatColor.translateAlternateColorCodes('&', getConfig().getString("check-failed")).replace("%player%", args[1]));
                            } else {
                                final String brand = getClientBrand(specifiedPlayer);
                                if (brand.equalsIgnoreCase("unknown")) {
                                    sender.sendMessage(prefix+ChatColor.translateAlternateColorCodes('&', getConfig().getString("check-failed")).replace("%player%", args[1]));
                                } else {
                                    sender.sendMessage(prefix+ChatColor.translateAlternateColorCodes('&', getConfig().getString("check-succesful")).replace("%player%", args[1]).replace("%brand%", brand));
                                }
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

    private String getClientBrand(Player player) {
        try {
            final String[] possibleFieldNames = new String[]{"clientBrand", "clientBrandName", "brand"};
            try {
                String brand = (String) player.getClass().getMethod("getClientBrandName").invoke((Object)player, new Object[0]);
                if (brand != null && !brand.isEmpty()) {
                    return brand;
                }
            }
            catch (Exception ignored) {}

            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            for (String fieldName : possibleFieldNames) {
                try {
                    Field brandField = handle.getClass().getDeclaredField(fieldName);
                    brandField.setAccessible(true);
                    Object brandValue = brandField.get(handle);
                    if (!(brandValue instanceof String) || ((String)brandValue).isEmpty()) continue;
                    return (String)brandValue;
                }
                catch (NoSuchFieldException ignored) {}
            }

            try {
                Field connectionField = handle.getClass().getDeclaredField("connection");
                connectionField.setAccessible(true);
                Object connection = connectionField.get(handle);

                Field brandField = connection.getClass().getDeclaredField("clientBrand");
                brandField.setAccessible(true);
                Object brandValue = brandField.get(connection);

                if (brandValue instanceof String && !((String) brandValue).isEmpty()) {
                    return (String) brandValue;
                }
            }
            catch (Exception ignored) {}

            try {
                final String[] methods = new String[]{"getClientBrand", "getClientBrandName"};
                String version = this.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
                Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
                Object craftPlayer = craftPlayerClass.cast(player);
                Object entityPlayer = craftPlayerClass.getMethod("getHandle").invoke(craftPlayer);
                for (String methodName : methods) {
                    try {
                        Object brand = entityPlayer.getClass().getMethod(methodName).invoke(entityPlayer);
                        if (!(brand instanceof String) || ((String)brand).isEmpty()) continue;
                        return (String)brand;
                    }
                    catch (Exception ignored) {}
                }
                for (String fieldName : possibleFieldNames) {
                    try {
                        Field brandField = entityPlayer.getClass().getDeclaredField(fieldName);
                        brandField.setAccessible(true);
                        Object brandValue = brandField.get(entityPlayer);
                        if (!(brandValue instanceof String) || ((String)brandValue).isEmpty()) continue;
                        return (String)brandValue;
                    }
                    catch (Exception ignored) {}
                }
            }
            catch (Exception ex) {
                this.getLogger().warning("Failed to detect client brand for " + player.getName() + ": " + ex.getMessage());
            }
            return "vanilla";
        }
        catch (Exception e) {
            this.getLogger().warning("Error detecting client brand for " + player.getName() + ": " + e.getMessage());
            return "unknown";
        }
    }
}

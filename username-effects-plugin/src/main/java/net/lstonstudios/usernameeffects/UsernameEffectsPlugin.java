package net.lstonstudios.usernameeffects;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class UsernameEffectsPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, NameEffect> effects = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMove = new ConcurrentHashMap<>();
    private final Set<UUID> afk = ConcurrentHashMap.newKeySet();
    private static final String AFK_PREFIX = ChatColor.GRAY + "AFK" + ChatColor.RESET + " ";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadEffects();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("nameeffect").setExecutor(this);
        long limitMs = getConfig().getLong("afk-minutes", 5) * 60_000L;
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            long now = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                Long last = lastMove.putIfAbsent(player.getUniqueId(), now);
                if (last != null && now - last >= limitMs) {
                    goAfk(player);
                }
            }
        }, 200L, 200L);
    }

    private void loadEffects() {
        effects.clear();
        if (!getConfig().isConfigurationSection("players")) {
            return;
        }
        for (String key : getConfig().getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String path = "players." + key + ".";
                effects.put(uuid, new NameEffect(
                        color(getConfig().getString(path + "prefix", "")),
                        color(getConfig().getString(path + "suffix", ""))));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(this, () -> applyEffect(player, effects.get(player.getUniqueId())));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onKick(PlayerKickEvent event) {
        if (event.getCause() != PlayerKickEvent.Cause.TIMEOUT) {
            return;
        }
        event.setCancelled(true);
        goAfk(event.getPlayer());
    }

    private void goAfk(Player player) {
        if (afk.add(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "You Have Gone AFK.");
            applyEffect(player, effects.get(player.getUniqueId()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!event.hasChangedPosition()) {
            return;
        }
        lastMove.put(player.getUniqueId(), System.currentTimeMillis());
        if (!afk.contains(player.getUniqueId())) {
            return;
        }
        afk.remove(player.getUniqueId());
        applyEffect(player, effects.get(player.getUniqueId()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        afk.remove(event.getPlayer().getUniqueId());
        lastMove.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        NameEffect effect = effects.get(event.getPlayer().getUniqueId());
        boolean isAfk = afk.contains(event.getPlayer().getUniqueId());
        if (effect == null && !isAfk) {
            return;
        }
        if (effect == null) {
            effect = new NameEffect("", "");
        }
        String prefix = (isAfk ? AFK_PREFIX : "") + (effect.prefix.isEmpty() ? "" : effect.prefix + " ");
        String suffix = effect.suffix.isEmpty() ? "" : " " + effect.suffix;
        event.setFormat(escapePercent(prefix) + "%1$s" + escapePercent(suffix) + ChatColor.RESET + ": %2$s");
    }

    private String escapePercent(String value) {
        return value.replace("%", "%%");
    }

    private boolean applyEffect(Player player, NameEffect effect) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "ue" + player.getUniqueId().toString().replace("-", "").substring(0, 14);
        Team team = scoreboard.getTeam(teamName);
        boolean isAfk = afk.contains(player.getUniqueId());
        if (effect == null && !isAfk) {
            if (team != null) {
                team.removePlayer(player);
                team.unregister();
            }
            return true;
        }

        if (effect == null) {
            effect = new NameEffect("", "");
        }
        String prefix = effect.prefix.isEmpty() ? "" : effect.prefix + " ";
        String suffix = effect.suffix.isEmpty() ? "" : " " + effect.suffix;
        if (prefix.length() > 16 || suffix.length() > 16) {
            return false;
        }
        prefix = (isAfk ? AFK_PREFIX : "") + prefix;
        if (prefix.length() > 64) {
            return false;
        }
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        Team currentTeam = scoreboard.getEntryTeam(player.getName());
        if (currentTeam != null && !currentTeam.getName().equals(teamName)) {
            return false;
        }
        team.setPrefix(prefix);
        team.setSuffix(suffix);
        team.addPlayer(player);
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean admin = sender.hasPermission("usernameeffects.admin");
        if (args.length == 0) {
            sendUsage(sender, label, admin);
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!admin) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
                return true;
            }
            reloadConfig();
            loadEffects();
            for (Player player : Bukkit.getOnlinePlayers()) {
                applyEffect(player, effects.get(player.getUniqueId()));
            }
            sender.sendMessage(ChatColor.GREEN + "Username effects reloaded.");
            return true;
        }
        boolean clear = args[0].equalsIgnoreCase("clear");
        boolean set = args[0].equalsIgnoreCase("set");
        if (!clear && !set) {
            sendUsage(sender, label, admin);
            return true;
        }
        Player target;
        int styleStart;
        if (admin) {
            if (args.length < 2) {
                sendUsage(sender, label, true);
                return true;
            }
            target = Bukkit.getPlayerExact(args[1]);
            styleStart = 2;
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "That player must be online.");
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this.");
                return true;
            }
            target = (Player) sender;
            styleStart = 1;
        }
        if (clear) {
            effects.remove(target.getUniqueId());
            getConfig().set("players." + target.getUniqueId(), null);
            saveConfig();
            applyEffect(target, null);
            sender.sendMessage(ChatColor.GREEN + "Cleared the username effect for " + target.getName() + ".");
            return true;
        }
        if (args.length <= styleStart) {
            sendUsage(sender, label, admin);
            return true;
        }

        String style = String.join(" ", Arrays.copyOfRange(args, styleStart, args.length));
        int separator = style.indexOf('|');
        String prefix = color((separator < 0 ? style : style.substring(0, separator)).trim());
        String suffix = color(separator < 0 ? "" : style.substring(separator + 1).trim());
        NameEffect effect = new NameEffect(prefix, suffix);
        if ((prefix.isEmpty() ? 0 : prefix.length() + 1) > 16 || (suffix.isEmpty() ? 0 : suffix.length() + 1) > 16) {
            sender.sendMessage(ChatColor.RED + "Prefix and suffix must each fit the 16-character 1.12 scoreboard limit.");
            return true;
        }
        if (!applyEffect(target, effect)) {
            sender.sendMessage(ChatColor.RED + "Could not apply the effect; the player may already be in another scoreboard team.");
            return true;
        }

        effects.put(target.getUniqueId(), effect);
        String path = "players." + target.getUniqueId() + ".";
        getConfig().set(path + "name", target.getName());
        getConfig().set(path + "prefix", prefix);
        getConfig().set(path + "suffix", suffix);
        saveConfig();
        sender.sendMessage(ChatColor.GREEN + "Applied the username effect to " + target.getName() + ".");
        return true;
    }

    private void sendUsage(CommandSender sender, String label, boolean admin) {
        if (admin) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " set <player> <prefix>|<suffix>");
            sender.sendMessage(ChatColor.YELLOW + "       /" + label + " clear <player>  or  /" + label + " reload");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " set <prefix>|<suffix>  (use & color codes)");
            sender.sendMessage(ChatColor.YELLOW + "       /" + label + " clear");
        }
    }

    private static final class NameEffect {
        private final String prefix;
        private final String suffix;

        private NameEffect(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }
    }
}

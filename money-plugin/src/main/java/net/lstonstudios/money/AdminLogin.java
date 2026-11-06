package net.lstonstudios.money;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.permissions.PermissionAttachment;

// Staff must sign in with /login before acting and are protected from targeted commands and damage.
final class AdminLogin implements Listener {
    private final MoneyPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Set<UUID> loggedIn = new HashSet<>();
    private final SecureRandom random = new SecureRandom();

    AdminLogin(MoneyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "admins.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    private static final String[] ADMIN_PERMISSIONS = {"money.moderate", "minecraft.command.ban",
            "minecraft.command.ban-ip", "minecraft.command.pardon", "minecraft.command.pardon-ip",
            "minecraft.command.banlist", "minecraft.command.kick", "minecraft.command.gamemode",
            "bukkit.command.ban.player", "bukkit.command.ban.ip", "bukkit.command.unban.player",
            "bukkit.command.unban.ip", "bukkit.command.banlist", "bukkit.command.kick",
            "minecraft.command.kill", "minecraft.command.tp", "minecraft.command.teleport", "bukkit.command.teleport",
            "minecraft.command.selector",
            "minecraft.command.give", "minecraft.command.xp", "minecraft.command.experience", "bukkit.command.give",
            "bukkit.command.xp", "money.admin", "customtnt.admin", "customability.admin",
            "customitems.admin", "customgun.admin", "spawnmobs.use", "usernameeffects.admin",
            "orbital.use", "orbital.give"};
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    // Admins can only moderate (ban, kick, mute, timeout, gamemode, tp). Ops can do everything.
    boolean isAdmin(UUID id) {
        return data.getStringList("admins").contains(id.toString());
    }

    boolean permHelp(CommandSender sender) {
        if (sender instanceof Player && !isStaff((Player) sender)) {
            sender.sendMessage(ChatColor.RED + "Only admins and ops can use /permhelp.");
            return true;
        }
        if (sender instanceof Player && needsLogin((Player) sender)) {
            prompt((Player) sender);
            return true;
        }
        boolean op = !(sender instanceof Player) || sender.isOp();
        sender.sendMessage(ChatColor.GOLD + "=== Your commands (" + (op ? "op" : "admin") + ") ===");
        sender.sendMessage(ChatColor.YELLOW + "Moderation: " + ChatColor.WHITE
                + "/ban /ban-ip /unban /pardon /pardon-ip /banlist /kick /gamemode");
        sender.sendMessage(ChatColor.WHITE + "  /mute|/unmute <player>, /timeout <player> <minutes|off>");
        sender.sendMessage(ChatColor.YELLOW + "Vanilla: " + ChatColor.WHITE + "/kill /tp /give /xp /summon (selectors like @a @s @p @r @e allowed)");
        sender.sendMessage(ChatColor.YELLOW + "Money: " + ChatColor.WHITE
                + "/pay <player|selector> <amount> (unlimited for admins), /bankrupt <player>");
        sender.sendMessage(ChatColor.GRAY + "  /pay accepts player selectors: @a @e @n @p @r @s (and selector filters).");
        sender.sendMessage(ChatColor.GRAY + "  Run mreload in the server console to reload the money config.");
        sender.sendMessage(ChatColor.WHITE + "  /cshop opens custom items; /cshop <1-5> opens the shop editor; /cshop <n> view|rename|section|setblock|delblock");
        sender.sendMessage(ChatColor.YELLOW + "Custom items: " + ChatColor.WHITE
                + "/customtnt, /customability, /customsword, /cgun (guns), /rldcustomtnt");
        sender.sendMessage(ChatColor.YELLOW + "Other: " + ChatColor.WHITE
                + "/spawn <entity>, /nameeffect set|clear <player> or reload, /orbital");
        sender.sendMessage(ChatColor.YELLOW + "Account: " + ChatColor.WHITE
                + "/login <password> [new-password], /permhelp");
        if (op) {
            sender.sendMessage(ChatColor.YELLOW + "Ops only: " + ChatColor.WHITE
                    + "/op /deop /admin /deadmin and every other server command.");
        } else {
            sender.sendMessage(ChatColor.GRAY + "Admins can't use /op, /deop, /admin, /deadmin or other commands.");
        }
        return true;
    }

    boolean isStaff(org.bukkit.OfflinePlayer player) {
        return player.isOp() || isAdmin(player.getUniqueId());
    }

    private void resetPlayer(Player player) {
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new org.bukkit.inventory.ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        player.updateInventory();
    }

    private void setPendingReset(UUID id, boolean pending) {
        List<String> list = new ArrayList<>(data.getStringList("pending-reset"));
        list.remove(id.toString());
        if (pending) {
            list.add(id.toString());
        }
        data.set("pending-reset", list);
        save();
    }

    private void applyPermissions(Player player) {
        PermissionAttachment old = attachments.remove(player.getUniqueId());
        if (old != null) {
            player.removeAttachment(old);
        }
        if (isAdmin(player.getUniqueId()) && !player.isOp()) {
            PermissionAttachment attachment = player.addAttachment(plugin);
            for (String permission : ADMIN_PERMISSIONS) {
                attachment.setPermission(permission, true);
            }
            attachments.put(player.getUniqueId(), attachment);
        }
        player.recalculatePermissions();
    }

    private void setAdmin(UUID id, boolean admin) {
        List<String> list = new ArrayList<>(data.getStringList("admins"));
        list.remove(id.toString());
        if (admin) {
            list.add(id.toString());
        }
        data.set("admins", list);
        save();
    }

    private boolean needsLogin(Player player) {
        return isStaff(player) && !loggedIn.contains(player.getUniqueId());
    }

    private boolean hasPassword(Player player) {
        return data.isString("passwords." + player.getUniqueId() + ".hash");
    }

    private void prompt(Player player) {
        if (hasPassword(player)) {
            player.sendMessage(ChatColor.YELLOW + "Use /login <password> to sign in (or /login <password> <new-password> to change it).");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Set a password using /login <password>.");
        }
    }

    private static String hash(String salt, String password) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((salt + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : digest) {
                out.append(String.format("%02x", b));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void save() {
        try {
            plugin.getDataFolder().mkdirs();
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save admins.yml: " + e.getMessage());
        }
    }

    private void setPassword(String path, String password) {
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        String salt = new java.math.BigInteger(1, bytes).toString(16);
        data.set(path + ".salt", salt);
        data.set(path + ".hash", hash(salt, password));
        save();
    }

    boolean login(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can sign in.");
            return true;
        }
        Player player = (Player) sender;
        if (!isStaff(player)) {
            player.sendMessage(ChatColor.RED + "Only admins can use /login.");
            return true;
        }
        if (args.length < 1 || args.length > 2 || (args.length == 2 && !hasPassword(player))) {
            prompt(player);
            return true;
        }
        String path = "passwords." + player.getUniqueId();
        if (args.length == 2) {
            if (!hash(data.getString(path + ".salt", ""), args[0]).equals(data.getString(path + ".hash"))) {
                player.sendMessage(ChatColor.RED + "Wrong password.");
            } else if (args[1].length() < 4) {
                player.sendMessage(ChatColor.RED + "Your new password must be at least 4 characters.");
            } else {
                setPassword(path, args[1]);
                loggedIn.add(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "Password changed. Use /login <new-password> from now on.");
            }
            return true;
        }
        if (!hasPassword(player)) {
            if (args[0].length() < 4) {
                player.sendMessage(ChatColor.RED + "Your password must be at least 4 characters.");
                return true;
            }
            setPassword(path, args[0]);
            loggedIn.add(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Password set. You are signed in. Use /login <password> next time.");
            return true;
        }
        if (hash(data.getString(path + ".salt", ""), args[0]).equals(data.getString(path + ".hash"))) {
            loggedIn.add(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "You are signed in.");
        } else {
            player.sendMessage(ChatColor.RED + "Wrong password.");
        }
        return true;
    }

    // /op and /deop (ops) or /admin and /deadmin (moderators). Only ops and the console may run them.
    boolean admin(CommandSender sender, Command command, String[] args, boolean grant, boolean op) {
        if (sender instanceof Player && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Only ops can do that.");
            return true;
        }
        if (sender instanceof Player && needsLogin((Player) sender)) {
            prompt((Player) sender);
            return true;
        }
        if (args.length != 1) {
            return false;
        }
        Player online = Bukkit.getPlayerExact(args[0]);
        org.bukkit.OfflinePlayer target = online != null ? online : Bukkit.getOfflinePlayer(args[0]);
        if (online == null && !target.hasPlayedBefore()) {
            sender.sendMessage(ChatColor.RED + "That player has never joined the server.");
            return true;
        }
        String name = target.getName() != null ? target.getName() : args[0];
        String role = op ? "an op" : "an admin";
        boolean has = op ? target.isOp() : isAdmin(target.getUniqueId());
        if (has == grant) {
            sender.sendMessage(ChatColor.YELLOW + name + (grant ? " is already " + role + "." : " is not " + role + "."));
            return true;
        }
        if (op) {
            target.setOp(grant);
            if (grant) {
                setAdmin(target.getUniqueId(), false);
            }
        } else {
            if (target.isOp()) {
                sender.sendMessage(ChatColor.RED + name + " is an op. Use /deop first.");
                return true;
            }
            setAdmin(target.getUniqueId(), grant);
        }
        if (!isStaff(target)) {
            loggedIn.remove(target.getUniqueId());
            if (!grant) {
                // Demoted players lose creative mode and any items they spawned in.
                if (online != null) {
                    resetPlayer(online);
                } else {
                    setPendingReset(target.getUniqueId(), true);
                }
            }
        }
        sender.sendMessage(ChatColor.GREEN + name + (grant ? " is now " + role + "." : " is no longer " + role + "."));
        if (online != null) {
            applyPermissions(online);
            if (grant) {
                online.sendMessage(ChatColor.GREEN + "You have became " + role + ". Type /permhelp for a list of"
                        + " commands for your perm!");
                prompt(online);
            } else {
                online.sendMessage(ChatColor.YELLOW + "You are no longer " + role + ".");
            }
        }
        return true;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        applyPermissions(player);
        if (!isStaff(player) && data.getStringList("pending-reset").contains(player.getUniqueId().toString())) {
            resetPlayer(player);
            setPendingReset(player.getUniqueId(), false);
        }
        if (!isStaff(player)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline() && needsLogin(player)) {
                    prompt(player);
                }
            }
        }, 40L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        loggedIn.remove(event.getPlayer().getUniqueId());
        attachments.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player sender = event.getPlayer();
        if (needsLogin(sender)) {
            String first = event.getMessage().split(" ")[0].toLowerCase();
            if (!first.equals("/login")) {
                event.setCancelled(true);
                prompt(sender);
                return;
            }
        }
        if (targetsStaff(sender, event.getMessage())) {
            event.setCancelled(true);
            sender.sendMessage(ChatColor.RED + "That admin or op is still logging in and cannot be targeted yet.");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerCommand(ServerCommandEvent event) {
        if (targetsStaff(event.getSender(), event.getCommand())) {
            event.setCancelled(true);
            event.getSender().sendMessage(ChatColor.RED + "That admin or op is still logging in and cannot be targeted yet.");
        }
    }

    private boolean targetsStaff(CommandSender sender, String input) {
        String[] parts = input.trim().replaceFirst("^/", "").split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return false;
        }
        String command = parts[0].toLowerCase(java.util.Locale.ROOT);
        int namespace = command.indexOf(':');
        if (namespace >= 0) {
            command = command.substring(namespace + 1);
        }
        if (HARMLESS_COMMANDS.contains(command)) {
            return false;
        }

        // Every selector anywhere in the line, including ones with spaces inside [...] filters
        java.util.regex.Matcher selectors = SELECTOR.matcher(input);
        while (selectors.find()) {
            if (selectsStaff(sender, selectors.group())) {
                return true;
            }
        }
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].startsWith("@") && selectsStaff(sender, parts[i])) {
                return true;
            }
        }
        return false;
    }

    private static final Pattern SELECTOR = Pattern.compile("@[a-zA-Z]+(?:\\[[^\\]]*\\])?");
    private static final Set<String> HARMLESS_COMMANDS = Set.of("login", "pay", "balance", "bal", "msg", "tell", "w",
            "whisper", "r", "reply", "me", "say", "help", "?", "list", "sell", "shop", "cshop", "kit", "stop");

    private boolean selectsStaff(CommandSender sender, String target) {
        if (target.startsWith("@")) {
            try {
                for (org.bukkit.entity.Entity entity : Bukkit.selectEntities(sender, target)) {
                    if (entity instanceof Player && needsLogin((Player) entity)
                            && !isSender(sender, (Player) entity)) {
                        return true;
                    }
                }
            } catch (IllegalArgumentException ignored) {
                return false;
            }
            return false;
        }
        String name = target.replaceAll("^[\"']+|[\"',]+$", "");
        if (name.isEmpty()) {
            return false;
        }
        // Names are case-insensitive in commands, so /kill steve must still hit Steve
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(name)) {
                return needsLogin(online) && !isSender(sender, online);
            }
        }
        if (name.length() == 36 && name.indexOf('-') == 8) {
            try {
                Player byId = Bukkit.getPlayer(UUID.fromString(name));
                return byId != null && needsLogin(byId) && !isSender(sender, byId);
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }
        return false;
    }

    private static boolean isSender(CommandSender sender, Player target) {
        return sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStaffDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && needsLogin((Player) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStaffTeleport(PlayerTeleportEvent event) {
        if (needsLogin(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStaffKick(PlayerKickEvent event) {
        if (needsLogin(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStaffPotionEffect(EntityPotionEffectEvent event) {
        if (event.getEntity() instanceof Player && needsLogin((Player) event.getEntity())
                && event.getNewEffect() != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStaffFoodChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player && needsLogin((Player) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (needsLogin(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.YELLOW + "Sign in with /login <password> before chatting.");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent event) {
        if (needsLogin(event.getPlayer()) && (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY() || event.getFrom().getZ() != event.getTo().getZ())) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent event) {
        if (needsLogin(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent event) {
        if (needsLogin(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (needsLogin(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}

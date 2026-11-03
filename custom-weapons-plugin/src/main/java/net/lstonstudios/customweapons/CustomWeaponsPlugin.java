package net.lstonstudios.customweapons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * The Wind Burst: a consumable that launches you skyward and cancels fall damage for 4 seconds.
 * (The custom Mace and Spear were removed; Minecraft now has native ones.)
 */
public final class CustomWeaponsPlugin extends JavaPlugin implements Listener, TabExecutor {
    // This marker is duplicated in the Money plugin's kit shop; keep them in sync.
    static final String WIND_MARKER = ChatColor.BLACK + "CustomWeapon:wind";
    static final Material WIND_MATERIAL = Material.PRISMARINE_CRYSTALS;

    private final Set<UUID> windAirborne = new HashSet<>();
    private final Map<UUID, Long> fallImmunity = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("weapon").setExecutor(this);
        getCommand("weapon").setTabCompleter(this);
    }

    // ----------------------------------------------------------------- items

    static ItemStack createWind() {
        ItemStack item = new ItemStack(WIND_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + "Wind Burst");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Right-click: launch yourself skyward,",
                ChatColor.GRAY + "then fall back down with the Mace.",
                ChatColor.GRAY + "Consumed on use. No fall damage for 4s.", WIND_MARKER));
        item.setItemMeta(meta);
        return item;
    }

    static boolean isWind(ItemStack item) {
        if (item == null || item.getType() != WIND_MATERIAL || !item.hasItemMeta()) {
            return false;
        }
        List<String> lore = item.getItemMeta().getLore();
        return lore != null && lore.contains(WIND_MARKER);
    }

    // ---------------------------------------------------------------- events

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        Action action = event.getAction();
        if (isWind(hand) && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true);
            windBurst(player, hand);
        }
    }

    private void windBurst(final Player player, ItemStack item) {
        final UUID id = player.getUniqueId();
        if (windAirborne.contains(id)) {
            return;
        }
        windAirborne.add(id);
        // No cooldown: the burst is available again as soon as the player touches the ground.
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || (++ticks > 5 && player.isOnGround()) || ticks > 600) {
                    windAirborne.remove(id);
                    cancel();
                }
            }
        }.runTaskTimer(this, 1L, 1L);
        player.setVelocity(new Vector(0, 1.36, 0));
        fallImmunity.put(player.getUniqueId(), System.currentTimeMillis() + 4000L);
        Location at = player.getLocation();
        at.getWorld().playSound(at, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.2f);
        at.getWorld().spawnParticle(Particle.CLOUD, at, 40, 0.4, 0.1, 0.4, 0.1);
        item.setAmount(item.getAmount() - 1);
        player.getInventory().setItemInMainHand(item.getAmount() > 0 ? item : null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL || !(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        Long until = fallImmunity.get(player.getUniqueId());
        if (until != null && until > System.currentTimeMillis()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        windAirborne.remove(event.getPlayer().getUniqueId());
        fallImmunity.remove(event.getPlayer().getUniqueId());
    }

    // --------------------------------------------------------------- command

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2 || args.length > 3) {
            return false;
        }
        String type = args[1].toLowerCase(Locale.ROOT);
        if (!type.equals("wind") && !type.equals("all")) {
            return false;
        }
        int amount = 1;
        if (args.length == 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Amount must be a whole number.");
                return true;
            }
            if (amount < 1 || amount > 64) {
                sender.sendMessage(ChatColor.RED + "Amount must be between 1 and 64.");
                return true;
            }
        }
        List<Player> targets = resolve(sender, args[0]);
        if (targets == null) {
            return true;
        }
        for (Player p : targets) {
            for (int i = 0; i < amount; i++) {
                give(p, createWind());
            }
        }
        sender.sendMessage(ChatColor.GREEN + "Gave " + amount + " " + type + " to " + targets.size() + " player(s).");
        return true;
    }

    private static void give(Player p, ItemStack item) {
        for (ItemStack rest : p.getInventory().addItem(item).values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), rest);
        }
    }

    private List<Player> resolve(CommandSender sender, String sel) {
        List<Player> out = new ArrayList<>();
        String lower = sel.toLowerCase(Locale.ROOT);
        if (lower.equals("@a")) {
            out.addAll(Bukkit.getOnlinePlayers());
        } else if (lower.equals("@s")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "@s needs a player sender.");
                return null;
            }
            out.add((Player) sender);
        } else if (lower.equals("@p")) {
            Location from = sender instanceof Player ? ((Player) sender).getLocation()
                    : sender instanceof BlockCommandSender ? ((BlockCommandSender) sender).getBlock().getLocation()
                    : null;
            if (from == null) {
                sender.sendMessage(ChatColor.RED + "@p needs a player or command block sender.");
                return null;
            }
            Player best = null;
            double bd = Double.MAX_VALUE;
            for (Player p : from.getWorld().getPlayers()) {
                double d = p.getLocation().distanceSquared(from);
                if (d < bd) {
                    bd = d;
                    best = p;
                }
            }
            if (best != null) {
                out.add(best);
            }
        } else {
            Player p = Bukkit.getPlayerExact(sel);
            if (p != null) {
                out.add(p);
            }
        }
        if (out.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No matching players found for " + sel + ".");
            return null;
        }
        return out;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("@a", "@s", "@p")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(p.getName());
                }
            }
        } else if (args.length == 2) {
            for (String s : Arrays.asList("wind", "all")) {
                if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
        }
        return out;
    }
}

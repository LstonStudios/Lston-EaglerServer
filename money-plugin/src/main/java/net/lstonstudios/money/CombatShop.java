package net.lstonstudios.money;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** /shop <1-6>: individual combat items. Left-click buys one stack as shown, shift-click buys 5 stacks. */
final class CombatShop implements Listener {
    static final int PAGES = 6;
    private static final int ITEM_AREA = 36; // rows 1-4; row 5 stays empty above the navigation row
    private static final int PREV = 45;
    private static final int INFO = 49;
    private static final int NEXT = 53;
    private static final String[] PAGE_NAMES = {"Armor & Weapons", "Tools & Arrows", "Healing & Foods",
            "Ores & Materials", "Potions", "Combat Blocks"};
    private static final Material[] PAGE_PANES = {Material.RED_STAINED_GLASS_PANE,
            Material.ORANGE_STAINED_GLASS_PANE, Material.PINK_STAINED_GLASS_PANE,
            Material.CYAN_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE};

    private final MoneyPlugin plugin;
    private final List<List<Product>> pages = new ArrayList<>();

    private static final class Product {
        final ItemStack stack;
        final long price;
        final String label;

        Product(ItemStack stack, long price, String label) {
            this.stack = stack;
            this.label = label;
            this.price = Math.min(MoneyPlugin.MAX_ITEM_VALUE, Math.max(1, price));
        }
    }

    private static final class Holder implements InventoryHolder {
        final int page;

        Holder(int page) {
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    CombatShop(MoneyPlugin plugin) {
        this.plugin = plugin;
        for (int i = 0; i < PAGES; i++) {
            pages.add(new ArrayList<Product>());
        }
        build();
    }

    // ---------------------------------------------------------------- catalog

    private void add(int page, String material, int amount, long price) {
        Material m = Material.matchMaterial(material);
        if (m == null || !m.isItem()) {
            plugin.getLogger().warning("Combat shop: unknown item " + material);
            return;
        }
        pages.get(page - 1).add(new Product(new ItemStack(m, amount), price, pretty(m)));
    }

    private void addPotion(int page, Material kind, String type, int amount, long price) {
        org.bukkit.potion.PotionType potion;
        try {
            potion = org.bukkit.potion.PotionType.valueOf(type);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Combat shop: unknown potion " + type);
            return;
        }
        ItemStack stack = new ItemStack(kind, amount);
        org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) stack.getItemMeta();
        meta.setBasePotionType(potion);
        stack.setItemMeta(meta);
        String prefix = kind == Material.SPLASH_POTION ? "Splash Potion of " : "Arrow of ";
        pages.get(page - 1).add(new Product(stack, price, prefix + pretty(type)));
    }

    private void build() {
        // Page 1: armor and weapons
        add(1, "DIAMOND_SWORD", 1, 3_500);
        add(1, "DIAMOND_SPEAR", 1, 3_500);
        add(1, "DIAMOND_AXE", 1, 3_000);
        add(1, "MACE", 1, 40_000);
        add(1, "TRIDENT", 1, 8_000);
        add(1, "BOW", 1, 300);
        add(1, "CROSSBOW", 1, 700);
        add(1, "DIAMOND_HELMET", 1, 2_500);
        add(1, "DIAMOND_CHESTPLATE", 1, 4_500);
        add(1, "DIAMOND_LEGGINGS", 1, 4_000);
        add(1, "DIAMOND_BOOTS", 1, 2_200);
        add(1, "TURTLE_HELMET", 1, 3_000);
        add(1, "ELYTRA", 1, 60_000);
        add(1, "SHIELD", 1, 500);
        add(1, "TOTEM_OF_UNDYING", 1, 15_000);
        add(1, "DIAMOND_HORSE_ARMOR", 1, 4_000);
        add(1, "DIAMOND_NAUTILUS_ARMOR", 1, 4_000);
        add(1, "WOLF_ARMOR", 1, 1_500);
        add(1, "CARVED_PUMPKIN", 1, 100);
        add(1, "TNT_MINECART", 1, 1_000);
        add(1, "END_CRYSTAL", 1, 2_500);
        add(1, "FIRE_CHARGE", 16, 700);
        add(1, "WIND_CHARGE", 16, 1_200);
        add(1, "SNOWBALL", 16, 50);
        add(1, "EGG", 16, 50);
        add(1, "SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE", 1, 2000);
        add(1, "VEX_ARMOR_TRIM_SMITHING_TEMPLATE", 1, 2000);
        add(1, "WARD_ARMOR_TRIM_SMITHING_TEMPLATE", 1, 2000);
        add(1, "SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE", 1, 2000);
        add(1, "SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE", 1, 2000);
        add(1, "SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE", 1, 2000);
        add(1, "RIB_ARMOR_TRIM_SMITHING_TEMPLATE", 1, 2000);
        add(1, "EYE_ARMOR_TRIM_SMITHING_TEMPLATE", 1, 2000);
        add(1, "TIDE_ARMOR_TRIM_SMITHING_TEMPLATE", 1, 2000);
        add(1, "WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE", 1, 2000);
        add(1, "GOAT_HORN", 1, 2500);

        // Page 2: tools and arrows
        add(2, "DIAMOND_PICKAXE", 1, 3_200);
        add(2, "DIAMOND_SHOVEL", 1, 2_000);
        add(2, "DIAMOND_HOE", 1, 2_000);
        add(2, "FLINT_AND_STEEL", 1, 200);
        add(2, "SHEARS", 1, 150);
        add(2, "FISHING_ROD", 1, 150);
        add(2, "BRUSH", 1, 150);
        add(2, "SPYGLASS", 1, 400);
        add(2, "COMPASS", 1, 150);
        add(2, "CLOCK", 1, 150);
        add(2, "RECOVERY_COMPASS", 1, 2_500);
        add(2, "MAP", 1, 150);
        add(2, "BUCKET", 1, 100);
        add(2, "WATER_BUCKET", 1, 150);
        add(2, "LAVA_BUCKET", 1, 400);
        add(2, "COD_BUCKET", 1, 250);
        add(2, "LEAD", 4, 100);
        add(2, "NAME_TAG", 1, 300);
        add(2, "SADDLE", 1, 500);
        add(2, "CARROT_ON_A_STICK", 1, 200);
        add(2, "WARPED_FUNGUS_ON_A_STICK", 1, 300);
        add(2, "ENDER_PEARL", 16, 1_600);
        add(2, "ARROW", 64, 400);
        add(2, "SPECTRAL_ARROW", 32, 1_200);
        add(2, "FIREWORK_ROCKET", 64, 1_000);
        addPotion(2, Material.TIPPED_ARROW, "STRENGTH", 8, 1_500);
        addPotion(2, Material.TIPPED_ARROW, "SWIFTNESS", 8, 1_200);
        addPotion(2, Material.TIPPED_ARROW, "POISON", 8, 1_200);
        addPotion(2, Material.TIPPED_ARROW, "SLOWNESS", 8, 1_200);
        addPotion(2, Material.TIPPED_ARROW, "WEAKNESS", 8, 1_200);
        addPotion(2, Material.TIPPED_ARROW, "HARMING", 8, 1_500);
        addPotion(2, Material.TIPPED_ARROW, "SLOW_FALLING", 8, 1_000);
        addPotion(2, Material.TIPPED_ARROW, "LEAPING", 8, 1_000);
        addPotion(2, Material.TIPPED_ARROW, "FIRE_RESISTANCE", 8, 1_000);
        addPotion(2, Material.TIPPED_ARROW, "HEALING", 8, 1_500);
        addPotion(2, Material.TIPPED_ARROW, "REGENERATION", 8, 1_500);

        // Page 3: healing and foods
        add(3, "GOLDEN_APPLE", 4, 2_000);
        add(3, "GOLDEN_CARROT", 32, 600);
        add(3, "COOKED_BEEF", 64, 400);
        add(3, "COOKED_PORKCHOP", 64, 400);
        add(3, "COOKED_CHICKEN", 64, 350);
        add(3, "COOKED_MUTTON", 64, 350);
        add(3, "COOKED_RABBIT", 64, 300);
        add(3, "COOKED_COD", 64, 250);
        add(3, "COOKED_SALMON", 64, 300);
        add(3, "BREAD", 64, 150);
        add(3, "BAKED_POTATO", 64, 200);
        add(3, "APPLE", 32, 150);
        add(3, "CARROT", 64, 100);
        add(3, "POTATO", 64, 100);
        add(3, "BEETROOT", 64, 100);
        add(3, "MELON_SLICE", 64, 100);
        add(3, "SWEET_BERRIES", 32, 100);
        add(3, "GLOW_BERRIES", 32, 150);
        add(3, "CHORUS_FRUIT", 16, 500);
        add(3, "COOKIE", 64, 100);
        add(3, "PUMPKIN_PIE", 16, 200);
        add(3, "CAKE", 1, 150);
        add(3, "MILK_BUCKET", 1, 150);
        add(3, "HONEY_BOTTLE", 8, 300);
        add(3, "BEEF", 64, 200);
        add(3, "PORKCHOP", 64, 200);
        add(3, "CHICKEN", 64, 150);
        add(3, "MUTTON", 64, 150);
        add(3, "RABBIT", 64, 150);
        add(3, "COD", 64, 100);
        add(3, "SALMON", 64, 150);
        add(3, "TROPICAL_FISH", 64, 150);
        add(3, "GLISTERING_MELON_SLICE", 16, 500);
        add(3, "PUMPKIN", 16, 100);
        add(3, "MELON", 16, 150);
        add(3, "BROWN_MUSHROOM", 16, 100);

        // Page 4: ores and materials
        add(4, "DIAMOND", 16, 6_000);
        add(4, "EMERALD", 16, 3_200);
        add(4, "IRON_INGOT", 32, 800);
        add(4, "COPPER_INGOT", 32, 400);
        add(4, "RAW_IRON", 32, 500);
        add(4, "RAW_COPPER", 32, 250);
        add(4, "COAL", 64, 400);
        add(4, "CHARCOAL", 64, 350);
        add(4, "QUARTZ", 32, 600);
        add(4, "AMETHYST_SHARD", 32, 500);
        add(4, "LAPIS_LAZULI", 64, 900);
        add(4, "REDSTONE", 64, 600);
        add(4, "IRON_NUGGET", 64, 400);
        add(4, "STICK", 64, 100);
        add(4, "STRING", 64, 300);
        add(4, "LEATHER", 32, 300);
        add(4, "BONE", 64, 200);
        add(4, "BONE_MEAL", 64, 150);
        add(4, "SLIME_BALL", 32, 500);
        add(4, "BLAZE_ROD", 8, 1_200);
        add(4, "BREEZE_ROD", 4, 800);
        add(4, "GHAST_TEAR", 4, 2_000);
        add(4, "MAGMA_CREAM", 16, 500);
        add(4, "NETHER_WART", 32, 300);
        add(4, "GLOWSTONE_DUST", 32, 400);
        add(4, "PRISMARINE_SHARD", 32, 500);
        add(4, "PRISMARINE_CRYSTALS", 16, 500);
        add(4, "ECHO_SHARD", 4, 3_000);
        add(4, "GUNPOWDER", 32, 600);
        add(4, "BLAZE_POWDER", 16, 800);
        add(4, "FEATHER", 32, 200);
        add(4, "FLINT", 32, 200);
        add(4, "SHULKER_SHELL", 2, 4_000);
        add(4, "PHANTOM_MEMBRANE", 8, 1_000);
        add(4, "ARMADILLO_SCUTE", 8, 1_200);
        add(4, "EXPERIENCE_BOTTLE", 16, 800);

        // Page 5: splash potions
        addPotion(5, Material.SPLASH_POTION, "HEALING", 4, 1_600);
        addPotion(5, Material.SPLASH_POTION, "STRONG_HEALING", 4, 2_400);
        addPotion(5, Material.SPLASH_POTION, "REGENERATION", 4, 1_800);
        addPotion(5, Material.SPLASH_POTION, "LONG_REGENERATION", 4, 2_200);
        addPotion(5, Material.SPLASH_POTION, "STRONG_REGENERATION", 4, 2_600);
        addPotion(5, Material.SPLASH_POTION, "STRENGTH", 4, 2_000);
        addPotion(5, Material.SPLASH_POTION, "LONG_STRENGTH", 4, 2_400);
        addPotion(5, Material.SPLASH_POTION, "STRONG_STRENGTH", 4, 3_000);
        addPotion(5, Material.SPLASH_POTION, "SWIFTNESS", 4, 1_500);
        addPotion(5, Material.SPLASH_POTION, "LONG_SWIFTNESS", 4, 1_900);
        addPotion(5, Material.SPLASH_POTION, "STRONG_SWIFTNESS", 4, 2_300);
        addPotion(5, Material.SPLASH_POTION, "FIRE_RESISTANCE", 4, 1_500);
        addPotion(5, Material.SPLASH_POTION, "LONG_FIRE_RESISTANCE", 4, 1_900);
        addPotion(5, Material.SPLASH_POTION, "NIGHT_VISION", 4, 1_200);
        addPotion(5, Material.SPLASH_POTION, "LONG_NIGHT_VISION", 4, 1_600);
        addPotion(5, Material.SPLASH_POTION, "INVISIBILITY", 4, 1_500);
        addPotion(5, Material.SPLASH_POTION, "LONG_INVISIBILITY", 4, 1_900);
        addPotion(5, Material.SPLASH_POTION, "LEAPING", 4, 1_200);
        addPotion(5, Material.SPLASH_POTION, "LONG_LEAPING", 4, 1_600);
        addPotion(5, Material.SPLASH_POTION, "STRONG_LEAPING", 4, 1_900);
        addPotion(5, Material.SPLASH_POTION, "WATER_BREATHING", 4, 1_200);
        addPotion(5, Material.SPLASH_POTION, "LONG_WATER_BREATHING", 4, 1_600);
        addPotion(5, Material.SPLASH_POTION, "SLOW_FALLING", 4, 1_500);
        addPotion(5, Material.SPLASH_POTION, "LONG_SLOW_FALLING", 4, 1_900);
        addPotion(5, Material.SPLASH_POTION, "TURTLE_MASTER", 4, 2_000);
        addPotion(5, Material.SPLASH_POTION, "STRONG_TURTLE_MASTER", 4, 2_600);
        addPotion(5, Material.SPLASH_POTION, "HARMING", 4, 1_800);
        addPotion(5, Material.SPLASH_POTION, "STRONG_HARMING", 4, 2_600);
        addPotion(5, Material.SPLASH_POTION, "POISON", 4, 1_800);
        addPotion(5, Material.SPLASH_POTION, "STRONG_POISON", 4, 2_400);
        addPotion(5, Material.SPLASH_POTION, "WEAKNESS", 4, 1_500);
        addPotion(5, Material.SPLASH_POTION, "LONG_WEAKNESS", 4, 1_900);
        addPotion(5, Material.SPLASH_POTION, "SLOWNESS", 4, 1_500);
        addPotion(5, Material.SPLASH_POTION, "STRONG_SLOWNESS", 4, 2_200);
        addPotion(5, Material.SPLASH_POTION, "LUCK", 4, 2_500);
        addPotion(5, Material.SPLASH_POTION, "WIND_CHARGED", 4, 2_000);

        // Page 6: combat blocks
        add(6, "OBSIDIAN", 16, 1_200);
        add(6, "COBWEB", 16, 1_000);
        add(6, "TNT", 16, 1_600);
        add(6, "RESPAWN_ANCHOR", 1, 3_500);
        add(6, "POINTED_DRIPSTONE", 16, 800);
        add(6, "POWDER_SNOW_BUCKET", 1, 400);
        add(6, "MAGMA_BLOCK", 16, 700);
        add(6, "SLIME_BLOCK", 8, 900);
        add(6, "HONEY_BLOCK", 8, 900);
        add(6, "SOUL_SAND", 16, 500);
        add(6, "ANVIL", 1, 900);
        add(6, "CACTUS", 16, 300);
        add(6, "CRYING_OBSIDIAN", 8, 1_500);
        add(6, "SOUL_SOIL", 16, 400);
        add(6, "CAMPFIRE", 1, 200);
        add(6, "SOUL_CAMPFIRE", 1, 300);
        add(6, "WITHER_ROSE", 1, 1_500);
        add(6, "TRIPWIRE_HOOK", 8, 300);
        add(6, "PISTON", 4, 600);
        add(6, "STICKY_PISTON", 4, 900);
        add(6, "DISPENSER", 4, 600);
        add(6, "LEVER", 8, 100);
        add(6, "TRAPPED_CHEST", 1, 300);
        add(6, "ICE", 16, 300);
        add(6, "PACKED_ICE", 16, 600);
        add(6, "BLUE_ICE", 8, 800);
        add(6, "GLOWSTONE", 16, 1_200);
        add(6, "SNOW_BLOCK", 16, 150);
        add(6, "LADDER", 16, 200);
        add(6, "SAND", 32, 200);
        add(6, "GRAVEL", 32, 200);
        add(6, "NETHERRACK", 32, 150);
        add(6, "SCAFFOLDING", 16, 300);
        add(6, "COBBLESTONE", 64, 150);
        add(6, "STONE_PRESSURE_PLATE", 8, 150);
        add(6, "END_STONE", 32, 300);
    }

    // --------------------------------------------------------------------- UI

    boolean command(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can open the shop.");
            return true;
        }
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                page = -1;
            }
            if (page < 1 || page > PAGES) {
                sender.sendMessage(ChatColor.RED + "Usage: /shop <1-" + PAGES + ">");
                return true;
            }
        }
        open((Player) sender, page);
        return true;
    }

    private static ItemStack named(Material m, String name, String... lore) {
        ItemStack s = new ItemStack(m);
        ItemMeta meta = s.getItemMeta();
        meta.setDisplayName(name);
        List<String> l = new ArrayList<>();
        for (String line : lore) {
            l.add(line);
        }
        meta.setLore(l);
        s.setItemMeta(meta);
        return s;
    }

    private ItemStack display(Product product) {
        ItemStack s = product.stack.clone();
        ItemMeta meta = s.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GOLD + "Price: " + plugin.money(product.price));
        lore.add(ChatColor.GRAY + "Click to buy. Shift-click for 5.");
        meta.setLore(lore);
        s.setItemMeta(meta);
        return s;
    }

    void open(Player player, int page) {
        Inventory inv = Bukkit.createInventory(new Holder(page), 54,
                ChatColor.DARK_GRAY + "Shop: " + PAGE_NAMES[page - 1] + " (" + page + "/" + PAGES + ")");
        List<Product> products = pages.get(page - 1);
        for (int slot = 0; slot < ITEM_AREA && slot < products.size(); slot++) {
            inv.setItem(slot, display(products.get(slot)));
        }
        ItemStack filler = named(PAGE_PANES[page - 1], " ");
        for (int slot = 45; slot < 54; slot++) {
            inv.setItem(slot, filler);
        }
        if (page > 1) {
            inv.setItem(PREV, named(Material.ARROW, ChatColor.YELLOW + "< Page " + (page - 1),
                    ChatColor.GRAY + PAGE_NAMES[page - 2]));
        }
        if (page < PAGES) {
            inv.setItem(NEXT, named(Material.ARROW, ChatColor.YELLOW + "Page " + (page + 1) + " >",
                    ChatColor.GRAY + PAGE_NAMES[page]));
        }
        inv.setItem(INFO, named(Material.GOLD_INGOT, ChatColor.GOLD + "Your balance: "
                + plugin.money(plugin.getBalance(player.getUniqueId())),
                ChatColor.GRAY + "/shop <1-" + PAGES + "> to jump to a page."));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof Holder)) {
            return;
        }
        event.setCancelled(MoneyPlugin.blocksMenuClick(event, top));
        if (!(event.getWhoClicked() instanceof Player) || event.getClickedInventory() != top) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        int page = ((Holder) top.getHolder()).page;
        int slot = event.getSlot();
        if (slot == PREV && page > 1) {
            open(player, page - 1);
        } else if (slot == NEXT && page < PAGES) {
            open(player, page + 1);
        } else if (slot < ITEM_AREA && slot < pages.get(page - 1).size()) {
            boolean bulk = event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT;
            buy(player, page, pages.get(page - 1).get(slot), bulk ? 5 : 1);
        }
    }

    private void buy(Player player, int page, Product product, int times) {
        long total = product.price * times;
        long balance = plugin.getBalance(player.getUniqueId());
        if (balance < total) {
            player.sendMessage(ChatColor.RED + "You need " + plugin.money(total - balance) + " more.");
            return;
        }
        Inventory preview = Bukkit.createInventory(null, 36);
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                contents[i] = contents[i].clone();
            }
        }
        preview.setStorageContents(contents);
        for (int n = 0; n < times; n++) {
            if (!preview.addItem(product.stack.clone()).isEmpty()) {
                player.sendMessage(ChatColor.RED + "Your inventory is full.");
                return;
            }
        }
        if (!plugin.charge(player.getUniqueId(), total)) {
            return;
        }
        player.getInventory().setStorageContents(preview.getStorageContents());
        player.sendMessage(ChatColor.GREEN + "Bought " + (product.stack.getAmount() * times) + "x "
                + product.label + " for " + plugin.money(total) + ". Balance: "
                + plugin.money(plugin.getBalance(player.getUniqueId())));
        open(player, page);
    }

    private static String pretty(Material m) {
        return pretty(m.name());
    }

    private static String pretty(String name) {
        StringBuilder out = new StringBuilder();
        for (String word : name.split("_")) {
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(word.charAt(0)).append(word.substring(1).toLowerCase());
        }
        return out.toString();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder && MoneyPlugin.dragTouchesMenu(event)) {
            event.setCancelled(true);
        }
    }
}

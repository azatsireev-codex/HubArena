package net.akat.shop;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.*;

public class ShopManager {

    private final List<ShopItem> shopItems = new ArrayList<>();
    private final Map<UUID, Map<ShopItem, Integer>> pending = new HashMap<>();
    private FileConfiguration shopConfig;

    public ShopManager(JavaPlugin plugin) { load(plugin); }

    public void load(JavaPlugin plugin) {
        shopItems.clear();
        File file = new File(plugin.getDataFolder(), "shop.yml");
        if (!file.exists()) plugin.saveResource("shop.yml", false);

        shopConfig = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> list = shopConfig.getMapList("shop");
        if (list == null || list.isEmpty()) {
            plugin.getLogger().warning("[Shop] В shop.yml нет элементов!");
            return;
        }

        for (Map<?, ?> raw : list) {
            Map<String, Object> map = (Map<String, Object>) raw;

            String name = map.getOrDefault("name", "Item").toString();
            Material material = Material.matchMaterial(map.getOrDefault("material", "STONE").toString().toUpperCase());
            if (material == null) material = Material.STONE;

            double price1 = map.get("price1") instanceof Number n ? n.doubleValue() : 0;
            double price2 = map.get("price2") instanceof Number n2 ? n2.doubleValue() : 0;
            int amount = map.containsKey("amount") ? ((Number) map.get("amount")).intValue() : 1;

            ShopAvailability availability;
            try {
                availability = ShopAvailability.valueOf(map.getOrDefault("mode", "BOTH").toString().toUpperCase());
            } catch (Exception e) {
                availability = ShopAvailability.BOTH;
            }

            List<String> lore = new ArrayList<>();

            // Загрузка зачарований
            Map<Enchantment, Integer> enchants = new HashMap<>();
            if (map.containsKey("enchants")) {
                Map<?, ?> enchantMap = (Map<?, ?>) map.get("enchants");
                for (Map.Entry<?, ?> entry : enchantMap.entrySet()) {
                    Enchantment enchant = Enchantment.getByName(entry.getKey().toString().toUpperCase());
                    if (enchant != null) {
                        enchants.put(enchant, ((Number) entry.getValue()).intValue());
                    }
                }
            }

            // Загрузка эффектов зелий
            Map<PotionEffectType, Integer> potionEffects = new HashMap<>();
            if (map.containsKey("effects")) {
                Map<?, ?> effectMap = (Map<?, ?>) map.get("effects");
                for (Map.Entry<?, ?> entry : effectMap.entrySet()) {
                    PotionEffectType effect = PotionEffectType.getByName(entry.getKey().toString().toUpperCase());
                    if (effect != null) {
                        potionEffects.put(effect, ((Number) entry.getValue()).intValue());
                    }
                }
            }

            shopItems.add(new ShopItem(name, material, price1, price2, lore, availability,
                    enchants, potionEffects, amount));
        }

        plugin.getLogger().info("[Shop] Загружено предметов: " + shopItems.size());
    }

    public List<ShopItem> getItems() { return shopItems; }

    public List<ShopItem> getPendingItems(Player player) {
        List<ShopItem> result = new ArrayList<>();
        Map<ShopItem, Integer> playerPending = pending.get(player.getUniqueId());
        if (playerPending != null) {
            for (Map.Entry<ShopItem, Integer> entry : playerPending.entrySet()) {
                for (int i = 0; i < entry.getValue(); i++) {
                    result.add(entry.getKey());
                }
            }
        }
        return result;
    }

    // Новый метод для получения количества каждого предмета
    public Map<ShopItem, Integer> getPendingItemsWithCount(Player player) {
        return pending.getOrDefault(player.getUniqueId(), new HashMap<>());
    }

    public void addPending(Player player, ShopItem item) {
        Map<ShopItem, Integer> playerPending = pending.computeIfAbsent(player.getUniqueId(), x -> new HashMap<>());
        playerPending.put(item, playerPending.getOrDefault(item, 0) + 1);
    }

    public void givePending(Player player) {
        Map<ShopItem, Integer> playerPending = pending.remove(player.getUniqueId());
        if (playerPending == null) return;

        for (Map.Entry<ShopItem, Integer> entry : playerPending.entrySet()) {
            ShopItem shopItem = entry.getKey();
            int quantity = entry.getValue();

            // Используем createItemStack() вместо простого ItemStack
            for (int i = 0; i < quantity; i++) {
                ItemStack itemStack = shopItem.createItemStack();
                player.getInventory().addItem(itemStack);
            }
        }
        player.sendMessage("§aВам выданы предметы, купленные вне PVP!");
    }

    public void clear(Player player) { pending.remove(player.getUniqueId()); }
}

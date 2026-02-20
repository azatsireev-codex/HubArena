package net.akat.shop;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopItem {

    private final String name;
    private final Material material;
    private final double price1;
    private final double price2;
    private final List<String> lore;
    private final ShopAvailability availability;
    private final Map<Enchantment, Integer> enchants;
    private final Map<PotionEffectType, Integer> potionEffects;
    private final int amount;

    public ShopItem(String name, Material material, double price1, double price2,
                    List<String> lore, ShopAvailability availability,
                    Map<Enchantment, Integer> enchants,
                    Map<PotionEffectType, Integer> potionEffects,
                    int amount) {
        this.name = name;
        this.material = material;
        this.price1 = price1;
        this.price2 = price2;
        this.lore = lore;
        this.availability = availability;
        this.enchants = enchants != null ? enchants : new HashMap<>();
        this.potionEffects = potionEffects != null ? potionEffects : new HashMap<>();
        this.amount = amount;
    }

    // Геттеры
    public String getName() { return name; }
    public Material getMaterial() { return material; }
    public double getPrice1() { return price1; }
    public double getPrice2() { return price2; }
    public List<String> getLore() { return lore; }
    public ShopAvailability getAvailability() { return availability; }
    public Map<Enchantment, Integer> getEnchants() { return enchants; }
    public Map<PotionEffectType, Integer> getPotionEffects() { return potionEffects; }
    public int getAmount() { return amount; }

    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(material, amount);

        // Применяем зачарования
        for (Map.Entry<Enchantment, Integer> enchant : enchants.entrySet()) {
            item.addUnsafeEnchantment(enchant.getKey(), enchant.getValue());
        }

        // Если это зелье или стрела с эффектами - применяем эффекты
        if (material == Material.POTION || material == Material.SPLASH_POTION ||
                material == Material.LINGERING_POTION || material == Material.TIPPED_ARROW) {

            // Получаем PotionMeta для зелий и стрел с эффектами
            if (item.getItemMeta() instanceof PotionMeta) {
                PotionMeta meta = (PotionMeta) item.getItemMeta();

                // Очищаем старые эффекты
                meta.clearCustomEffects();

                // Добавляем кастомные эффекты
                for (Map.Entry<PotionEffectType, Integer> effect : potionEffects.entrySet()) {
                    // Для зелий - длительность 3 минуты, для стрел - 10 секунд
                    int duration = (material == Material.TIPPED_ARROW) ?
                            (20 * 10) : // 10 секунд для стрел
                            (20 * 60 * 3); // 3 минуты для зелий

                    // Уровень эффекта: amplifier = level - 1
                    int amplifier = effect.getValue() - 1;
                    PotionEffect potionEffect = new PotionEffect(effect.getKey(), duration, amplifier, true, true);
                    meta.addCustomEffect(potionEffect, true);
                }

                // Устанавливаем цвет на основе первого эффекта
                if (!potionEffects.isEmpty()) {
                    PotionEffectType mainEffect = potionEffects.keySet().iterator().next();
                    meta.setColor(mainEffect.getColor());
                }

                item.setItemMeta(meta);
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e" + name);
            meta.setUnbreakable(true);

            item.setItemMeta(meta);
        }

        return item;
    }
}

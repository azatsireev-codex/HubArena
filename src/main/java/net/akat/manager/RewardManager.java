package net.akat.manager;

import net.akat.Main;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class RewardManager {

    private final Map<Integer, RewardSet> rewardSets = new HashMap<>();
    private final Map<UUID, PlayerProgress> playerProgress = new HashMap<>();
    private final File rewardFile;
    private FileConfiguration rewardConfig;

    public RewardManager() {
        rewardFile = new File(Main.getInstance().getDataFolder(), "rewards.yml");
        loadRewards();
    }

    /** Загружаем rewards.yml */
    public void loadRewards() {
        if (!rewardFile.exists()) {
            Main.getInstance().saveResource("rewards.yml", false);
        }
        rewardConfig = YamlConfiguration.loadConfiguration(rewardFile);
        rewardSets.clear();

        if (!rewardConfig.contains("rewards")) return;

        for (String key : rewardConfig.getConfigurationSection("rewards").getKeys(false)) {

            int kills = Integer.parseInt(key);
            RewardSet set = new RewardSet();

            set.helmet = loadItem("rewards." + key + ".helmet");
            set.chestplate = loadItem("rewards." + key + ".chestplate");
            set.leggings = loadItem("rewards." + key + ".leggings");
            set.boots = loadItem("rewards." + key + ".boots");

            if (rewardConfig.contains("rewards." + key + ".items")) {
                List<?> rawList = rewardConfig.getList("rewards." + key + ".items");
                if (rawList != null) {
                    for (Object obj : rawList) {
                        if (obj instanceof Map<?,?> map) {
                            ItemStack item = parseItem(map);
                            if (item != null) set.items.add(item);
                        }
                    }
                }
            }
            rewardSets.put(kills, set);
        }
    }

    /** Выдаёт следующий предмет строго для конкретного уровня убийств */
    public void giveNextReward(Player player) {

        UUID uuid = player.getUniqueId();
        PlayerProgress progress = playerProgress.computeIfAbsent(uuid, k -> new PlayerProgress());

        // Уровни по порядку 0,1,2,3...
        List<Integer> levels = new ArrayList<>(rewardSets.keySet());
        Collections.sort(levels);

        for (int level : levels) {
            RewardSet set = rewardSets.get(level);
            if (set == null) continue;

            List<ItemStack> available = new ArrayList<>();

            // --- Броня ---
            if (set.helmet != null && !progress.givenArmor.contains(level + ":helmet"))
                available.add(set.helmet);

            if (set.chestplate != null && !progress.givenArmor.contains(level + ":chestplate"))
                available.add(set.chestplate);

            if (set.leggings != null && !progress.givenArmor.contains(level + ":leggings"))
                available.add(set.leggings);

            if (set.boots != null && !progress.givenArmor.contains(level + ":boots"))
                available.add(set.boots);

            // --- Items ---
            for (int i = 0; i < set.items.size(); i++) {
                String key = level + ":" + i;
                if (!progress.givenItems.contains(key))
                    available.add(set.items.get(i));
            }

            // Если на уровне есть невырданные предметы — выдаём ОДИН
            if (!available.isEmpty()) {

                ItemStack chosen = available.get(new Random().nextInt(available.size()));
                ItemStack itemToGive = makeUnbreakable(chosen.clone());
                PlayerInventory inv = player.getInventory();

                // Броня
                if (chosen.equals(set.helmet)) {
                    inv.setHelmet(itemToGive);
                    progress.givenArmor.add(level + ":helmet");

                } else if (chosen.equals(set.chestplate)) {
                    inv.setChestplate(itemToGive);
                    progress.givenArmor.add(level + ":chestplate");

                } else if (chosen.equals(set.leggings)) {
                    inv.setLeggings(itemToGive);
                    progress.givenArmor.add(level + ":leggings");

                } else if (chosen.equals(set.boots)) {
                    inv.setBoots(itemToGive);
                    progress.givenArmor.add(level + ":boots");

                } else {
                    int index = set.items.indexOf(chosen);
                    if (isWeapon(chosen)) {
                        replaceWeapon(inv, itemToGive);
                        progress.givenItems.add(level + ":" + index);

                    } else {
                        inv.addItem(itemToGive);
                        progress.givenItems.add(level + ":" + index);
                    }
                }

                player.sendMessage("§aВы получили предмет: §f" + chosen.getType());
                return;
            }
        }

        // Если все уровни полностью выданы
        player.sendMessage("§eВсе награды уже получены!");
    }

    /** Сбрасываем прогресс игрока */
    public void resetPlayerProgress(Player player) {
        playerProgress.remove(player.getUniqueId());
    }

    /** Перезагрузка rewards.yml */
    public void reloadRewards() {
        loadRewards();
    }

    // --- Вспомогательные методы ---
    private ItemStack parseItem(Map<?, ?> map) {
        Object typeObj = map.get("type");
        if (typeObj == null) return null;

        Material mat = Material.matchMaterial(typeObj.toString());
        if (mat == null) return null;

        int amount = map.containsKey("amount") ? (int) map.get("amount") : 1;
        ItemStack item = new ItemStack(mat, amount);

        if (map.containsKey("enchants")) {
            Map<String, Object> enchants = (Map<String, Object>) map.get("enchants");
            for (String ench : enchants.keySet()) {
                Enchantment e = Enchantment.getByName(ench);
                if (e != null) item.addUnsafeEnchantment(e, (int) enchants.get(ench));
            }
        }
        return makeUnbreakable(item);
    }

    private ItemStack loadItem(String path) {
        if (!rewardConfig.contains(path + ".type")) return null;

        Material mat = Material.matchMaterial(rewardConfig.getString(path + ".type"));
        if (mat == null) return null;

        ItemStack item = new ItemStack(mat);

        if (rewardConfig.contains(path + ".enchants")) {
            for (String enchName :
                    rewardConfig.getConfigurationSection(path + ".enchants").getKeys(false)) {

                Enchantment ench = Enchantment.getByName(enchName);
                if (ench != null) {
                    int lvl = rewardConfig.getInt(path + ".enchants." + enchName);
                    item.addUnsafeEnchantment(ench, lvl);
                }
            }
        }
        return makeUnbreakable(item);
    }

    private void replaceWeapon(PlayerInventory inv, ItemStack newWeapon) {

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);

            if (it != null && isWeapon(it)) {
                inv.setItem(i, newWeapon.clone());
                return;
            }
        }

        inv.addItem(newWeapon.clone());
    }

    private ItemStack makeUnbreakable(ItemStack item) {
        if (item == null) return item;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }

        return item;
    }

    /** Хранение прогресса игрока по выдаче */
    private static class PlayerProgress {
        Set<String> givenArmor = new HashSet<>();
        Set<String> givenItems = new HashSet<>();
    }

    /** Хранение комплекта наград */
    private static class RewardSet {
        ItemStack helmet, chestplate, leggings, boots;
        List<ItemStack> items = new ArrayList<>();
    }

    private boolean isWeapon(ItemStack item) {
        if (item == null) return false;

        Material type = item.getType();

        return type.name().endsWith("_SWORD")
                || type.name().endsWith("_AXE");
    }
}
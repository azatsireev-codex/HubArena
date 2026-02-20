package net.akat.manager;

import net.akat.Main;
import net.akat.top.RatingDisplayFormatter;
import net.akat.top.TopDisplayManager;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ArenaManager {

    private Location corner1;
    private Location corner2;
    private List<Location> teleportPoints;
    private Location exitLocation;

    private TopDisplayManager ratingDisplayManager;

    private final Set<UUID> pvpPlayers = new HashSet<>();
    private final Map<UUID, ItemStack[]> savedInventory = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    private final Map<UUID, LastHitInfo> lastDamage = new HashMap<>();
    private final Set<UUID> diedInCombat = new HashSet<>();

    public ArenaManager() {
        this.ratingDisplayManager = new TopDisplayManager(new RatingDisplayFormatter());
        reloadConfig();
    }

    public void reloadConfig() {
        FileConfiguration config = Main.getInstance().getConfig();

        // Загрузка базовых настроек арены
        loadArenaConfig(config);

        // Загрузка дисплеев
        ratingDisplayManager.loadFromConfig(config, "top-display");

        // Создание дисплеев
        ratingDisplayManager.createDisplay();

        // Запуск автообновления
        ratingDisplayManager.startAutoUpdate();
    }

    private void loadArenaConfig(FileConfiguration config) {
        // Границы арены
        corner1 = new Location(
                Bukkit.getWorld("hub"),
                config.getInt("arena.corner1.x"),
                config.getInt("arena.corner1.y"),
                config.getInt("arena.corner1.z")
        );

        corner2 = new Location(
                Bukkit.getWorld("hub"),
                config.getInt("arena.corner2.x"),
                config.getInt("arena.corner2.y"),
                config.getInt("arena.corner2.z")
        );

        // Точки телепортации
        teleportPoints = new ArrayList<>();
        if (config.contains("teleportPoints")) {
            for (Map<?, ?> map : config.getMapList("teleportPoints")) {
                Number xNum = (Number) map.get("x");
                Number yNum = (Number) map.get("y");
                Number zNum = (Number) map.get("z");

                if (xNum != null && yNum != null && zNum != null) {
                    double x = xNum.doubleValue();
                    double y = yNum.doubleValue();
                    double z = zNum.doubleValue();
                    teleportPoints.add(new Location(Bukkit.getWorld("hub"), x, y, z));
                }
            }
        }

        // Локация выхода из PVP
        if (config.contains("exitLocation")) {
            exitLocation = new Location(
                    Bukkit.getWorld("hub"),
                    config.getInt("exitLocation.x"),
                    config.getInt("exitLocation.y"),
                    config.getInt("exitLocation.z")
            );
        } else {
            exitLocation = null;
        }
    }

    public void removeDisplays() {
        ratingDisplayManager.removeDisplay();
    }

    public void forceUpdateDisplays() {
        ratingDisplayManager.forceUpdate();
    }

    public boolean isPlayerInArena(Player player) {
        Location playerLoc = player.getLocation();

        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());

        return playerLoc.getX() >= minX && playerLoc.getX() <= maxX &&
                playerLoc.getY() >= minY && playerLoc.getY() <= maxY &&
                playerLoc.getZ() >= minZ && playerLoc.getZ() <= maxZ;
    }

    public boolean isPlayerOutsideArenaWithBuffer(Player player, double bufferDistance) {
        Location playerLoc = player.getLocation();

        double minX = Math.min(corner1.getX(), corner2.getX()) - bufferDistance;
        double maxX = Math.max(corner1.getX(), corner2.getX()) + bufferDistance;
        double minZ = Math.min(corner1.getZ(), corner2.getZ()) - bufferDistance;
        double maxZ = Math.max(corner1.getZ(), corner2.getZ()) + bufferDistance;

        // Если игрок внутри расширенных границ - он еще не слишком далеко
        if (playerLoc.getX() >= minX && playerLoc.getX() <= maxX &&
                playerLoc.getZ() >= minZ && playerLoc.getZ() <= maxZ) {
            return false;
        }

        return true;
    }

    public double getDistanceToArena(Player player) {
        Location playerLoc = player.getLocation();

        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());

        // Находим ближайшую точку на границе арены
        double closestX = Math.max(minX, Math.min(playerLoc.getX(), maxX));
        double closestZ = Math.max(minZ, Math.min(playerLoc.getZ(), maxZ));

        Location closestPoint = new Location(playerLoc.getWorld(), closestX, playerLoc.getY(), closestZ);
        return playerLoc.distance(closestPoint);
    }

    public void teleportPlayerToRandomLocation(Player player) {
        if (teleportPoints.isEmpty()) {
            player.sendMessage("§cОшибка: точки телепортации не заданы!");
            return;
        }

        Random rand = new Random();
        Location randomLocation = teleportPoints.get(rand.nextInt(teleportPoints.size()));

        if (randomLocation.getWorld() == null) {
            player.sendMessage("§cОшибка: мир для телепортации не найден!");
            return;
        }

        player.teleport(randomLocation);
    }

    // --- Методы для PVP режима с сохранением инвентаря ---

    public void enterPVP(Player player) {
        UUID uuid = player.getUniqueId();
        if (!pvpPlayers.contains(uuid)) {
            // Сохраняем текущий инвентарь
            savedInventory.put(uuid, player.getInventory().getContents());
            savedArmor.put(uuid, player.getInventory().getArmorContents());

            // Очищаем инвентарь для PVP
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);

            pvpPlayers.add(uuid);

            // Выдаём случайный стартовый предмет из набора 0
            Main.getInstance().getRewardManager().giveNextReward(player);
            Main.getInstance().getShopManager().givePending(player);
        }
    }

    public void exitPVP(Player player) {
        UUID uuid = player.getUniqueId();
        if (!pvpPlayers.contains(uuid)) return;

        boolean died = diedInCombat.contains(uuid);

        if (!died) {
            LastHitInfo info = lastDamage.get(uuid);

            if (info != null) {
                long now = System.currentTimeMillis();

                if (now - info.timestamp <= 10000) {
                    Player killer = Bukkit.getPlayer(info.damager);

                    if (killer != null && killer.isOnline() && isInPVPMode(killer)) {

                        Main.getInstance().getRatingManager().addKill(killer);
                        Main.getInstance().getRatingManager().updateRating(killer, true);
                        Main.getInstance().getRewardManager().giveNextReward(killer);

                        dropApplesAndCarrots(player);
                        playKillSound(killer);
                        sendCustomDeathMessage(killer, player);
                        calculateKillReward(killer, player);
                    }
                }
            }
        }

        // Удаляем отметку о смерти
        diedInCombat.remove(uuid);

        // Чистим lastDamage (и как жертву, и как атакующего)
        lastDamage.remove(uuid);
        lastDamage.values().removeIf(hit -> hit.damager.equals(uuid));

        // Очистка PVP-инвентаря
        player.closeInventory();
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        Main.getInstance().getShopManager().clear(player);

        // Восстанавливаем обычный инвентарь
        if (savedInventory.containsKey(uuid))
            player.getInventory().setContents(savedInventory.get(uuid));

        if (savedArmor.containsKey(uuid))
            player.getInventory().setArmorContents(savedArmor.get(uuid));

        // Удаляем сохранённые данные
        savedInventory.remove(uuid);
        savedArmor.remove(uuid);

        pvpPlayers.remove(uuid);

        Main.getInstance().getRatingManager().resetKills(player);
        Main.getInstance().getRewardManager().resetPlayerProgress(player);

        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExhaustion(0f);
        player.setFireTicks(0);
        player.clearActivePotionEffects();

        if (exitLocation != null) {
            player.teleport(exitLocation.clone().add(0.5, 0, 0.5));
        }
    }

    public void playKillSound(Player killer) {
        Location loc = killer.getLocation();
        World world = killer.getWorld();

        world.playSound(loc, Sound.ENTITY_PIG_DEATH, 1.2f, 1.0f);
    }

    public void dropApplesAndCarrots(Player player) {
        Location location = player.getLocation();
        World world = player.getWorld();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && (item.getType() == Material.APPLE || item.getType() == Material.GOLDEN_CARROT)) {
                world.dropItemNaturally(location, item.clone());
            }
        }
    }

    public void sendCustomDeathMessage(Player killer, Player victim) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (isInPVPMode(online)) {
                online.sendMessage(
                        net.md_5.bungee.api.ChatColor.of("#FF5555") + "⚔ " +
                                net.md_5.bungee.api.ChatColor.of("#FFAA00") + killer.getName() +
                                net.md_5.bungee.api.ChatColor.of("#FFFFFF") + " уничтожил " +
                                net.md_5.bungee.api.ChatColor.of("#55FF55") + victim.getName() +
                                net.md_5.bungee.api.ChatColor.of("#FF5555") + " ⚔"
                );
            }
        }
    }

    public int calculateKillReward(Player killer, Player victim) {
        int baseReward = 10; // Базовая награда
        List<String> bonusMessages = new ArrayList<>();
        bonusMessages.add("§a+" + baseReward + " за убийство");
        int killerRating = Main.getInstance().getRatingManager().getPlayerRating(killer);
        int victimRating = Main.getInstance().getRatingManager().getPlayerRating(victim);

        if (victimRating > killerRating) {
            int ratingDifference = victimRating - killerRating;
            int bonus = Math.min(ratingDifference / 10, 20);
            if (bonus > 0) {
                baseReward += bonus;
                bonusMessages.add("§6+" + bonus + " за победу над сильным противником");
            }
        }

        int killStreak = Main.getInstance().getRatingManager().getKills(killer);
        if (killStreak >= 10) {
            baseReward += 10;
            bonusMessages.add("§6+10 за серию из 10+ убийств!");
        } else if (killStreak >= 5) {
            baseReward += 5;
            bonusMessages.add("§6+5 за серию из 5+ убийств");
        }

        killer.sendMessage("§e┌─ Награда за убийство ──");
        for (String message : bonusMessages) {
            killer.sendMessage("§e│ " + message);
        }
        killer.sendMessage("§e└─ Итого: §a" + baseReward + " Кубиславов");

        return Math.max(baseReward, 5);
    }

    public void setLastHit(Player victim, Player damager) {
        lastDamage.put(victim.getUniqueId(), new LastHitInfo(
                damager.getUniqueId(),
                System.currentTimeMillis()
        ));
    }

    public void markDiedInCombat(Player player) {
        diedInCombat.add(player.getUniqueId());
    }

    public boolean isInPVPMode(Player player) {
        return pvpPlayers.contains(player.getUniqueId());
    }

    // --- Геттеры ---
    public Location getCorner1() { return corner1; }
    public Location getCorner2() { return corner2; }
    public Set<UUID> getPVPPlayers() { return Collections.unmodifiableSet(pvpPlayers); }
    public Location getExitLocation() { return exitLocation; }
    public List<Location> getTeleportPoints() { return Collections.unmodifiableList(teleportPoints); }

    // Геттеры для менеджеров дисплеев
    public TopDisplayManager getRatingDisplayManager() { return ratingDisplayManager; }

    private static class LastHitInfo {
        UUID damager;
        long timestamp;

        public LastHitInfo(UUID damager, long timestamp) {
            this.damager = damager;
            this.timestamp = timestamp;
        }
    }
}

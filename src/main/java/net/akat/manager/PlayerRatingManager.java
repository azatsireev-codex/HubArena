package net.akat.manager;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerRatingManager {

    private final Map<String, Integer> playerRatings;
    private final Map<String, Integer> playerKills;
    private final File dataFile;
    private final JavaPlugin plugin;

    public PlayerRatingManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerRatings = new HashMap<>();
        this.playerKills = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "player_data.bin");
        loadData();
    }

    /** Ультра-быстрая загрузка */
    public void loadData() {
        if (!dataFile.exists()) return;

        try (DataInputStream dis = new DataInputStream(new FileInputStream(dataFile))) {
            // Читаем рейтинги
            int ratingsSize = dis.readInt();
            for (int i = 0; i < ratingsSize; i++) {
                String playerName = dis.readUTF();
                int rating = dis.readInt();
                playerRatings.put(playerName, rating);
            }

            // Читаем убийства
            int killsSize = dis.readInt();
            for (int i = 0; i < killsSize; i++) {
                String playerName = dis.readUTF();
                int kills = dis.readInt();
                playerKills.put(playerName, kills);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка загрузки данных: " + e.getMessage());
        }
    }

    /** Ультра-быстрое сохранение */
    public void saveData() {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(dataFile))) {
            // Сохраняем рейтинги
            dos.writeInt(playerRatings.size());
            for (Map.Entry<String, Integer> entry : playerRatings.entrySet()) {
                dos.writeUTF(entry.getKey());
                dos.writeInt(entry.getValue());
            }

            // Сохраняем убийства
            dos.writeInt(playerKills.size());
            for (Map.Entry<String, Integer> entry : playerKills.entrySet()) {
                dos.writeUTF(entry.getKey());
                dos.writeInt(entry.getValue());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка сохранения данных: " + e.getMessage());
        }
    }

    public void forceSave() {
        saveData();
    }

    /** Обновляет рейтинг игрока при победе или поражении */
    public void updateRating(Player player, boolean win) {
        int currentRating = playerRatings.getOrDefault(player.getName(), 1000);
        int ratingChange = win ? 10 : -10;
        playerRatings.put(player.getName(), Math.max(0, currentRating + ratingChange)); // не меньше 0
    }

    /** Возвращает текущий рейтинг игрока */
    public int getPlayerRating(Player player) {
        return playerRatings.getOrDefault(player.getName(), 1000);
    }

    /** Возвращает рейтинг игрока по имени */
    public int getPlayerRating(String playerName) {
        return playerRatings.getOrDefault(playerName, 1000);
    }

    /** Увеличивает количество убийств */
    public void addKill(Player player) {
        String name = player.getName();
        playerKills.put(name, playerKills.getOrDefault(name, 0) + 1);
    }

    /** Возвращает количество убийств игрока */
    public int getKills(Player player) {
        return playerKills.getOrDefault(player.getName(), 0);
    }

    /** Сбрасывает количество убийств игрока */
    public void resetKills(Player player) {
        playerKills.put(player.getName(), 0);
    }

    /** Полный сброс данных (напр. при reloadconfig) */
    public void reload() {
        playerKills.clear();
        playerRatings.clear();
    }

    /** Получение топа игроков по рейтингу */
    public List<String> getTopPlayers(int limit) {
        List<String> topPlayers = new ArrayList<>();

        // Сортируем игроков по рейтингу (по убыванию) и берем топ N
        playerRatings.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue())) // сортировка по убыванию
                .limit(limit)
                .forEach(entry -> {
                    String playerName = entry.getKey();
                    int rating = entry.getValue();
                    topPlayers.add(playerName + ":" + rating);
                });

        return topPlayers;
    }

    /** Получение топа игроков по убийствам */
    public List<String> getTopKillers(int limit) {
        List<String> topKillers = new ArrayList<>();

        playerKills.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .limit(limit)
                .forEach(entry -> {
                    String playerName = entry.getKey();
                    int kills = entry.getValue();
                    topKillers.add(playerName + ":" + kills);
                });

        return topKillers;
    }

    /** Получение всех игроков с рейтингом (для отладки) */
    public Map<String, Integer> getAllRatings() {
        return new HashMap<>(playerRatings);
    }

    /** Получение всех убийств (для отладки) */
    public Map<String, Integer> getAllKills() {
        return new HashMap<>(playerKills);
    }
}

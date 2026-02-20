package net.akat.top;

import net.akat.Main;
import net.akat.manager.PlayerRatingManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;

import java.util.List;

public class TopDisplayManager {

    private final DisplayFormatter formatter;
    private TextDisplay textDisplay;
    private Location displayLocation;
    private int updateInterval;
    private boolean enabled;

    public TopDisplayManager(DisplayFormatter formatter) {
        this.formatter = formatter;
    }

    public void loadFromConfig(FileConfiguration config, String configPath) {
        this.enabled = config.getBoolean(configPath + ".enabled", false);

        if (!enabled) return;

        String worldName = config.getString(configPath + ".location.world", "hub");
        World world = Bukkit.getWorld(worldName);

        if (world != null) {
            this.displayLocation = new Location(
                    world,
                    config.getDouble(configPath + ".location.x"),
                    config.getDouble(configPath + ".location.y"),
                    config.getDouble(configPath + ".location.z")
            );
            this.updateInterval = config.getInt(configPath + ".update-interval", 30);
        }
    }

    public void createDisplay() {
        if (!enabled || displayLocation == null) return;

        // Удаляем старый TextDisplay если есть
        if (textDisplay != null) {
            textDisplay.remove();
        }

        // Создаем новый TextDisplay
        textDisplay = (TextDisplay) displayLocation.getWorld().spawnEntity(
                displayLocation, EntityType.TEXT_DISPLAY
        );

        configureDisplay();
        updateDisplay();
    }

    private void configureDisplay() {
        textDisplay.setCustomNameVisible(false);
        textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
        textDisplay.setSeeThrough(true);
        textDisplay.setBackgroundColor(Color.fromARGB(0));
        textDisplay.setBillboard(Display.Billboard.CENTER);
        textDisplay.setShadowed(true);
        textDisplay.setShadowStrength(0.8f);
        textDisplay.setShadowRadius(2.0f);
    }

    public void updateDisplay() {
        if (textDisplay == null || textDisplay.isDead()) return;

        PlayerRatingManager ratingManager = Main.getInstance().getRatingManager();
        if (ratingManager == null) {
            textDisplay.text(Component.text("§cЗагрузка..."));
            return;
        }

        List<String> topPlayers = ratingManager.getTopPlayers(10);

        if (topPlayers.isEmpty()) {
            textDisplay.text(formatter.getEmptyMessage());
        } else {
            textDisplay.text(formatter.formatTopPlayers(topPlayers));
        }
    }

    public void startAutoUpdate() {
        if (!enabled) return;

        Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            if (textDisplay != null && !textDisplay.isDead()) {
                updateDisplay();
            }
        }, 0L, updateInterval * 20L);
    }

    public void removeDisplay() {
        if (textDisplay != null && !textDisplay.isDead()) {
            textDisplay.remove();
        }
    }

    public void forceUpdate() {
        updateDisplay();
    }

    public boolean isEnabled() {
        return enabled;
    }
}

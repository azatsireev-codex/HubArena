package net.akat.listener;

import net.akat.Main;
import net.akat.manager.ArenaManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArenaMoveListener implements Listener {

    private final ArenaManager arenaManager = Main.getInstance().getArenaManager();

    private final Map<UUID, Long> lastMessageTime = new HashMap<>();
    private final Map<UUID, Long> lastTeleportTime = new HashMap<>();

    private static final long MESSAGE_COOLDOWN = 2000;
    private static final long TELEPORT_COOLDOWN = 3000;
    private static final double DEATH_HEIGHT = -16.0;
    private static final double TELEPORT_BUFFER = 10.0;

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null) return;

        boolean inPVP = arenaManager.isInPVPMode(player);
        boolean inArenaXZ = isInArenaXZ(player);

        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();

        if (inPVP && to.getY() < DEATH_HEIGHT) {
            player.setHealth(0.0);
            return;
        }

        // --- Для обычных игроков (не в PVP) ---
        if (!inPVP && inArenaXZ) {
            if (player.hasPermission("pvparena.bypass")) return;

            double centerX = (arenaManager.getCorner1().getX() + arenaManager.getCorner2().getX()) / 2;
            double centerZ = (arenaManager.getCorner1().getZ() + arenaManager.getCorner2().getZ()) / 2;

            Vector push = player.getLocation().toVector()
                    .subtract(new Vector(centerX, player.getLocation().getY(), centerZ))
                    .setY(0)
                    .normalize()
                    .multiply(2.5);

            player.setVelocity(push);

            // Отправляем сообщение не чаще 1 раза каждые 2 секунды
            if (!lastMessageTime.containsKey(uuid) || now - lastMessageTime.get(uuid) > MESSAGE_COOLDOWN) {
                player.sendMessage("§cВы не можете заходить в PVP арену!");
                lastMessageTime.put(uuid, now);
            }
            return;
        }

        // --- Для игроков в PVP ---
        if (inPVP) {
            // Проверяем, не ушел ли игрок слишком далеко от арены
            if (arenaManager.isPlayerOutsideArenaWithBuffer(player, TELEPORT_BUFFER)) {
                handlePlayerTooFar(player, now, uuid);
                return;
            }

            // Если игрок просто вышел за границы (но еще в буферной зоне) - отталкиваем
            if (!inArenaXZ) {
                double centerX = (arenaManager.getCorner1().getX() + arenaManager.getCorner2().getX()) / 2;
                double centerZ = (arenaManager.getCorner1().getZ() + arenaManager.getCorner2().getZ()) / 2;

                Vector push = new Vector(centerX, player.getLocation().getY(), centerZ)
                        .subtract(player.getLocation().toVector())
                        .setY(0)
                        .normalize()
                        .multiply(2.0);

                player.setVelocity(push);

                if (!lastMessageTime.containsKey(uuid) || now - lastMessageTime.get(uuid) > MESSAGE_COOLDOWN) {
                    player.sendMessage("§cВы не можете покинуть арену пока находитесь в PVP!");
                    lastMessageTime.put(uuid, now);
                }
            }
        }
    }

    private void handlePlayerTooFar(Player player, long currentTime, UUID uuid) {
        if (lastTeleportTime.containsKey(uuid) &&
                currentTime - lastTeleportTime.get(uuid) < TELEPORT_COOLDOWN) {
            return;
        }
        arenaManager.teleportPlayerToRandomLocation(player);
        lastTeleportTime.put(uuid, currentTime);
    }

    // Проверка арены только по X и Z (игнорируем Y)
    private boolean isInArenaXZ(Player player) {
        Location loc = player.getLocation();

        double minX = Math.min(arenaManager.getCorner1().getX(), arenaManager.getCorner2().getX());
        double maxX = Math.max(arenaManager.getCorner1().getX(), arenaManager.getCorner2().getX());
        double minZ = Math.min(arenaManager.getCorner1().getZ(), arenaManager.getCorner2().getZ());
        double maxZ = Math.max(arenaManager.getCorner1().getZ(), arenaManager.getCorner2().getZ());

        return loc.getX() >= minX && loc.getX() <= maxX &&
                loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }
}

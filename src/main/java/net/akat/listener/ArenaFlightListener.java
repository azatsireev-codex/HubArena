package net.akat.listener;

import net.akat.Main;
import net.akat.manager.ArenaManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

public class ArenaFlightListener implements Listener {

    private final ArenaManager arenaManager = Main.getInstance().getArenaManager();

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        if (!shouldBlockFlight(player)) {
            return;
        }

        event.setCancelled(true);
        disableFlight(player);
        player.sendMessage("§cНа арене полёт запрещён.");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        enforceFlightRestriction(event.getPlayer());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        enforceFlightRestriction(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        enforceFlightRestriction(event.getPlayer());
    }

    private void enforceFlightRestriction(Player player) {
        if (!shouldBlockFlight(player)) {
            return;
        }

        disableFlight(player);
    }

    private boolean shouldBlockFlight(Player player) {
        return arenaManager.isInPVPMode(player) || arenaManager.isPlayerInArenaXZ(player);
    }

    private void disableFlight(Player player) {
        if (player.isFlying()) {
            player.setFlying(false);
        }
        if (player.getAllowFlight()) {
            player.setAllowFlight(false);
        }
    }
}

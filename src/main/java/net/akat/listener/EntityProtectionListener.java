package net.akat.listener;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class EntityProtectionListener implements Listener {

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player player) {
            // Разрешаем только тем, у кого есть akat.build
            if (!player.hasPermission("akat.build")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("akat.build")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (player.hasPermission("akat.build")) return; // билдеры могут ломать

        Entity entity = event.getEntity();

        if (isProtectedEntity(entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("akat.build")) return;

        Entity entity = event.getRightClicked();
        if (isProtectedEntity(entity)) {
            event.setCancelled(true);
        }
    }

    private boolean isProtectedEntity(Entity entity) {
        return entity instanceof ItemFrame ||
                entity instanceof GlowItemFrame ||
                entity instanceof Painting ||
                entity instanceof ArmorStand ||
                entity instanceof Minecart ||
                entity instanceof Boat ||
                entity instanceof Hanging;
    }
}

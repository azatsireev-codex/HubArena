package net.akat.listener;

import net.akat.Main;
import net.akat.manager.ArenaManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class PvPArenaListener implements Listener {

    private final ArenaManager arenaManager = Main.getInstance().getArenaManager();

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player && event.getEntity() instanceof Player)) return;

        Player damager = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        boolean damagerInPVP = arenaManager.isInPVPMode(damager);
        boolean victimInPVP = arenaManager.isInPVPMode(victim);

        boolean damagerInArena = arenaManager.isPlayerInArena(damager);
        boolean victimInArena = arenaManager.isPlayerInArena(victim);

        // Если оба в PVP и в арене — разрешаем урон
        if (damagerInPVP && victimInPVP && damagerInArena && victimInArena) {
            event.setCancelled(false);
            arenaManager.setLastHit(victim, damager);
        } else {
            event.setCancelled(true);
        }
    }

    // Любой урон игроку разрешён, если он в PVP на арене
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Если игрок в PVP режиме и на арене - разрешаем весь урон
        if (arenaManager.isInPVPMode(player) && arenaManager.isPlayerInArena(player)) {
            event.setCancelled(false);
        } else {
            // Если игрок НЕ в PVP режиме - запрещаем урон (кроме суицида)
            if (event.getCause() != EntityDamageEvent.DamageCause.SUICIDE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (arenaManager.isInPVPMode(player) && arenaManager.isPlayerInArena(player)) {
            event.setCancelled(false);
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("akat.build") || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        if (arenaManager.isInPVPMode(player) && arenaManager.isPlayerInArena(player)) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                // Запрещаем открытие контейнеров
                Block clickedBlock = event.getClickedBlock();
                if (clickedBlock != null) {
                    // Используем теги для проверки контейнеров
                    if (clickedBlock.getType().isInteractable()) {
                        event.setCancelled(true);
                        return;
                    }
                }

                // Разрешаем правый клик по другим блокам
                return;
            }

            // Разрешаем правый клик в воздух
            if (event.getAction() == Action.RIGHT_CLICK_AIR) {
                return;
            }
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("akat.build") || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        if (arenaManager.isInPVPMode(player) && arenaManager.isPlayerInArena(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("akat.build") || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        if (arenaManager.isInPVPMode(player) && arenaManager.isPlayerInArena(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (arenaManager.isInPVPMode(player) && !arenaManager.isPlayerInArena(player)) {
            event.setCancelled(true);
            player.sendMessage("§cВы не можете выбрасывать предметы в PVP-режиме вне арены!");
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("akat.build") || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPotionDrink(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() == Material.POTION) {
            event.setReplacement(new ItemStack(Material.AIR));
        }
        if (item.getType() == Material.MILK_BUCKET) {
            event.setReplacement(new ItemStack(Material.AIR));
        }
    }
}

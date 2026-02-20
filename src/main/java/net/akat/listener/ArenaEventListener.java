package net.akat.listener;

import net.akat.Main;
import net.akat.api.AkatPointaucAPI;
import net.akat.manager.ArenaManager;
import net.akat.shop.ShopManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ArenaEventListener implements Listener {

    private final ArenaManager arenaManager = Main.getInstance().getArenaManager();
    private final ShopManager shopManager = Main.getInstance().getShopManager();

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        Player victim = event.getEntity();

        boolean isPVPDeath = arenaManager.isInPVPMode(victim) ||
                (killer != null && arenaManager.isInPVPMode(killer));

        event.setDeathMessage(null);

        // --- УБИЙЦА ---
        if (killer != null) {
            Main.getInstance().getRatingManager().addKill(killer);

            int kills = Main.getInstance().getRatingManager().getKills(killer);

            Main.getInstance().getRatingManager().updateRating(killer, true);
            showRatingInActionBar(killer);

            Main.getInstance().getRewardManager().giveNextReward(killer);

            int rewardAmount = arenaManager.calculateKillReward(killer, victim);
            addPoints(killer, rewardAmount);
            arenaManager.playKillSound(killer);
            killer.setHealth(killer.getMaxHealth());

            if (isPVPDeath) {
                arenaManager.sendCustomDeathMessage(killer, victim);
            }
        }

        // --- ЖЕРТВА ---
        Main.getInstance().getArenaManager().markDiedInCombat(victim);
        Main.getInstance().getRatingManager().updateRating(victim, false);
        showRatingInActionBar(victim);

        arenaManager.dropApplesAndCarrots(victim);
        event.getDrops().clear();

        event.setDroppedExp(0);
        event.setKeepLevel(true);

        // Полностью очищаем инвентарь
        victim.closeInventory();
        victim.getInventory().clear();
        victim.getInventory().setArmorContents(null);
        victim.getInventory().setItemInOffHand(null);

        // Сбрасываем убийства и этап выдачи наград
        Main.getInstance().getRatingManager().resetKills(victim);
        Main.getInstance().getRewardManager().resetPlayerProgress(victim);

        // Уход из PVP
        if (arenaManager.isInPVPMode(victim)) {
            arenaManager.exitPVP(victim);
            victim.sendMessage("§cВы покидаете PVP-режим после смерти.");
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (arenaManager.isInPVPMode(player)) {
            arenaManager.exitPVP(player);
            shopManager.clear(player);
        }
    }

    // Показ рейтинга
    private void showRatingInActionBar(Player player) {
        int rating = Main.getInstance().getRatingManager().getPlayerRating(player);
        player.sendActionBar("Ваш текущий рейтинг: " + rating);
    }

    // Метод для добавления кубиславов через API
    private void addPoints(Player player, int amount) {
        AkatPointaucAPI api = Main.getInstance().getAkatAPI();
        if (api != null) {
            api.addPoints(player.getUniqueId(), amount);
        }
    }
}

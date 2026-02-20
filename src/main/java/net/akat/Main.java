package net.akat;

import net.akat.api.AkatPointaucAPI;
import net.akat.api.http.BalanceHttpClient;
import net.akat.command.FlyCommand;
import net.akat.command.PVPCommand;
import net.akat.command.ReloadConfigCommand;
import net.akat.listener.*;
import net.akat.manager.ArenaManager;
import net.akat.manager.CurrencyManager;
import net.akat.manager.PlayerRatingManager;
import net.akat.manager.RewardManager;
import net.akat.shop.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;
    private ArenaManager arenaManager;
    private CurrencyManager currencyManager;
    private PlayerRatingManager ratingManager;
    private RewardManager rewardManager;
    private ShopManager shopManager;

    private AkatPointaucAPI akatAPI;
    private BalanceHttpClient client;

    @Override
    public void onEnable() {
        instance = this;
        this.client = new BalanceHttpClient("http://localhost:32555");

        RegisteredServiceProvider<AkatPointaucAPI> rsp =
                Bukkit.getServicesManager().getRegistration(AkatPointaucAPI.class);

        if (rsp != null) {
            akatAPI = rsp.getProvider();
            getLogger().info("✅ AkatPointaucAPI найден и подключен!");
        } else {
            getLogger().warning("⚠ AkatPointaucAPI не найден! Баланс кубиславов недоступен!");
        }

        saveDefaultConfig();
        saveResource("rewards.yml", false);
        saveResource("shop.yml", false);

        ratingManager = new PlayerRatingManager(this);
        shopManager = new ShopManager(this);
        arenaManager = new ArenaManager();
        currencyManager = CurrencyManager.getInstance();
        rewardManager = new RewardManager();

        getCommand("pvp").setExecutor(new PVPCommand());
        getCommand("fly").setExecutor(new FlyCommand());
        getCommand("reloadconfig").setExecutor(new ReloadConfigCommand());

        getServer().getPluginManager().registerEvents(new ArenaEventListener(), this);
        getServer().getPluginManager().registerEvents(new PvPArenaListener(), this);
        getServer().getPluginManager().registerEvents(new ArenaMoveListener(), this);
        getServer().getPluginManager().registerEvents(new EntityProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new PVPCommandBlocker(), this);

    }

    @Override
    public void onDisable() {
        if (arenaManager != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (arenaManager.isInPVPMode(player)) {
                    arenaManager.exitPVP(player);
                    player.sendMessage("§cСервер выключается — вы выведены из PVP-режима.");
                }
            }
            arenaManager.removeDisplays();
        }

        if (ratingManager != null) {
            ratingManager.forceSave();
        }

        instance = null;
    }

    public static Main getInstance() {
        return instance;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }

    public PlayerRatingManager getRatingManager() {
        return ratingManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public AkatPointaucAPI getAkatAPI() {
        return akatAPI;
    }

    public BalanceHttpClient getBalanceHttpClient() {
        return client;
    }
}
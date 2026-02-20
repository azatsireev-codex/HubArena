package net.akat.manager;

import net.akat.Main;
import net.akat.api.AkatPointaucAPI;
import org.bukkit.entity.Player;

public class CurrencyManager {

    private static CurrencyManager instance;

    // Заглушка для второй валюты
    public enum CurrencyType { CUBISLAV, SECOND }

    private CurrencyManager() {}

    public static CurrencyManager getInstance() {
        if (instance == null) {
            instance = new CurrencyManager();
        }
        return instance;
    }

    private AkatPointaucAPI getAPI() {
        return Main.getInstance().getAkatAPI();
    }

    // ===== Методы работы с кубиславами через AkatPointauc =====
    public int getCubislav(Player player) {
        AkatPointaucAPI api = getAPI();
        if (api == null) return 0; // если API недоступен
        return api.getPoints(player.getUniqueId());
    }

    public boolean hasSufficientFunds(Player player, int amount, CurrencyType currency) {
        if (currency == CurrencyType.SECOND) return false; // заглушка

        int balance = getCubislav(player);
        return balance >= amount;
    }

    public void subtractFunds(Player player, int amount, CurrencyType currency) {
        if (currency == CurrencyType.SECOND) return; // заглушка

        AkatPointaucAPI api = getAPI();
        if (api != null) {
            api.removePoints(player.getUniqueId(), amount);
        }
    }

    // ===== Перегруженные методы для совместимости с кодом магазина =====
    public boolean hasSufficientFunds(Player player, int amount) {
        return hasSufficientFunds(player, amount, CurrencyType.CUBISLAV);
    }

    public void subtractFunds(Player player, int amount) {
        subtractFunds(player, amount, CurrencyType.CUBISLAV);
    }

    // ===== Методы для второй валюты (пока заглушка) =====
    public boolean hasSecondCurrency(Player player, int amount) {
        return hasSufficientFunds(player, amount, CurrencyType.SECOND);
    }

    public void subtractSecondCurrency(Player player, int amount) {
        subtractFunds(player, amount, CurrencyType.SECOND);
    }
}

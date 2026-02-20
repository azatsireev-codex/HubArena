package net.akat.command;

import net.akat.Main;
import net.akat.shop.PVPShopMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PVPCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эту команду может выполнить только игрок.");
            return false;
        }

        Player player = (Player) sender;

        // Если нет аргументов - открываем магазин
        if (args.length == 0) {
            if (!player.hasPermission("pvparena.shop")) {
                player.sendMessage("§cУ вас нет прав для открытия магазина!");
                return false;
            }

            new PVPShopMenu(Main.getInstance(), Main.getInstance().getBalanceHttpClient()).open(player);
            return true;
        }

        // Обработка аргументов
        switch (args[0].toLowerCase()) {
            case "join":
                return handleJoin(player);
            case "exit":
                return handleExit(player);
            default:
                player.sendMessage("§cНеизвестная команда. Используйте:");
                player.sendMessage("§6/pvp §f- открыть магазин");
                player.sendMessage("§6/pvp join §f- войти в PVP режим");
                player.sendMessage("§6/pvp exit §f- выйти из PVP режима");
                return false;
        }
    }

    private boolean handleJoin(Player player) {
        if (Main.getInstance().getArenaManager().isInPVPMode(player)) {
            player.sendMessage("§cВы уже находитесь в PVP режиме!");
            player.sendMessage("§eИспользуйте §6/pvp exit§e для выхода.");
            return false;
        }

        if (!player.hasPermission("pvparena.enterpvp")) {
            player.sendMessage("§cУ вас нет прав для входа в PVP арену.");
            return false;
        }

        if (!player.hasPermission("pvparena.free")) {
            if (!Main.getInstance().getCurrencyManager().hasSufficientFunds(player, 50)) {
                player.sendMessage("§cУ вас недостаточно кубислав для входа в PVP режим.");
                return false;
            }

            Main.getInstance().getCurrencyManager().subtractFunds(player, 50);
        }

        // Вход в PVP
        Main.getInstance().getArenaManager().enterPVP(player);

        // Телепортируем игрока на случайную точку арены
        Main.getInstance().getArenaManager().teleportPlayerToRandomLocation(player);
        player.sendMessage("§aВы вошли в PVP арену!");

        return true;
    }

    private boolean handleExit(Player player) {
        if (Main.getInstance().getArenaManager().isInPVPMode(player)) {
            Main.getInstance().getArenaManager().exitPVP(player);
        } else {
            player.sendMessage("§eВы не находитесь в PVP режиме.");
        }
        return true;
    }
}

package net.akat.command;

import net.akat.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReloadConfigCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эту команду может выполнить только игрок.");
            return false;
        }

        // Проверка прав доступа
        Player player = (Player) sender;
        if (!player.hasPermission("pvparena.reloadconfig")) {
            player.sendMessage("У вас нет прав на выполнение этой команды.");
            return false;
        }

        // Перезагрузка конфигурации
        Main.getInstance().getArenaManager().reloadConfig();
        Main.getInstance().getRewardManager().loadRewards();
        player.sendMessage("Конфигурация была перезагружена.");

        return true;
    }
}

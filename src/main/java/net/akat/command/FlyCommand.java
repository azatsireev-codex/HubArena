package net.akat.command;

import net.akat.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Эту команду может выполнить только игрок.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("donate.fly")) {
            player.sendMessage("§cУ вас нет прав на использование /fly.");
            return true;
        }

        boolean inPvpMode = Main.getInstance().getArenaManager().isInPVPMode(player);
        boolean inArena = Main.getInstance().getArenaManager().isPlayerInArenaXZ(player);

        if (inPvpMode || inArena) {
            player.sendMessage("§cНа PVP арене использовать /fly нельзя.");
            return true;
        }

        boolean enableFlight = !player.getAllowFlight();
        player.setAllowFlight(enableFlight);

        if (!enableFlight) {
            player.setFlying(false);
            player.sendMessage("§eРежим полёта выключен.");
            return true;
        }

        player.sendMessage("§aРежим полёта включен.");
        return true;
    }
}

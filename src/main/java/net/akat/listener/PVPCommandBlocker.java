package net.akat.listener;

import net.akat.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Set;

public class PVPCommandBlocker implements Listener {

    private final Set<String> blockedCommands = Set.of(
            "spawn", "home"
    );

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (!Main.getInstance().getArenaManager().isInPVPMode(player)) {
            return;
        }

        String message = event.getMessage().toLowerCase();
        String command = message.substring(1).split(" ")[0];

        if (blockedCommands.contains(command)) {
            event.setCancelled(true);
            player.sendMessage("§cЭта команда недоступна в PVP режиме!");
            player.sendMessage("§eИспользуйте §6/pvp exit§e для выхода из PVP.");
        }
    }
}

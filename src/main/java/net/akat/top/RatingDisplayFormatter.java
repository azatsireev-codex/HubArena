package net.akat.top;

import net.kyori.adventure.text.Component;

import java.util.List;

public class RatingDisplayFormatter implements DisplayFormatter {

    @Override
    public Component formatTopPlayers(List<String> topPlayers) {
        StringBuilder displayText = new StringBuilder();
        displayText.append("§6🏆 Топ 10 Игроков §6🏆\n\n");

        for (int i = 0; i < topPlayers.size(); i++) {
            String playerData = topPlayers.get(i);
            String[] parts = playerData.split(":");
            if (parts.length == 2) {
                String playerName = parts[0];
                int rating = Integer.parseInt(parts[1]);

                String positionColor = getPositionColor(i + 1);
                displayText.append(positionColor)
                        .append("#").append(i + 1)
                        .append(" §f").append(playerName)
                        .append(" §7- §a").append(rating)
                        .append(" очков\n");
            }
        }

        return Component.text(displayText.toString());
    }

    @Override
    public Component getEmptyMessage() {
        return Component.text("§7Нет данных о рейтинге");
    }

    private String getPositionColor(int position) {
        switch (position) {
            case 1: return "§6";
            case 2: return "§7";
            case 3: return "§c";
            default: return "§e";
        }
    }
}

package net.akat.top;

import net.kyori.adventure.text.Component;

import java.util.List;

public interface DisplayFormatter {
    Component formatTopPlayers(List<String> topPlayers);
    Component getEmptyMessage();
}

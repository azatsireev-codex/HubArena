package net.akat.shop;

import net.akat.Main;
import net.akat.api.http.BalanceHttpClient;
import net.akat.api.spigui.SpiGUI;
import net.akat.api.spigui.buttons.SGButton;
import net.akat.api.spigui.menu.SGMenu;
import net.akat.manager.ArenaManager;
import net.akat.manager.CurrencyManager;
import net.akat.util.ClickLimiter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.stream.Collectors;

public class PVPShopMenu {

    private final SpiGUI gui;
    private final ShopManager shopManager;
    private final CurrencyManager currencyManager;
    private final ArenaManager arenaManager;
    private final BalanceHttpClient coinsHttpClient;
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public PVPShopMenu(JavaPlugin plugin, BalanceHttpClient coinsHttpClient) {
        this.gui = new SpiGUI(plugin);
        this.shopManager = Main.getInstance().getShopManager();
        this.currencyManager = Main.getInstance().getCurrencyManager();
        this.arenaManager = Main.getInstance().getArenaManager();
        this.coinsHttpClient = coinsHttpClient;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        boolean inPVP = arenaManager.isInPVPMode(player);
        List<ShopItem> items = shopManager.getItems();
        Map<ShopItem, Integer> pendingItems = shopManager.getPendingItemsWithCount(player);

        int coinsBalance = coinsHttpClient.getBalanceInfo(player.getName()).getBalance();

        // Динамическое название меню в зависимости от режима PVP
        String menuTitle = inPVP ? "§f\uE006\uE032" : "§f\uE006\uE031";
        SGMenu menu = gui.create(menuTitle, 6);
        playerPages.put(player.getUniqueId(), page);

        final int currentPage = page;
        final Player currentPlayer = player;

        // ВСЕГДА добавляем информацию о покупках (даже если список пустой)
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta meta = infoItem.getItemMeta();
        meta.setDisplayName("§aВаши покупки для PVP");

        List<String> lore = new ArrayList<>();
        lore.add("§7Следующие предметы будут");
        lore.add("§7выданы при входе в PVP:");
        lore.add("");

        if (!pendingItems.isEmpty()) {
            for (Map.Entry<ShopItem, Integer> entry : pendingItems.entrySet()) {
                if (entry.getValue() > 1) {
                    lore.add("§e• " + entry.getKey().getName() + " §7(x" + entry.getValue() + ")");
                } else {
                    lore.add("§e• " + entry.getKey().getName());
                }
            }
            lore.add("");
            lore.add("§c⚠ Они пропадут если выйти с сервера!");
        } else {
            lore.add("§8Пока нет покупок");
            lore.add("§8Купите предметы в магазине,");
            lore.add("§8чтобы они появились здесь");
        }

        meta.setLore(lore);
        NamespacedKey modelKey = new NamespacedKey("minecraft", "null");
        meta.setItemModel(modelKey);
        infoItem.setItemMeta(meta);

        SGButton infoButton = new SGButton(infoItem).withListener(ClickLimiter.wrapWithLimit(e -> {
            e.setCancelled(true);
            currentPlayer.sendMessage("§aВаши pending предметы сохранены!");
        }));

        menu.setButton(47, infoButton); // Первая книга
        menu.setButton(48, infoButton); // Дубликат книги рядом

        // Динамические кнопки для PVP/выхода из PVP
        ItemStack pvpButtonItem;
        if (inPVP) {
            // Кнопка выхода из PVP
            pvpButtonItem = new ItemStack(Material.OAK_DOOR);
            ItemMeta exitMeta = pvpButtonItem.getItemMeta();
            exitMeta.setDisplayName("§c🚪 Выйти из PVP");

            List<String> exitLore = new ArrayList<>();
            exitLore.add("");
            exitLore.add("§a✅ Вы в PVP режиме");
            exitLore.add("§cНажмите чтобы выйти!");

            exitMeta.setLore(exitLore);
            exitMeta.setItemModel(modelKey);
            pvpButtonItem.setItemMeta(exitMeta);
        } else {
            // Кнопка входа в PVP
            pvpButtonItem = new ItemStack(Material.DIAMOND_SWORD);
            ItemMeta pvpMeta = pvpButtonItem.getItemMeta();
            pvpMeta.setDisplayName("§c⚔ Войти в PVP");

            List<String> pvpLore = new ArrayList<>();
            pvpLore.add("");
            pvpLore.add("§e⚠ Вы в лобби");
            pvpLore.add("§eНажмите чтобы войти в PVP!");
            pvpLore.add("");
            //pvpLore.add("§6Стоимость входа: §e50 кубислав");

            pvpMeta.setLore(pvpLore);
            pvpMeta.setItemModel(modelKey);
            pvpButtonItem.setItemMeta(pvpMeta);
        }

        // Создаем кнопку для входа/выхода из PVP
        SGButton pvpButton = new SGButton(pvpButtonItem).withListener(ClickLimiter.wrapWithLimit(e -> {
            e.setCancelled(true);
            Player p = (Player) e.getWhoClicked();

            boolean playerInPVP = arenaManager.isInPVPMode(p);

            if (playerInPVP) {
                // Выход из PVP
                p.closeInventory();
                p.performCommand("pvp exit");
                p.sendMessage("§cВыход из PVP режима...");
            } else {
                // Вход в PVP
                if (arenaManager.isInPVPMode(p)) {
                    p.sendMessage("§aВы уже в PVP режиме!");
                    return;
                }
                p.closeInventory();
                p.performCommand("pvp join");
                p.sendMessage("§aПодключение к PVP режиму...");
            }
        }));

        // Заполняем слоты 44, 43, 42 одинаковыми кнопками
        menu.setButton(42, pvpButton);
        menu.setButton(43, pvpButton);
        menu.setButton(44, pvpButton);

        // Фильтруем предметы по доступности
        List<ShopItem> availableItems = items.stream()
                .filter(item -> {
                    if (item.getAvailability() == ShopAvailability.ONLY_PVP && !inPVP) return false;
                    if (item.getAvailability() == ShopAvailability.ONLY_LOBBY && inPVP) return false;
                    return true;
                })
                .collect(Collectors.toList());

        // НАСТРОЙКИ ПАГИНАЦИИ: 30 товаров на страницу, 3 свободные колонки справа
        int itemsPerRow = 6; // 9 слотов в строке минус 3 свободных = 6 товаров
        int rowsForItems = 5; // 5 строк для товаров (с 0 по 4 строки)
        int itemsPerPage = itemsPerRow * rowsForItems; // 6 * 5 = 30 товаров

        int totalPages = (int) Math.ceil((double) availableItems.size() / itemsPerPage);

        if (currentPage >= totalPages && totalPages > 0) {
            playerPages.put(currentPlayer.getUniqueId(), totalPages - 1);
            open(currentPlayer, totalPages - 1);
            return;
        }

        // Добавляем предметы на текущую страницу
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, availableItems.size());
        int slot = 0;

        for (int i = startIndex; i < endIndex; i++) {
            ShopItem item = availableItems.get(i);

            // Вычисляем позицию в GUI с учетом свободных колонок
            int row = (slot / itemsPerRow); // строка (0-4)
            int column = (slot % itemsPerRow); // колонка (0-5)
            int guiSlot = row * 9 + column; // преобразуем в слот GUI

            // Используем createItemStack() для отображения в магазине
            ItemStack stack = item.createItemStack();
            ItemMeta metaItem = stack.getItemMeta();

            // Обновляем лор для отображения в магазине
            List<String> loreItem = new ArrayList<>();
            if (metaItem.getLore() != null) {
                loreItem.addAll(metaItem.getLore());
            }

            // Показываем количество уже купленных предметов
            int purchasedCount = pendingItems.getOrDefault(item, 0);
            if (purchasedCount > 0) {
                loreItem.add("");
                loreItem.add("§a✓ Куплено: §e" + purchasedCount + " шт.");
            }

            loreItem.add("");
            if (item.getPrice1() > 0 && item.getPrice2() > 0) {
                loreItem.add("§6Цены:");
                loreItem.add("§e• " + item.getPrice1() + " Кубислав");
                loreItem.add("§6• " + item.getPrice2() + " Нефткоинов");
                loreItem.add("");
                loreItem.add("§a🛒 ЛКМ - Купить за Кубислав");
                loreItem.add("§6🛒 ПКМ - Купить за Нефткоины");
            } else if (item.getPrice1() > 0) {
                loreItem.add("§6Цена: §e" + item.getPrice1() + " Кубислав");
                loreItem.add("");
                loreItem.add("§a🛒 ЛКМ - Купить " + item.getAmount() + " шт.");
            } else if (item.getPrice2() > 0) {
                loreItem.add("§6Цена: §6" + item.getPrice2() + " Нефткоинов");
                loreItem.add("");
                loreItem.add("§6🛒 ПКМ - Купить " + item.getAmount() + " шт.");
            }

            metaItem.setLore(loreItem);
            stack.setItemMeta(metaItem);

            menu.setButton(guiSlot, new SGButton(stack).withListener(ClickLimiter.wrapWithLimit(e -> {
                e.setCancelled(true);
                Player p = (Player) e.getWhoClicked();

                if (e.getClick().isLeftClick() && item.getPrice1() > 0) {
                    // Покупка за первую валюту (Кубислав)
                    if (!currencyManager.hasSufficientFunds(p, (int)item.getPrice1())) {
                        p.sendMessage("§cНедостаточно кубислав!");
                        return;
                    }
                    currencyManager.subtractFunds(p, (int)item.getPrice1());
                    if (inPVP) {
                        giveNow(p, item);
                    } else {
                        shopManager.addPending(p, item);
                        int newCount = shopManager.getPendingItemsWithCount(p).getOrDefault(item, 0);
                        p.sendMessage("§a" + item.getName() + " §fбудет выдан при входе в PVP!");
                        p.sendMessage("§eТеперь у вас " + newCount + " шт. этого предмета в ожидании");
                        p.closeInventory();
                        open(p, playerPages.getOrDefault(p.getUniqueId(), 0));
                    }

                } else if (e.getClick().isRightClick() && item.getPrice2() > 0) {
                    // Покупка за вторую валюту (Коины)
                    BalanceHttpClient.BalanceInfo balanceInfo = coinsHttpClient.getBalanceInfo(p.getName());
                    if (balanceInfo.getBalance() < item.getPrice2()) {
                        p.sendMessage("§cНедостаточно коинов! У вас: §e" + balanceInfo.getBalance());
                        return;
                    }

                    if (coinsHttpClient.withdraw(p, item.getPrice2())) {
                        if (inPVP) {
                            giveNow(p, item);
                        } else {
                            shopManager.addPending(p, item);
                            int newCount = shopManager.getPendingItemsWithCount(p).getOrDefault(item, 0);
                            p.sendMessage("§a" + item.getName() + " §fбудет выдан при входе в PVP!");
                            p.sendMessage("§eТеперь у вас " + newCount + " шт. этого предмета в ожидании");
                            p.closeInventory();
                            open(p, playerPages.getOrDefault(p.getUniqueId(), 0));
                        }
                    } else {
                        p.sendMessage("§cОшибка при списании коинов! Обратитесь к администрации!");
                    }
                } else {
                    p.sendMessage("§cЭтот способ покупки недоступен для данного предмета!");
                }
            })));
            slot++;
        }

        // Добавляем кнопки пагинации если нужно
        if (totalPages > 1) {
            if (currentPage > 0) {
                ItemStack backButton = new ItemStack(Material.ARROW);
                ItemMeta backMeta = backButton.getItemMeta();
                backMeta.setDisplayName("§6Предыдущая страница");
                backMeta.setItemModel(modelKey);
                backButton.setItemMeta(backMeta);
                menu.setButton(45, new SGButton(backButton).withListener(ClickLimiter.wrapWithLimit(e -> {
                    e.setCancelled(true);
                    open(currentPlayer, currentPage - 1);
                })));
            }

            if (currentPage < totalPages - 1) {
                ItemStack nextButton = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextButton.getItemMeta();
                nextMeta.setDisplayName("§6Следующая страница");
                nextMeta.setItemModel(modelKey);
                nextButton.setItemMeta(nextMeta);
                menu.setButton(53, new SGButton(nextButton).withListener(ClickLimiter.wrapWithLimit(e -> {
                    e.setCancelled(true);
                    open(currentPlayer, currentPage + 1);
                })));
            }
        }

        currentPlayer.openInventory(menu.getInventory());
    }

    private void giveNow(Player player, ShopItem item) {
        // Используем createItemStack() вместо простого ItemStack
        ItemStack itemStack = item.createItemStack();
        player.getInventory().addItem(itemStack);
        player.sendMessage("§aВы купили: §e" + item.getName());

        // Если это зелье или стрела с эффектами, показываем дополнительную информацию
        if (!item.getPotionEffects().isEmpty()) {
            StringBuilder effects = new StringBuilder();
            for (Map.Entry<PotionEffectType, Integer> effect : item.getPotionEffects().entrySet()) {
                if (effects.length() > 0) effects.append(", ");
                effects.append(getEffectDisplayName(effect.getKey())).append(" ").append(effect.getValue());
            }
            player.sendMessage("§bЭффекты: §f" + effects.toString());
        }

        // Если есть зачарования, показываем их
        if (!item.getEnchants().isEmpty()) {
            StringBuilder enchants = new StringBuilder();
            for (Map.Entry<Enchantment, Integer> enchant : item.getEnchants().entrySet()) {
                if (enchants.length() > 0) enchants.append(", ");
                enchants.append(getEnchantDisplayName(enchant.getKey())).append(" ").append(enchant.getValue());
            }
            player.sendMessage("§9Зачарования: §f" + enchants.toString());
        }
    }

    // Вспомогательные методы для красивого отображения названий
    private String getEffectDisplayName(PotionEffectType effect) {
        switch (effect.getName().toLowerCase()) {
            case "speed": return "Скорость";
            case "slowness": return "Замедление";
            case "increase_damage": return "Сила";
            case "heal": return "Лечение";
            case "harm": return "Урон";
            case "jump": return "Прыжок";
            case "regeneration": return "Регенерация";
            case "fire_resistance": return "Огнестойкость";
            case "water_breathing": return "Подводное дыхание";
            case "invisibility": return "Невидимость";
            case "night_vision": return "Ночное зрение";
            case "weakness": return "Слабость";
            case "poison": return "Отравление";
            default: return effect.getName();
        }
    }

    private String getEnchantDisplayName(Enchantment enchant) {
        switch (enchant.getKey().getKey().toLowerCase()) {
            case "protection": return "Защита";
            case "fire_protection": return "Огнезащита";
            case "feather_falling": return "Невесомость";
            case "blast_protection": return "Взрывозащита";
            case "projectile_protection": return "Защита от снарядов";
            case "sharpness": return "Острота";
            case "smite": return "Небесная кара";
            case "bane_of_arthropods": return "Бич членистоногих";
            case "knockback": return "Отдача";
            case "fire_aspect": return "Заговор огня";
            case "looting": return "Добыча";
            case "power": return "Сила";
            case "punch": return "Отбрасывание";
            case "flame": return "Пламя";
            case "infinity": return "Бесконечность";
            case "efficiency": return "Эффективность";
            case "unbreaking": return "Прочность";
            case "fortune": return "Удача";
            case "mending": return "Починка";
            default: return enchant.getKey().getKey();
        }
    }
}

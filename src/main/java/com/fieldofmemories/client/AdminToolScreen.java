package com.fieldofmemories.client;

import com.fieldofmemories.network.NetworkHandler;
import com.fieldofmemories.network.HorrorNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AdminToolScreen extends Screen {
    private String page = "home";
    private Button spawnToggleButton;
    private static int memoryReadMask;
    private static boolean memorySpawnEnabled = true;
    private static int memorySpawnChance = 10;
    private static String boxLootData = "";
    private static int selectedLootItem;
    private static int worldDanger;
    private static int horrorActivity = 100;
    private static int scarecrowChance = 10;
    private static int faceChance = 10;
    private static int spiritChance = 2;
    private static int mimicChance = 3;
    private static int treeDiscChance = 3;
    private static int houseCameraChance = 32;

    private static final String[] LOOT_ITEM_IDS = {
            "minecraft:iron_ingot", "minecraft:iron_nugget", "minecraft:gold_ingot",
            "minecraft:carrot", "minecraft:potato", "minecraft:redstone", "minecraft:lapis_lazuli",
            "minecraft:amethyst_shard", "minecraft:copper_ingot", "minecraft:bone", "minecraft:stick",
            "minecraft:spruce_planks", "minecraft:string", "minecraft:feather", "minecraft:leather",
            "minecraft:flint", "minecraft:beef", "minecraft:porkchop", "minecraft:chicken",
            "minecraft:mutton", "minecraft:rabbit", "minecraft:bread", "minecraft:apple",
            "minecraft:coal", "minecraft:torch", "minecraft:emerald", "minecraft:diamond",
            "minecraft:arrow", "minecraft:golden_carrot",
            "fieldofmemories:memory_disc_1", "fieldofmemories:memory_disc_2"
    };
    private static final String[] LOOT_ITEM_NAMES = {
            "Железный слиток", "Железный самородок", "Золотой слиток", "Морковь", "Картофель",
            "Редстоун", "Лазурит", "Осколок аметиста", "Медный слиток", "Кость", "Палка",
            "Еловые доски", "Нить", "Перо", "Кожа", "Кремень", "Сырая говядина",
            "Сырая свинина", "Сырая курятина", "Сырая баранина", "Сырая крольчатина",
            "Хлеб", "Яблоко", "Уголь", "Факел", "Изумруд", "Алмаз", "Стрела",
            "Золотая морковь", "Диск: Пирог с клубничкой", "Диск: Сторож"
    };

    private static final String[] DISC_NAMES = {
            "Пирог с клубничкой", "Сторож", "Уличная прядь", "Городская выгода",
            "Закрытая дверь", "Отец святой", "Поле", "Вечер"
    };

    public AdminToolScreen() { super(Component.literal("Field of Memories — Admin Tool")); }

    public static void updateHorrorData(int danger, int activity, int scarecrow, int face,
                                        int spirit, int mimic, int treeDisc, int houseCamera) {
        worldDanger = Math.max(0, Math.min(100, danger));
        horrorActivity = Math.max(10, Math.min(300, activity));
        scarecrowChance = Math.max(0, Math.min(100, scarecrow));
        faceChance = Math.max(0, Math.min(100, face));
        spiritChance = Math.max(0, Math.min(100, spirit));
        mimicChance = Math.max(0, Math.min(100, mimic));
        treeDiscChance = Math.max(0, Math.min(100, treeDisc));
        houseCameraChance = Math.max(0, Math.min(100, houseCamera));
    }

    public static void updateBoxLootData(String data) {
        boxLootData = data == null ? "" : data;
    }

    private static String selectedLootDetails() {
        String selected = LOOT_ITEM_IDS[selectedLootItem];
        int totalWeight = 0;
        String[] entries = boxLootData.split(";");
        for (String entry : entries) {
            String[] parts = entry.split(",");
            if (parts.length == 4) {
                try { totalWeight += Integer.parseInt(parts[3]); } catch (NumberFormatException ignored) {}
            }
        }
        for (String entry : entries) {
            String[] parts = entry.split(",");
            if (parts.length == 4 && parts[0].equals(selected)) {
                try {
                    int weight = Integer.parseInt(parts[3]);
                    double chance = totalWeight <= 0 ? 0.0D : 100.0D * weight / totalWeight;
                    return String.format("Шанс за выбор: %.1f%% | мин: %s | макс: %s | вес: %s",
                            chance, parts[1], parts[2], parts[3]);
                } catch (NumberFormatException ignored) {
                    return "Ошибка данных таблицы";
                }
            }
        }
        return "Предмет не добавлен";
    }

    public static void updateMemoryData(int readMask, boolean spawnEnabled, int spawnChance) {
        memoryReadMask = readMask & 0xFF;
        memorySpawnEnabled = spawnEnabled;
        memorySpawnChance = Math.max(0, Math.min(100, spawnChance));
        if (Minecraft.getInstance().screen instanceof AdminToolScreen screen && screen.spawnToggleButton != null) {
            screen.spawnToggleButton.setMessage(Component.literal(
                    memorySpawnEnabled ? "Отключить спавн предмета в мире" : "Включить спавн предмета в мире"));
        }
    }

    @Override
    protected void init() {
        clearWidgets();
        spawnToggleButton = null;
        int cx = width / 2;
        int y = height / 2 - 65;
        if (page.equals("home")) {
            add("События", cx, y, () -> go("events")); y += 25;
            add("Регулировка активности", cx, y, () -> go("activity")); y += 25;
            add("Движение", cx, y, () -> go("movement")); y += 25;
            add("Выносливость", cx, y, () -> go("stamina")); y += 25;
            add("Прыжки", cx, y, () -> go("jumps")); y += 25;
            add("Управление предметами", cx, y, () -> go("items")); y += 25;
            add("Сюжет и статистика", cx, y, () -> go("story")); y += 25;
            add("Туман", cx, y, () -> go("fog"));
        } else if (page.equals("events")) {
            add("Заставить игрока упасть", cx, y, () -> go("players"));
            add("Вызвать скример на игроке", cx, y + 25, () -> go("scream_players"));
            add("Вызвать мимика", cx, y + 50, HorrorNetwork::requestMimic);
            add("Назад", cx, y + 102, () -> go("home"));
        } else if (page.equals("activity")) {
            HorrorNetwork.requestData();
            add("Шанс пугала −", cx - 82, y, () -> HorrorNetwork.adjust(0, -5));
            add("Шанс пугала +", cx + 82, y, () -> HorrorNetwork.adjust(0, 5));
            add("Шанс лица −", cx - 82, y + 25, () -> HorrorNetwork.adjust(1, -5));
            add("Шанс лица +", cx + 82, y + 25, () -> HorrorNetwork.adjust(1, 5));
            add("Шанс духа −", cx - 82, y + 50, () -> HorrorNetwork.adjust(3, -1));
            add("Шанс духа +", cx + 82, y + 50, () -> HorrorNetwork.adjust(3, 1));
            add("Шанс мимика −", cx - 82, y + 75, () -> HorrorNetwork.adjust(4, -1));
            add("Шанс мимика +", cx + 82, y + 75, () -> HorrorNetwork.adjust(4, 1));
            add("Общая активность −", cx - 82, y + 100, () -> HorrorNetwork.adjust(2, -10));
            add("Общая активность +", cx + 82, y + 100, () -> HorrorNetwork.adjust(2, 10));
            add("Обновить данные", cx, y + 127, HorrorNetwork::requestData);
            add("Назад", cx, y + 152, () -> go("home"));
        } else if (page.equals("scream_players")) {
            int row = 0;
            if (Minecraft.getInstance().level != null) {
                for (var player : Minecraft.getInstance().level.players()) {
                    if (row >= 6) break;
                    final var targetId = player.getUUID();
                    add(player.getGameProfile().getName(), cx, y + row * 24, () -> {
                        HorrorNetwork.requestScream(targetId);
                        go("events");
                    });
                    row++;
                }
            }
            add("Назад", cx, y + 6 * 24, () -> go("events"));
        } else if (page.equals("players")) {
            int row = 0;
            if (Minecraft.getInstance().level != null) {
                for (var player : Minecraft.getInstance().level.players()) {
                    if (row >= 6) break;
                    final var targetId = player.getUUID();
                    add(player.getGameProfile().getName(), cx, y + row * 24, () -> {
                        NetworkHandler.sendForceFall(targetId);
                        go("events");
                    });
                    row++;
                }
            }
            add("Назад", cx, y + 6 * 24, () -> go("events"));
        } else if (page.equals("movement")) {
            add("Скорость бега −", cx - 82, y, () -> MovementClient.setAdminSpeed(MovementClient.getAdminSpeed() - 0.02F));
            add("Скорость бега +", cx + 82, y, () -> MovementClient.setAdminSpeed(MovementClient.getAdminSpeed() + 0.02F));
            add("Назад", cx, y + 52, () -> go("home"));
        } else if (page.equals("stamina")) {
            add("Макс. выносливость −", cx - 82, y, () -> MovementClient.setAdminMaxStamina(MovementClient.getAdminMaxStamina() - 10));
            add("Макс. выносливость +", cx + 82, y, () -> MovementClient.setAdminMaxStamina(MovementClient.getAdminMaxStamina() + 10));
            add("Восстановление −", cx - 82, y + 27, () -> MovementClient.setAdminRecovery(MovementClient.getAdminRecovery() - 0.05F));
            add("Восстановление +", cx + 82, y + 27, () -> MovementClient.setAdminRecovery(MovementClient.getAdminRecovery() + 0.05F));
            add("Назад", cx, y + 100, () -> go("home"));
        } else if (page.equals("jumps")) {
            int jumpY = height / 2 - 12;
            add("Дальность −", cx - 82, jumpY, () -> MovementClient.setAdminJump(MovementClient.getAdminJump() - 0.05F));
            add("Дальность +", cx + 82, jumpY, () -> MovementClient.setAdminJump(MovementClient.getAdminJump() + 0.05F));
            add("Откат −", cx - 82, jumpY + 22, () -> MovementClient.setAdminJumpCooldown(MovementClient.getAdminJumpCooldown() - 2));
            add("Откат +", cx + 82, jumpY + 22, () -> MovementClient.setAdminJumpCooldown(MovementClient.getAdminJumpCooldown() + 2));
            add("Длинный прыжок −", cx - 82, jumpY + 44, () -> MovementClient.setAdminLongJumpDistance(MovementClient.getAdminLongJumpDistance() - 0.1F));
            add("Длинный прыжок +", cx + 82, jumpY + 44, () -> MovementClient.setAdminLongJumpDistance(MovementClient.getAdminLongJumpDistance() + 0.1F));
            add("Высота −", cx - 82, jumpY + 66, () -> MovementClient.setAdminLongJumpHeight(MovementClient.getAdminLongJumpHeight() - 0.05F));
            add("Высота +", cx + 82, jumpY + 66, () -> MovementClient.setAdminLongJumpHeight(MovementClient.getAdminLongJumpHeight() + 0.05F));
            add("Цена выносливости −", cx - 82, jumpY + 88, () -> MovementClient.setAdminLongJumpCost(MovementClient.getAdminLongJumpCost() - 2));
            add("Цена выносливости +", cx + 82, jumpY + 88, () -> MovementClient.setAdminLongJumpCost(MovementClient.getAdminLongJumpCost() + 2));
            add("Назад", cx, jumpY + 110, () -> go("home"));
        } else if (page.equals("items")) {
            add("Батарейка", cx, y, () -> HorrorNetwork.giveAdminItem("battery"));
            add("Видеокамера", cx, y + 25, () -> HorrorNetwork.giveAdminItem("video_camera"));
            add("Фотокамера", cx, y + 50, () -> HorrorNetwork.giveAdminItem("old_camera"));
            add("Воспоминания", cx, y + 75, () -> go("memory_items"));
            add("Лут коробок", cx, y + 100, () -> go("box_loot"));
            add("Шансы появления", cx, y + 125, () -> go("item_spawn"));
            add("Назад", cx, y + 155, () -> go("home"));
        } else if (page.equals("item_spawn")) {
            HorrorNetwork.requestData();
            add("Шанс диска на дереве −", cx - 82, y, () -> HorrorNetwork.adjust(5, -1));
            add("Шанс диска на дереве +", cx + 82, y, () -> HorrorNetwork.adjust(5, 1));
            add("Шанс камеры в доме −", cx - 82, y + 35, () -> HorrorNetwork.adjust(6, -1));
            add("Шанс камеры в доме +", cx + 82, y + 35, () -> HorrorNetwork.adjust(6, 1));
            add("Назад", cx, y + 85, () -> go("items"));
        } else if (page.equals("memory_items")) {
            NetworkHandler.requestMemoryDiscStats();
            add("Выдать предмет", cx, y, () -> go("give_memory_disc"));
            spawnToggleButton = add(memorySpawnEnabled ? "Отключить спавн предмета в мире" : "Включить спавн предмета в мире",
                    cx, y + 25, () -> NetworkHandler.sendMemoryDiscAction(1, 0));
            add("Увеличить шанс предмета в мире", cx, y + 50, () -> NetworkHandler.sendMemoryDiscAction(2, 0));
            add("Уменьшить шанс предмета в мире", cx, y + 75, () -> NetworkHandler.sendMemoryDiscAction(3, 0));
            add("Назад", cx, y + 110, () -> go("items"));
        } else if (page.equals("give_memory_disc")) {
            for (int i = 0; i < DISC_NAMES.length; i++) {
                final int discId = i;
                add(DISC_NAMES[i], cx, y + i * 23, () -> {
                    NetworkHandler.sendMemoryDiscAction(0, discId);
                    go("memory_items");
                });
            }
            add("Назад", cx, y + 8 * 23, () -> go("memory_items"));
        } else if (page.equals("box_loot")) {
            NetworkHandler.requestBoxLootTable();
            int lootY = height / 2 - 35;
            add("<", cx - 82, lootY, () -> {
                selectedLootItem = Math.floorMod(selectedLootItem - 1, LOOT_ITEM_IDS.length);
                init();
            });
            add(">", cx + 82, lootY, () -> {
                selectedLootItem = (selectedLootItem + 1) % LOOT_ITEM_IDS.length;
                init();
            });
            add("Добавить предмет", cx, lootY + 25, () -> NetworkHandler.editBoxLoot(LOOT_ITEM_IDS[selectedLootItem], 0));
            add("Убрать предмет", cx, lootY + 50, () -> NetworkHandler.editBoxLoot(LOOT_ITEM_IDS[selectedLootItem], 1));
            add("Вес шанса −", cx - 82, lootY + 78, () -> NetworkHandler.editBoxLoot(LOOT_ITEM_IDS[selectedLootItem], 3));
            add("Вес шанса +", cx + 82, lootY + 78, () -> NetworkHandler.editBoxLoot(LOOT_ITEM_IDS[selectedLootItem], 2));
            add("Мин. кол-во −", cx - 82, lootY + 103, () -> NetworkHandler.editBoxLoot(LOOT_ITEM_IDS[selectedLootItem], 5));
            add("Мин. кол-во +", cx + 82, lootY + 103, () -> NetworkHandler.editBoxLoot(LOOT_ITEM_IDS[selectedLootItem], 4));
            add("Макс. кол-во −", cx - 82, lootY + 128, () -> NetworkHandler.editBoxLoot(LOOT_ITEM_IDS[selectedLootItem], 7));
            add("Макс. кол-во +", cx + 82, lootY + 128, () -> NetworkHandler.editBoxLoot(LOOT_ITEM_IDS[selectedLootItem], 6));
            add("Назад", cx, lootY + 155, () -> go("items"));
        } else if (page.equals("fog")) {
            add("Дальность −", cx - 82, y, () -> NetworkHandler.changeFogDistance(-8));
            add("Дальность +", cx + 82, y, () -> NetworkHandler.changeFogDistance(8));
            add("Назад", cx, y + 52, () -> go("home"));
        } else if (page.equals("story")) {
            NetworkHandler.requestMemoryDiscStats();
            add("Назад", cx, height - 35, () -> go("home"));
        }
    }

    private Button add(String label, int centerX, int y, Runnable action) {
        Button button = Button.builder(Component.literal(label), b -> action.run())
                .bounds(centerX - 90, y, 180, 20).build();
        addRenderableWidget(button);
        return button;
    }

    private void go(String next) { page = next; init(); }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        gui.drawCenteredString(font, Component.literal("АДМИН-ПАНЕЛЬ"), width / 2, height / 2 - 94, 0xFFFF9A38);
        gui.drawCenteredString(font, Component.literal(pageName()), width / 2, height / 2 - 81, 0xFFE0E0E0);
        if (page.equals("events")) {
            drawRightInfo(gui, "Опасность мира: " + worldDanger + " / 100", height / 2 + 18);
            drawRightInfo(gui, "Шанс пугала: " + scarecrowChance + "%", height / 2 + 30);
            drawRightInfo(gui, "Шанс лица: " + faceChance + "%", height / 2 + 42);
        }
        if (page.equals("activity")) {
            drawRightInfo(gui, "Опасность мира: " + worldDanger + " / 100", height / 2 + 2);
            drawRightInfo(gui, "Общая активность: " + horrorActivity + "%", height / 2 + 14);
            drawRightInfo(gui, "Шанс пугала: " + scarecrowChance + "%", height / 2 + 26);
            drawRightInfo(gui, "Шанс лица: " + faceChance + "%", height / 2 + 38);
            drawRightInfo(gui, "Шанс духа: " + spiritChance + "%", height / 2 + 50);
            drawRightInfo(gui, "Шанс мимика: " + mimicChance + "%", height / 2 + 62);
        }
        if (page.equals("item_spawn")) {
            drawRightInfo(gui, "Диск на дереве: " + treeDiscChance + "%", height / 2 + 4);
            drawRightInfo(gui, "Камера в доме: " + houseCameraChance + "%", height / 2 + 16);
        }
        if (page.equals("movement")) drawRightInfo(gui, String.format("Бонус скорости: %.2f", MovementClient.getAdminSpeed()), height / 2 + 12);
        if (page.equals("stamina")) {
            drawRightInfo(gui, "Максимум: " + (int) MovementClient.getAdminMaxStamina(), height / 2 + 2);
            drawRightInfo(gui, String.format("Восстановление: %.2f / тик", MovementClient.getAdminRecovery()), height / 2 + 14);
        }
        if (page.equals("jumps")) {
            drawRightInfo(gui, String.format("Дальность: %.2f | Откат: %d", MovementClient.getAdminJump(), MovementClient.getAdminJumpCooldown()), height / 2 - 64);
            drawRightInfo(gui, String.format("Длинный прыжок: %.2f | Высота: %.2f", MovementClient.getAdminLongJumpDistance(), MovementClient.getAdminLongJumpHeight()), height / 2 - 52);
            drawRightInfo(gui, String.format("Цена выносливости: %.0f", MovementClient.getAdminLongJumpCost()), height / 2 - 40);
        }
        if (page.equals("fog")) drawRightInfo(gui, "Дальность: " + FogClient.getDistance() + " блоков", height / 2 + 12);
        if (page.equals("box_loot")) {
            gui.drawCenteredString(font, LOOT_ITEM_NAMES[selectedLootItem], width / 2, height / 2 - 58, 0xFFFFD08A);
            drawRightInfo(gui, selectedLootDetails(), height / 2 + 12);
        }

        if (page.equals("memory_items")) {
            drawRightInfo(gui, "Спавн: " + (memorySpawnEnabled ? "включён" : "отключён"), height / 2 + 8);
            drawRightInfo(gui, "Шанс: " + memorySpawnChance + "%", height / 2 + 20);
        }
        if (page.equals("story")) {
            int top = height / 2 - 62;
            gui.drawCenteredString(font, Component.literal("Прочитано дисков: " + Integer.bitCount(memoryReadMask) + " / 8"),
                    width / 2, top, 0xFFFFD08A);
            for (int i = 0; i < DISC_NAMES.length; i++) {
                boolean read = (memoryReadMask & (1 << i)) != 0;
                String line = (read ? "✓ " : "○ ") + DISC_NAMES[i] + (read ? " — прочитан" : " — не прочитан");
                gui.drawCenteredString(font, Component.literal(line), width / 2, top + 16 + i * 12,
                        read ? 0xFF9BE39B : 0xFFB0B0B0);
            }
        }
        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void drawRightInfo(GuiGraphics gui, String value, int y) {
        Component text = Component.literal(value);
        int x = Math.max(width / 2 + 96, width - 12 - font.width(text));
        gui.drawString(font, text, x, y, 0xFFFFFFFF, false);
    }

    private String pageName() {
        return switch (page) {
            case "events" -> "СОБЫТИЯ";
            case "activity" -> "РЕГУЛИРОВКА АКТИВНОСТИ";
            case "players" -> "ВЫБЕРИТЕ ИГРОКА";
            case "scream_players" -> "ВЫБЕРИТЕ ЦЕЛЬ СКРИМЕРА";
            case "movement" -> "ДВИЖЕНИЕ";
            case "stamina" -> "ВЫНОСЛИВОСТЬ";
            case "jumps" -> "ПРЫЖКИ";
            case "items" -> "УПРАВЛЕНИЕ ПРЕДМЕТАМИ";
            case "memory_items" -> "ВОСПОМИНАНИЯ";
            case "item_spawn" -> "ШАНСЫ ПОЯВЛЕНИЯ";
            case "give_memory_disc" -> "ВЫБЕРИТЕ ДИСК";
            case "story" -> "СЮЖЕТ И СТАТИСТИКА";
            case "fog" -> "ТУМАН";
            case "box_loot" -> "ЛУТ КОРОБОК";
            default -> "ГЛАВНОЕ МЕНЮ";
        };
    }

    @Override public boolean isPauseScreen() { return false; }
}

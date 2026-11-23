package com.fieldofmemories.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class OldBookScreen extends Screen {
    private static final String[] TITLES = {
            "Мимик", "Предмет-мимик", "Страшное лицо", "Подозрительное пугало",
            "Сталкер за окном", "Дух", "Старая фотокамера", "Видеокамера",
            "Крест", "Пустой крест", "Диски и чужие воспоминания",
            "Аномальный табурет", "Тёмный и чёрный разлом", "Розовый разлом"
    };
    private static final String[] NOTES = {
            "Я видел человека, который двигался совсем как знакомый мне игрок. Но стоило присмотреться, стало не по себе: что-то в его движениях было неправильным. Он не пытался заговорить и будто просто ждал, пока я подойду ближе. Если встречу его снова, не стану сразу подходить. Надо понять, кого именно я видел.",
            "Сегодня мне попалась вещь, которая выглядела совершенно обычной. Я уже потянулся за ней, когда заметил странные знаки. После этого всё произошло слишком быстро, а вещь исчезла. Может, некоторые находки здесь лучше не поднимать с земли? В следующий раз сначала осмотрю её издалека.",
            "Я заметил лицо там, где его быть не должно. Оно не похоже на живого человека, но взгляд будто чувствуется даже через темноту. Когда я слишком долго смотрел в ответ, голова пронзительно заболела. Возможно, не стоит разглядывать его без конца… Или есть способ заставить его отступить?",
            "Пугало опять стояло посреди поля. Я уверен, что раньше его там не было. Самое странное — я отвёл взгляд всего на мгновение, а потом уже не мог вспомнить, в какой стороне оно находилось. Может, оно просто теряется среди травы. Но почему мне кажется, что оно каждый раз оказывается ближе?",
            "За окном кто-то был. Высокая фигура, неподвижная, словно ждала, когда я её замечу. Я посмотрел прямо на неё — и она исчезла так быстро, будто её там никогда не было. Не уверен, что хочу проверять, что случится, если подпустить её ближе.",
            "Я чувствую его раньше, чем успеваю увидеть. Воздух становится тяжёлым, а где-то рядом будто кто-то медленно ходит кругами. Когда оно приблизилось, я едва успел отступить. Обычные удары, кажется, ему безразличны. Может быть, яркая вспышка способна разорвать эту тьму?",
            "Фотокамера ещё работает, хотя выглядит так, будто пережила не одно десятилетие. Вспышка забирает заряд, но на мгновение освещает даже то, чего я не замечал. После нескольких снимков рядом со мной что-то изменилось. Нужно беречь заряд и не тратить вспышку впустую. Возможно, некоторые вещи боятся оказаться на снимке.",
            "Видеокамера показывает мир иначе. Через её объектив я могу наблюдать, не подходя слишком близко, но батарея не бесконечна. Странно, что иногда мне кажется, будто в кадре кто-то есть, хотя передо мной пустое место. Надо следить за зарядом и не доверять каждому изображению.",
            "Обычный крест. Я не знаю, почему он кажется таким тяжёлым, даже когда лежит в руке. Однажды он будто принял на себя чужой удар. После этого я стал относиться к нему осторожнее. Может, он способен защитить меня ещё раз, но я не уверен, что останется прежним.",
            "Крест больше не ощущается прежним. Внутри него словно что-то погасло. И всё же он не совсем бесполезен: если собрать достаточно чужих воспоминаний, возможно, пустоту получится чем-то заполнить. Не знаю, стоит ли пытаться. Мне не нравится мысль о том, что именно может вернуться внутрь.",
            "Каждый диск вызывает одну и ту же боль — будто кто-то пытается протолкнуть в мою голову воспоминание, которое мне не принадлежит. Я нахожу их в самых разных местах: среди вещей в коробках и домах, на деревьях, после встречи с теми, кто не должен существовать. Иногда достаточно долго заниматься обычным делом, чтобы земля под ногами слабо засветилась. Когда я впервые собрал урожай, виски пронзило болью. Что будет, если продолжить искать? Кажется, каждый диск хранит часть чего-то, что я давно забыл… или никогда не знал.",
            "Этот табурет выглядит совершенно обычным, но рядом с ним мне становится трудно думать. Стоит сесть — и всё вокруг начинает расползаться, словно я проваливаюсь в собственную память. Я слышу странные звуки и вижу слова, которых не писал. Если уж снова решусь сесть, нужно быть готовым быстро реагировать. Главное — не задерживаться там дольше, чем получится выдержать.",
            "Разлом тянет к себе даже тогда, когда я стою в стороне. Чёрная поверхность будто поглощает свет, а рядом с ней трудно удержаться на месте. Один из таких провалов словно вырывает меня из привычного мира, другой может оказаться последним, что я увижу. Крест, кажется, способен остановить его, но я не хочу проверять это на себе.",
            "Этот разлом совсем другой. Он не просто затягивает вещи — он будто рассматривает их. Я видел, как предмет зависал перед ним, а потом возвращался уже не тем, чем был. Иногда он выплёвывает что-то полезное, иногда — что-то совершенно неожиданное. Но я не могу отделаться от мысли, что он выбирает сам, что оставить мне. Не стоит бросать туда то, что нельзя потерять."
    };

    private int unlockedMask;
    private List<Integer> pages = List.of();
    private int pageIndex;
    private int left, top, bookWidth, bookHeight;

    private OldBookScreen(int mask) {
        super(Component.literal("Старая книга"));
        this.unlockedMask = mask;
        rebuildPages();
    }

    public static void open(int mask) {
        Minecraft.getInstance().setScreen(new OldBookScreen(mask));
    }

    private void rebuildPages() {
        List<Integer> found = new ArrayList<>();
        for (int i = 0; i < TITLES.length; i++) {
            if ((unlockedMask & (1 << i)) != 0) found.add(i);
        }
        pages = found;
        pageIndex = Math.max(0, Math.min(pageIndex, pages.size() - 1));
    }

    @Override
    protected void init() {
        bookWidth = Math.min(330, width - 24);
        bookHeight = Math.min(236, height - 24);
        left = (width - bookWidth) / 2;
        top = (height - bookHeight) / 2;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        int mid = left + bookWidth / 2;
        gui.fill(left - 3, top + 4, left + bookWidth + 3, top + bookHeight + 4, 0xFF211912);
        gui.fill(left, top, left + bookWidth, top + bookHeight, 0xFF6B452C);
        gui.fill(left + 5, top + 5, mid - 2, top + bookHeight - 5, 0xFFE4D5B4);
        gui.fill(mid + 2, top + 5, left + bookWidth - 5, top + bookHeight - 5, 0xFFE9DDBF);
        gui.fill(mid - 2, top + 5, mid + 2, top + bookHeight - 5, 0xFF9C8060);
        gui.fill(left + 9, top + 9, mid - 6, top + 11, 0x22906E4C);
        gui.fill(mid + 6, top + 9, left + bookWidth - 9, top + 11, 0x22906E4C);

        if (!pages.isEmpty()) {
            int entry = pages.get(pageIndex);
            gui.drawCenteredString(font, TITLES[entry], mid, top + 22, 0xFF352719);
            gui.fill(left + 24, top + 42, left + bookWidth - 24, top + 43, 0x55765A3A);
            List<FormattedCharSequence> lines = font.split(Component.literal(NOTES[entry]),
                    Math.max(80, bookWidth - 48));
            int y = top + 54;
            int maxY = top + bookHeight - 28;
            for (FormattedCharSequence line : lines) {
                if (y > maxY) break;
                gui.drawString(font, line, left + 24, y, 0xFF403326, false);
                y += 10;
            }
            String pageLabel = (pageIndex + 1) + " / " + pages.size();
            gui.drawCenteredString(font, pageLabel, mid, top + bookHeight - 18, 0xFF705A42);
            if (pageIndex > 0) gui.drawString(font, "‹", left + 14, top + bookHeight - 22, 0xFF493321, false);
            if (pageIndex + 1 < pages.size()) gui.drawString(font, "›", left + bookWidth - 20, top + bookHeight - 22, 0xFF493321, false);
        }
        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void movePage(int direction) {
        if (pages.isEmpty()) return;
        pageIndex = Math.max(0, Math.min(pages.size() - 1, pageIndex + direction));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !pages.isEmpty()) {
            if (mouseX < left + 42 && mouseY > top + bookHeight - 38) movePage(-1);
            else if (mouseX > left + bookWidth - 42 && mouseY > top + bookHeight - 38) movePage(1);
            else if (mouseX < left + bookWidth / 2.0D) movePage(-1);
            else movePage(1);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            movePage(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            movePage(1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean isPauseScreen() { return false; }
}

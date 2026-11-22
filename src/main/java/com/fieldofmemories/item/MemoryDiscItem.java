package com.fieldofmemories.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MemoryDiscItem extends Item {
    public static final int WARNING_TICKS = 60;
    public static final int FRAGMENT_TICKS = 50;
    private final int discId;

    private static final String[] TEXTS = {
            "Сегодня, как и всегда, отличная погода. Мама начала готовить тесто для пирога. Ещё с утра она собрала клубничку для пирога. Тесто уже почти готово... но она даже не знает, что клубники больше нет. Будь я дома, меня бы за такое убили. Но дважды не убить.",
            "Я починил старое пугало. Каждый раз будто специально оно ломается и падает в сторону города. Свободы захотела? Ха-ха-ха. Странно, конечно: ветра почти нет... Но, думаю, я, старый дурак, плохо его закрепил.",
            "Сегодня я снова видела её возле поля. Она сказала, что боится праздника. Я всеми силами пыталась узнать почему, но она с улыбкой убегала от вопроса. Странная, но такая добрая и любознательная. Надеюсь, завтра тоже увидимся.",
            "До праздника осталось три дня! Все уже готовятся. Дети делают фигурки, а взрослые собирают дрова. Жду не дождусь, когда кукла взлетит в божественном пламени! Мы сожжём эту большую куклу! Хорошая традиция. Напоминает о том, что даже после смерти можно начать всё заново.",
            "МАМА, Я ГОВОРЮ ТЕБЕ! ТАМ КТО-ТО КРИЧАЛ! КУКЛА КРИЧАЛА МНЕ! В СМЫСЛЕ Я ДУРАК!? Ты не понимаешь! Там точно были крики! Я не мог перепутать! Это точно был крик не из толпы! Пойми! Поверь мне, мама! Я чувствую, что-то неладное. Этот огромный туман после праздника не хочет пропадать! Не к добру это.",
            "Даже тут я буду верен Богу, хотя уже выполнил свой долг. Мне не удалось спасти всех от проклятия, но я спасу тех, кого ещё можно. Даже у самого чистого человека взгляд может быть затуманен... Я не вижу её, но чувствую. К сожалению, зеркал я в городе больше не нашёл.",
            "Вы видите пугало. Оно стоит среди разных полей. Неподвижное и такое одинокое. Старые лохмотья едва держатся на деревянном каркасе. Высокая трава скрывает землю. Такое неаккуратное место... Ветер, грубый и словно призрачный, заставляет всё колыхаться. Кроме пугала, вы никого не видите.",
            "Вы слышите лишь шум листвы и ветра."
    };

    public MemoryDiscItem(int discId, Properties properties) {
        super(properties.stacksTo(1));
        if (discId < 0 || discId >= TEXTS.length) throw new IllegalArgumentException("Invalid memory disc id");
        this.discId = discId;
    }

    public int getDiscId() { return discId; }
    public static String getText(int discId) { return TEXTS[discId]; }

    public static List<String> getFragments(int discId) {
        String text = getText(discId);
        String[] sentences = text.split("(?<=[.!?…])\\s+");
        List<String> fragments = new ArrayList<>();
        for (String sentence : sentences) {
            String remaining = sentence.trim();
            while (remaining.length() > 68) {
                int split = remaining.lastIndexOf(' ', 68);
                if (split < 20) split = remaining.indexOf(' ', 68);
                if (split < 0) break;
                fragments.add(capitalizeFirstLetter(remaining.substring(0, split).trim()));
                remaining = remaining.substring(split + 1).trim();
            }
            if (!remaining.isEmpty()) fragments.add(capitalizeFirstLetter(remaining));
        }
        return fragments;
    }

    private static String capitalizeFirstLetter(String text) {
        if (text.isEmpty()) return text;
        int firstCodePoint = text.codePointAt(0);
        return new StringBuilder()
                .appendCodePoint(Character.toUpperCase(firstCodePoint))
                .append(text.substring(Character.charCount(firstCodePoint)))
                .toString();
    }

}

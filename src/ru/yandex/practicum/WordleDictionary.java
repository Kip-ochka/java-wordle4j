package ru.yandex.practicum;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import ru.yandex.practicum.exception.WordleStateException;

/**
 * Игровой словарь: список слов, пригодных для игры, и рутинные операции над словами.
 * <p>
 * Слова внутри словаря всегда хранятся в нормализованном виде: нижний регистр,
 * буква «ё» заменена на «е», длина ровно {@link #WORD_LENGTH} букв русского алфавита.
 * <p>
 * Класс не работает с консолью и с файлами: чтение файла — задача
 * {@link WordleDictionaryLoader}, вывод — задача {@link Wordle}.
 */
public class WordleDictionary {

    /** Длина загаданного слова. */
    public static final int WORD_LENGTH = 5;

    /** Буква есть в слове и стоит на своём месте. */
    public static final char MARK_EXACT = '+';

    /** Буква есть в слове, но стоит в другом месте. */
    public static final char MARK_PRESENT = '^';

    /** Такой буквы в слове нет. */
    public static final char MARK_ABSENT = '-';

    /** Первая буква русского алфавита в нижнем регистре. */
    private static final char FIRST_LETTER = 'а';

    /** Последняя буква русского алфавита в нижнем регистре («ё» приводится к «е»). */
    private static final char LAST_LETTER = 'я';

    /** Количество букв алфавита, используется для счётчиков букв. */
    public static final int ALPHABET_SIZE = LAST_LETTER - FIRST_LETTER + 1;

    /** Слова словаря в порядке добавления. */
    private final List<String> words;

    /** Тот же набор слов для проверки вхождения за O(1). */
    private final Set<String> index;

    /**
     * Создаёт словарь из готового набора нормализованных слов.
     * Дубликаты отбрасываются, порядок исходной коллекции сохраняется.
     */
    public WordleDictionary(Collection<String> words) {
        if (words == null) {
            throw new WordleStateException("Список слов словаря не может быть null");
        }
        this.index = new HashSet<>(Math.max(16, words.size() * 2));
        this.words = new ArrayList<>(words.size());
        for (String word : words) {
            if (index.add(word)) {
                this.words.add(word);
            }
        }
    }

    /** Количество слов в словаре. */
    public int size() {
        return words.size();
    }

    /** Пуст ли словарь. */
    public boolean isEmpty() {
        return words.isEmpty();
    }

    /** Список слов только для чтения. */
    public List<String> getWords() {
        return Collections.unmodifiableList(words);
    }

    /**
     * Есть ли слово в словаре.
     * Слово должно быть уже нормализовано методом {@link #normalize(String)}.
     */
    public boolean contains(String word) {
        return index.contains(word);
    }

    /** Случайное слово словаря — то самое, которое предстоит отгадать. */
    public String randomWord(Random random) {
        if (isEmpty()) {
            throw new WordleStateException("Нельзя выбрать слово из пустого словаря");
        }
        return words.get(random.nextInt(words.size()));
    }

    /**
     * Оставляет только те слова, которые могли бы быть ответом:
     * сравнение такого слова с {@code guess} даёт ровно подсказку {@code pattern}.
     * <p>
     * Такой фильтр никогда не теряет правильный ответ: настоящий ответ по
     * определению даёт на ход игрока именно ту подсказку, которую увидел игрок.
     *
     * @param guess   слово, которое было названо
     * @param pattern подсказка, полученная на это слово
     * @return новый словарь из подходящих слов
     */
    public WordleDictionary filterByHint(String guess, String pattern) {
        List<String> suitable = new ArrayList<>();
        for (String word : words) {
            if (match(word, guess).equals(pattern)) {
                suitable.add(word);
            }
        }
        return new WordleDictionary(suitable);
    }

    /** Возвращает словарь без указанных слов. */
    public WordleDictionary exclude(Collection<String> excluded) {
        List<String> rest = new ArrayList<>(words.size());
        for (String word : words) {
            if (!excluded.contains(word)) {
                rest.add(word);
            }
        }
        return new WordleDictionary(rest);
    }

    /** Метка порядка байтов, которая может стоять в начале UTF-8 файла. */
    private static final String BYTE_ORDER_MARK = "\uFEFF";

    /**
     * Приводит слово к игровому виду: убирает метку BOM и пробелы по краям,
     * переводит в нижний регистр и заменяет «ё» на «е» — в игре они равнозначны.
     */
    public static String normalize(String word) {
        if (word == null) {
            return "";
        }
        return word.replace(BYTE_ORDER_MARK, "").trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
    }

    /**
     * Подходит ли нормализованное слово для игры:
     * ровно {@link #WORD_LENGTH} букв, и все они — буквы русского алфавита.
     */
    public static boolean isGameWord(String word) {
        if (word == null || word.length() != WORD_LENGTH) {
            return false;
        }
        return isRussian(word);
    }

    /** Состоит ли слово только из букв русского алфавита в нижнем регистре. */
    public static boolean isRussian(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }
        for (int i = 0; i < word.length(); i++) {
            char letter = word.charAt(i);
            if (letter < FIRST_LETTER || letter > LAST_LETTER) {
                return false;
            }
        }
        return true;
    }

    /** Порядковый номер буквы в алфавите, используется для счётчиков букв. */
    public static int letterIndex(char letter) {
        if (letter < FIRST_LETTER || letter > LAST_LETTER) {
            throw new WordleStateException("Символ «" + letter + "» не является буквой русского алфавита");
        }
        return letter - FIRST_LETTER;
    }

    /**
     * Сравнивает названное слово с ответом по правилам Wordle
     * и строит строку-подсказку из символов
     * {@link #MARK_EXACT}, {@link #MARK_PRESENT} и {@link #MARK_ABSENT}.
     * <p>
     * Повторяющиеся буквы учитываются честно: буква отмечается как {@link #MARK_PRESENT}
     * только если в ответе остались её «неизрасходованные» вхождения.
     * Например, сравнение ответа «колос» со словом «около» даёт «^^^^-»,
     * а не подсказку с лишней буквой «о».
     *
     * @param answer загаданное слово
     * @param guess  слово игрока
     * @return строка подсказки длиной {@link #WORD_LENGTH}
     */
    public static String match(String answer, String guess) {
        if (answer == null || guess == null
                || answer.length() != WORD_LENGTH || guess.length() != WORD_LENGTH) {
            throw new WordleStateException("Сравнивать можно только слова из " + WORD_LENGTH + " букв");
        }

        StringBuilder hint = new StringBuilder(WORD_LENGTH);
        hint.append(String.valueOf(MARK_ABSENT).repeat(WORD_LENGTH));

        // Первый проход: точные совпадения; остальные буквы ответа складываем в счётчик.
        int[] unusedLetters = new int[ALPHABET_SIZE];
        for (int i = 0; i < WORD_LENGTH; i++) {
            char answerLetter = answer.charAt(i);
            if (answerLetter == guess.charAt(i)) {
                hint.setCharAt(i, MARK_EXACT);
            } else {
                unusedLetters[letterIndex(answerLetter)]++;
            }
        }

        // Второй проход: буквы не на своём месте, но только пока они есть в счётчике.
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (hint.charAt(i) == MARK_EXACT) {
                continue;
            }
            int letter = letterIndex(guess.charAt(i));
            if (unusedLetters[letter] > 0) {
                unusedLetters[letter]--;
                hint.setCharAt(i, MARK_PRESENT);
            }
        }

        return hint.toString();
    }

    /** Подсказка, означающая полное совпадение слова с ответом. */
    public static String winningHint() {
        return String.valueOf(MARK_EXACT).repeat(WORD_LENGTH);
    }
}

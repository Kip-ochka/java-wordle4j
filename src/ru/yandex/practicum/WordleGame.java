package ru.yandex.practicum;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import ru.yandex.practicum.exception.NoSuggestionAvailable;
import ru.yandex.practicum.exception.WordHasInvalidCharacters;
import ru.yandex.practicum.exception.WordHasWrongLength;
import ru.yandex.practicum.exception.WordNotFoundInDictionary;
import ru.yandex.practicum.exception.WordleGameException;
import ru.yandex.practicum.exception.WordleStateException;

/**
 * Класс игры: хранит состояние партии и умеет делать ход, проверять ответ
 * и вычислять слово-подсказку.
 * <p>
 * Класс сознательно ничего не знает о консоли — весь ввод и вывод остаётся
 * в {@link Wordle}. Это позволяет тестировать игру без эмуляции ввода.
 */
public class WordleGame {

    /** Сколько ходов даётся игроку. */
    public static final int TOTAL_STEPS = 6;

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PrintWriter log;

    private final Random random;

    /** Полный игровой словарь. */
    private final WordleDictionary dictionary;

    /** Загаданное слово. */
    private final String answer;

    /** Слова, которые ещё могут оказаться ответом с учётом всех подсказок. */
    private WordleDictionary candidates;

    /** Сколько ходов осталось. */
    private int steps;

    private GameState state;

    /** История партии: названное слово -> полученная подсказка, в порядке ходов. */
    private final Map<String, String> history = new LinkedHashMap<>();

    /** Слова, уже выданные игроку в качестве подсказки: одно и то же не предлагаем дважды. */
    private final Set<String> suggested = new LinkedHashSet<>();

    /** Буквы, которые точно есть в ответе. */
    private final Set<Character> presentLetters = new TreeSet<>();

    /** Буквы, которых точно нет в ответе. */
    private final Set<Character> absentLetters = new TreeSet<>();

    /** Открытые позиции: индекс буквы -> сама буква, в порядке позиций в слове. */
    private final Map<Integer, Character> knownPositions = new TreeMap<>();

    /**
     * Обычная игра: слово загадывается случайно из всего словаря.
     */
    public WordleGame(WordleDictionary dictionary, PrintWriter log) {
        this(dictionary, log, new Random());
    }

    /**
     * Игра с заданным источником случайности — удобно для воспроизводимых тестов.
     */
    public WordleGame(WordleDictionary dictionary, PrintWriter log, Random random) {
        this(dictionary, log, random, requireDictionary(dictionary).randomWord(random));
    }

    /**
     * Игра с заранее известным ответом — используется в тестах.
     *
     * @param dictionary игровой словарь
     * @param log        лог-файл программы
     * @param random     источник случайности для подсказок
     * @param answer     загаданное слово, должно быть в словаре
     */
    public WordleGame(WordleDictionary dictionary, PrintWriter log, Random random, String answer) {
        requireDictionary(dictionary);
        if (log == null) {
            throw new IllegalArgumentException("Лог обязателен для игры");
        }
        String normalizedAnswer = WordleDictionary.normalize(answer);
        if (!dictionary.contains(normalizedAnswer)) {
            throw new WordleStateException("Загаданного слова «" + normalizedAnswer + "» нет в словаре");
        }

        this.dictionary = dictionary;
        this.log = log;
        this.random = random;
        this.answer = normalizedAnswer;
        this.candidates = dictionary;
        this.steps = TOTAL_STEPS;
        this.state = GameState.IN_PROGRESS;

        writeLog("Начата игра. Слов в словаре: " + dictionary.size()
                + ", ходов: " + steps + ", загадано: " + this.answer);
    }

    /**
     * Делает ход: проверяет слово на корректность, уменьшает счётчик ходов,
     * строит подсказку и сужает множество вариантов.
     *
     * @param rawWord слово в том виде, в каком его ввёл игрок
     * @return результат хода
     * @throws WordHasInvalidCharacters слово пустое или содержит не только русские буквы
     * @throws WordHasWrongLength       длина слова не равна {@link WordleDictionary#WORD_LENGTH}
     * @throws WordNotFoundInDictionary такого слова нет в словаре
     */
    public MoveResult makeMove(String rawWord) throws WordleGameException {
        requireRunning();

        String word = WordleDictionary.normalize(rawWord);
        validate(word);

        steps--;
        if (steps < 0) {
            throw new WordleStateException("Счётчик ходов ушёл в минус: " + steps);
        }

        String hint = WordleDictionary.match(answer, word);
        history.put(word, hint);
        rememberLetters(word, hint);

        if (word.equals(answer)) {
            state = GameState.WON;
        } else {
            narrowCandidates(word, hint);
            if (steps == 0) {
                state = GameState.LOST;
            }
        }

        writeLog("Ход: " + word + " -> " + hint + ", осталось ходов: " + steps
                + ", вариантов: " + candidates.size() + ", состояние: " + state);

        return new MoveResult(word, hint, steps, state);
    }

    /**
     * Предлагает слово, которое не противоречит ни одной из уже полученных подсказок.
     * <p>
     * Из подходящих слов выбирается то, буквы которого чаще всего встречаются
     * среди оставшихся вариантов: такое слово в среднем сильнее сужает поиск,
     * поэтому компьютер способен пройти игру сам. Слова, уже предложенные ранее,
     * повторно не выдаются.
     *
     * @return слово-подсказка
     * @throws NoSuggestionAvailable новых подходящих слов не осталось
     */
    public String suggest() throws NoSuggestionAvailable {
        requireRunning();

        List<String> pool = new ArrayList<>(candidates.size());
        for (String word : candidates.getWords()) {
            if (!suggested.contains(word)) {
                pool.add(word);
            }
        }
        if (pool.isEmpty()) {
            writeLog("Подсказка невозможна: вариантов " + candidates.size()
                    + ", все они уже предлагались");
            throw new NoSuggestionAvailable("Подходящих слов больше не осталось");
        }

        String suggestion = chooseBest(pool);
        suggested.add(suggestion);
        writeLog("Подсказка: " + suggestion + " (из " + pool.size() + " вариантов)");
        return suggestion;
    }

    /**
     * Выбирает из списка слово с самыми частыми буквами.
     * Частота считается по уникальным буквам слова, поэтому слова с повторами
     * получают меньший вес и проверяют за один ход больше разных букв.
     * Сложность — O(N × {@link WordleDictionary#WORD_LENGTH}).
     */
    private String chooseBest(List<String> pool) {
        // Сколько слов содержат каждую букву алфавита.
        int[] frequency = new int[WordleDictionary.ALPHABET_SIZE];
        for (String word : pool) {
            boolean[] counted = new boolean[WordleDictionary.ALPHABET_SIZE];
            for (int i = 0; i < word.length(); i++) {
                int letter = WordleDictionary.letterIndex(word.charAt(i));
                if (!counted[letter]) {
                    counted[letter] = true;
                    frequency[letter]++;
                }
            }
        }

        List<String> best = new ArrayList<>();
        int bestScore = -1;
        for (String word : pool) {
            int score = 0;
            boolean[] counted = new boolean[WordleDictionary.ALPHABET_SIZE];
            for (int i = 0; i < word.length(); i++) {
                int letter = WordleDictionary.letterIndex(word.charAt(i));
                if (!counted[letter]) {
                    counted[letter] = true;
                    score += frequency[letter];
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best.clear();
                best.add(word);
            } else if (score == bestScore) {
                best.add(word);
            }
        }
        return best.get(random.nextInt(best.size()));
    }

    /** Проверяет ввод игрока до обращения к словарю. */
    private void validate(String word) throws WordleGameException {
        if (word.isEmpty()) {
            throw new WordHasInvalidCharacters(word);
        }
        if (word.length() != WordleDictionary.WORD_LENGTH) {
            throw new WordHasWrongLength(word);
        }
        if (!WordleDictionary.isRussian(word)) {
            throw new WordHasInvalidCharacters(word);
        }
        // Правильный ответ заведомо есть в словаре, лишний поиск не нужен.
        if (!word.equals(answer) && !dictionary.contains(word)) {
            throw new WordNotFoundInDictionary(word);
        }
    }

    /** Сужает множество вариантов и проверяет, что ответ из него не потерялся. */
    private void narrowCandidates(String word, String hint) {
        candidates = candidates.filterByHint(word, hint);
        if (!candidates.contains(answer)) {
            throw new WordleStateException("Ответ «" + answer + "» потерян при фильтрации после хода «"
                    + word + "» с подсказкой «" + hint + "»");
        }
    }

    /** Копит сведения о буквах — они попадают в лог и доступны тестам. */
    private void rememberLetters(String word, String hint) {
        for (int i = 0; i < WordleDictionary.WORD_LENGTH; i++) {
            char letter = word.charAt(i);
            char mark = hint.charAt(i);
            if (mark == WordleDictionary.MARK_EXACT) {
                presentLetters.add(letter);
                knownPositions.put(i, letter);
            } else if (mark == WordleDictionary.MARK_PRESENT) {
                presentLetters.add(letter);
            } else if (!presentLetters.contains(letter)) {
                absentLetters.add(letter);
            }
        }
        writeLog("Известно: буквы есть " + presentLetters + ", букв нет " + absentLetters
                + ", открытые позиции " + knownPositions);
    }

    private void requireRunning() {
        if (state != GameState.IN_PROGRESS) {
            throw new WordleStateException("Игра уже завершена в состоянии " + state);
        }
        if (steps <= 0) {
            throw new WordleStateException("Ходы закончились, но игра осталась в состоянии " + state);
        }
    }

    private static WordleDictionary requireDictionary(WordleDictionary dictionary) {
        if (dictionary == null || dictionary.isEmpty()) {
            throw new WordleStateException("Игра невозможна: словарь пуст");
        }
        return dictionary;
    }

    private void writeLog(String message) {
        log.println(LocalDateTime.now().format(TIMESTAMP) + " [Game] " + message);
    }

    /** Загадка партии. Показывается игроку только после её окончания. */
    public String getAnswer() {
        return answer;
    }

    public GameState getState() {
        return state;
    }

    /** Закончилась ли игра — условие выхода из игрового цикла. */
    public boolean isFinished() {
        return state != GameState.IN_PROGRESS;
    }

    public int getStepsLeft() {
        return steps;
    }

    /** Номер текущего хода, начиная с единицы. */
    public int getStepNumber() {
        return TOTAL_STEPS - steps + 1;
    }

    /** Сколько ходов уже сделано. */
    public int getStepsMade() {
        return TOTAL_STEPS - steps;
    }

    /** История партии в порядке ходов: слово -> подсказка. */
    public Map<String, String> getHistory() {
        return Collections.unmodifiableMap(history);
    }

    /** Слова, которые ещё могут быть ответом. */
    public WordleDictionary getCandidates() {
        return candidates;
    }

    public Set<Character> getPresentLetters() {
        return Collections.unmodifiableSet(presentLetters);
    }

    public Set<Character> getAbsentLetters() {
        return Collections.unmodifiableSet(absentLetters);
    }

    public Map<Integer, Character> getKnownPositions() {
        return Collections.unmodifiableMap(knownPositions);
    }
}

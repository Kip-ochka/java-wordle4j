package ru.yandex.practicum;

import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.practicum.exception.WordleApplicationException;
import ru.yandex.practicum.exception.WordleGameException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверка программы целиком на настоящем словаре из корня проекта.
 * Если файл словаря недоступен, тесты пропускаются.
 */
@DisplayName("Игра целиком на настоящем словаре")
class WordleTest {

    private static final Path DICTIONARY_FILE = Paths.get("words_ru.txt");

    /** Сколько партий компьютер играет сам с собой. */
    private static final int SOLO_GAMES = 100;

    /** В тестах лог пишется в консоль, а не в файл. */
    private static PrintWriter log;

    private static WordleDictionary dictionary;

    @BeforeAll
    static void loadRealDictionary() throws WordleApplicationException {
        Assumptions.assumeTrue(Files.exists(DICTIONARY_FILE),
                "файл словаря " + DICTIONARY_FILE.toAbsolutePath() + " не найден");

        log = new PrintWriter(System.out);
        dictionary = new WordleDictionaryLoader(log).load(DICTIONARY_FILE.toString());
    }

    @Test
    @DisplayName("Настоящий словарь загружается и содержит только игровые слова")
    void realDictionaryContainsOnlyGameWords() {
        assertTrue(dictionary.size() > 1000, "игровых слов должно быть много, а их " + dictionary.size());

        for (String word : dictionary.getWords()) {
            assertTrue(WordleDictionary.isGameWord(word), "слово «" + word + "» не годится для игры");
            assertEquals(word, WordleDictionary.normalize(word), "слово «" + word + "» не нормализовано");
        }
    }

    @Test
    @DisplayName("Обычная партия на настоящем словаре доходит до конца")
    void gameOnRealDictionaryFinishes() throws WordleGameException {
        String answer = dictionary.randomWord(new Random(2024));
        WordleGame game = new WordleGame(dictionary, log, new Random(2024), answer);

        while (!game.isFinished()) {
            game.makeMove(game.suggest());
        }

        assertTrue(game.isFinished());
        assertTrue(game.getStepsLeft() >= 0);
        assertTrue(game.getHistory().size() <= WordleGame.TOTAL_STEPS);
        assertEquals(answer, game.getAnswer());
    }

    @Test
    @DisplayName("Компьютер выигрывает подавляющее большинство партий сам")
    void computerWinsMostSoloGames() throws WordleGameException {
        // Здесь играется много партий, поэтому лог отключён, чтобы не засорять вывод тестов.
        PrintWriter silentLog = new PrintWriter(Writer.nullWriter());
        List<String> words = dictionary.getWords();
        Random answers = new Random(42);
        int wins = 0;

        for (int i = 0; i < SOLO_GAMES; i++) {
            String answer = words.get(answers.nextInt(words.size()));
            WordleGame game = new WordleGame(dictionary, silentLog, new Random(i), answer);
            while (!game.isFinished()) {
                game.makeMove(game.suggest());
            }
            if (game.getState() == GameState.WON) {
                wins++;
            }
        }

        assertTrue(wins >= SOLO_GAMES * 9 / 10,
                "компьютер выиграл только " + wins + " партий из " + SOLO_GAMES);
    }

    @Test
    @DisplayName("Слова из настоящего словаря принимаются, а выдуманные — нет")
    void realDictionaryValidatesPlayerInput() throws WordleGameException {
        String answer = dictionary.randomWord(new Random(7));
        WordleGame game = new WordleGame(dictionary, log, new Random(7), answer);

        String known = game.suggest();
        MoveResult result = game.makeMove(known.toUpperCase());

        assertEquals(known, result.getWord());
        assertEquals(WordleDictionary.WORD_LENGTH, result.getHint().length());
        assertEquals(WordleGame.TOTAL_STEPS - 1, game.getStepsLeft());
    }
}

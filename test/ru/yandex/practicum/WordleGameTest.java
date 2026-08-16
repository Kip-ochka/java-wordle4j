package ru.yandex.practicum;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.practicum.exception.NoSuggestionAvailable;
import ru.yandex.practicum.exception.WordHasInvalidCharacters;
import ru.yandex.practicum.exception.WordHasWrongLength;
import ru.yandex.practicum.exception.WordNotFoundInDictionary;
import ru.yandex.practicum.exception.WordleGameException;
import ru.yandex.practicum.exception.WordleStateException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Игровой класс: ходы, состояние и подсказки")
class WordleGameTest {

    private static final String ANSWER = "герой";

    private static final List<String> WORDS = Arrays.asList(
            "герой", "гонец", "весна", "весло", "ветер", "верба",
            "ворон", "конус", "камыш", "мороз", "колос", "около",
            "сокол", "полка", "палка", "ножик");

    /** В тестах лог пишется в консоль, а не в файл. */
    private static PrintWriter log;

    private static WordleDictionary dictionary;

    private WordleGame game;

    @BeforeAll
    static void createDictionary() {
        log = new PrintWriter(System.out);
        dictionary = new WordleDictionary(WORDS);
    }

    @BeforeEach
    void createGame() {
        game = new WordleGame(dictionary, log, new Random(1), ANSWER);
    }

    @Test
    @DisplayName("Новая игра начинается с шести ходов и полного словаря вариантов")
    void newGameIsReadyToPlay() {
        assertEquals(WordleGame.TOTAL_STEPS, game.getStepsLeft());
        assertEquals(1, game.getStepNumber());
        assertEquals(0, game.getStepsMade());
        assertEquals(GameState.IN_PROGRESS, game.getState());
        assertFalse(game.isFinished());
        assertEquals(ANSWER, game.getAnswer());
        assertEquals(dictionary.size(), game.getCandidates().size());
        assertTrue(game.getHistory().isEmpty());
    }

    @Test
    @DisplayName("Правильный ход уменьшает счётчик и возвращает подсказку")
    void correctMoveDecreasesSteps() throws WordleGameException {
        MoveResult result = game.makeMove("гонец");

        assertEquals("гонец", result.getWord());
        assertEquals("+^-^-", result.getHint());
        assertEquals(5, result.getStepsLeft());
        assertEquals(GameState.IN_PROGRESS, result.getState());
        assertFalse(result.isWin());
        assertEquals(5, game.getStepsLeft());
        assertEquals(2, game.getStepNumber());
    }

    @Test
    @DisplayName("Слово игрока нормализуется перед проверкой")
    void moveNormalizesPlayerInput() throws WordleGameException {
        MoveResult result = game.makeMove("  ГоНеЦ  ");

        assertEquals("гонец", result.getWord());
        assertEquals("+^-^-", result.getHint());
    }

    @Test
    @DisplayName("Угаданное слово завершает игру победой")
    void guessedWordWinsTheGame() throws WordleGameException {
        MoveResult result = game.makeMove("ГЕРОЙ");

        assertTrue(result.isWin());
        assertEquals(WordleDictionary.winningHint(), result.getHint());
        assertEquals(GameState.WON, game.getState());
        assertTrue(game.isFinished());
        assertEquals(1, game.getStepsMade());
    }

    @Test
    @DisplayName("Когда ходы кончились, игра завершается проигрышем")
    void gameIsLostWhenStepsRunOut() throws WordleGameException {
        List<String> wrongWords = Arrays.asList("гонец", "весна", "весло", "ветер", "верба", "ворон");

        for (String word : wrongWords) {
            assertFalse(game.isFinished());
            game.makeMove(word);
        }

        assertEquals(GameState.LOST, game.getState());
        assertTrue(game.isFinished());
        assertEquals(0, game.getStepsLeft());
        assertEquals(WordleGame.TOTAL_STEPS, game.getHistory().size());
    }

    @Test
    @DisplayName("Ход в завершённой игре — внутренняя ошибка")
    void moveAfterEndOfGameFails() throws WordleGameException {
        game.makeMove(ANSWER);

        assertThrows(WordleStateException.class, () -> game.makeMove("гонец"));
        assertThrows(WordleStateException.class, () -> game.suggest());
    }

    @Test
    @DisplayName("Слово неправильной длины не засчитывается за ход")
    void wordOfWrongLengthDoesNotCountAsMove() {
        assertThrows(WordHasWrongLength.class, () -> game.makeMove("мост"));
        assertThrows(WordHasWrongLength.class, () -> game.makeMove("мостики"));

        assertEquals(WordleGame.TOTAL_STEPS, game.getStepsLeft());
        assertTrue(game.getHistory().isEmpty());
    }

    @Test
    @DisplayName("Слово не из русских букв не засчитывается за ход")
    void wordWithForeignCharactersDoesNotCountAsMove() {
        assertThrows(WordHasInvalidCharacters.class, () -> game.makeMove("hello"));
        assertThrows(WordHasInvalidCharacters.class, () -> game.makeMove("ай-ай"));
        assertThrows(WordHasInvalidCharacters.class, () -> game.makeMove("12345"));
        assertThrows(WordHasInvalidCharacters.class, () -> game.makeMove(""));
        assertThrows(WordHasInvalidCharacters.class, () -> game.makeMove("   "));
        assertThrows(WordHasInvalidCharacters.class, () -> game.makeMove(null));

        assertEquals(WordleGame.TOTAL_STEPS, game.getStepsLeft());
    }

    @Test
    @DisplayName("Слова не из словаря не засчитываются за ход")
    void unknownWordDoesNotCountAsMove() {
        WordNotFoundInDictionary exception =
                assertThrows(WordNotFoundInDictionary.class, () -> game.makeMove("зебра"));

        assertEquals("зебра", exception.getWord());
        assertEquals(WordleGame.TOTAL_STEPS, game.getStepsLeft());
    }

    @Test
    @DisplayName("История хранит все ходы вместе с подсказками")
    void historyKeepsMovesInOrder() throws WordleGameException {
        game.makeMove("гонец");
        game.makeMove("ворон");

        Map<String, String> history = game.getHistory();
        assertEquals(2, history.size());
        assertEquals(Arrays.asList("гонец", "ворон"), new ArrayList<>(history.keySet()));
        assertEquals("+^-^-", history.get("гонец"));
        assertThrows(UnsupportedOperationException.class, () -> history.put("весна", "-----"));
    }

    @Test
    @DisplayName("Игра запоминает, какие буквы есть в ответе, а каких нет")
    void gameRemembersLetters() throws WordleGameException {
        game.makeMove("гонец");

        assertTrue(game.getPresentLetters().contains('г'));
        assertTrue(game.getPresentLetters().contains('о'));
        assertTrue(game.getPresentLetters().contains('е'));
        assertTrue(game.getAbsentLetters().contains('н'));
        assertTrue(game.getAbsentLetters().contains('ц'));
        assertEquals('г', game.getKnownPositions().get(0).charValue());
        assertFalse(game.getKnownPositions().containsKey(1));
    }

    @Test
    @DisplayName("Подсказка — слово из словаря, и оно не повторяется")
    void suggestionIsFreshDictionaryWord() throws WordleGameException {
        String first = game.suggest();
        String second = game.suggest();

        assertTrue(dictionary.contains(first));
        assertTrue(dictionary.contains(second));
        assertNotEquals(first, second);
    }

    @Test
    @DisplayName("Подсказка не противоречит уже полученным подсказкам")
    void suggestionAgreesWithPreviousHints() throws WordleGameException {
        MoveResult first = game.makeMove("гонец");

        String suggestion = game.suggest();
        assertEquals(first.getHint(), WordleDictionary.match(suggestion, first.getWord()),
                "подсказка «" + suggestion + "» противоречит ходу «" + first.getWord() + "»");

        for (String candidate : game.getCandidates().getWords()) {
            assertEquals(first.getHint(), WordleDictionary.match(candidate, first.getWord()),
                    "вариант «" + candidate + "» противоречит ходу «" + first.getWord() + "»");
        }
    }

    @Test
    @DisplayName("Правильный ответ никогда не исчезает из вариантов")
    void answerStaysAmongCandidates() throws WordleGameException {
        game.makeMove("гонец");
        assertTrue(game.getCandidates().contains(ANSWER));

        game.makeMove("ворон");
        assertTrue(game.getCandidates().contains(ANSWER));
        assertTrue(game.getCandidates().size() < dictionary.size(), "варианты должны сужаться");
    }

    @Test
    @DisplayName("Когда подходящих слов больше нет, игра сообщает об этом исключением")
    void suggestionMayRunOut() throws WordleGameException {
        // Оставляем ровно один вариант — сам ответ — и «расходуем» его подсказкой.
        WordleDictionary tiny = new WordleDictionary(Arrays.asList(ANSWER, "гонец"));
        WordleGame tinyGame = new WordleGame(tiny, log, new Random(1), ANSWER);
        tinyGame.makeMove("гонец");

        assertEquals(1, tinyGame.getCandidates().size());
        assertEquals(ANSWER, tinyGame.suggest());
        assertThrows(NoSuggestionAvailable.class, () -> tinyGame.suggest());
    }

    @Test
    @DisplayName("Компьютер отгадывает любое слово словаря за шесть ходов")
    void computerSolvesEveryWordOnItsOwn() throws WordleGameException {
        for (String answer : WORDS) {
            WordleGame solo = new WordleGame(dictionary, log, new Random(answer.hashCode()), answer);
            while (!solo.isFinished()) {
                solo.makeMove(solo.suggest());
            }
            assertEquals(GameState.WON, solo.getState(), "компьютер не отгадал слово «" + answer + "»");
        }
    }

    @Test
    @DisplayName("Игру нельзя создать с пустым словарём или чужим ответом")
    void gameRejectsBrokenSetup() {
        WordleDictionary empty = new WordleDictionary(new ArrayList<>());

        assertThrows(WordleStateException.class, () -> new WordleGame(empty, log));
        assertThrows(WordleStateException.class, () -> new WordleGame(null, log));
        assertThrows(WordleStateException.class,
                () -> new WordleGame(dictionary, log, new Random(1), "зебра"));
        assertThrows(IllegalArgumentException.class,
                () -> new WordleGame(dictionary, null, new Random(1), ANSWER));
    }
}

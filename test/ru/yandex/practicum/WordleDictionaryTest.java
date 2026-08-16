package ru.yandex.practicum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.practicum.exception.WordleStateException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Словарь и побуквенное сравнение слов")
class WordleDictionaryTest {

    /** Метка порядка байтов, которая может встретиться в начале UTF-8 файла. */
    private static final char BOM = (char) 0xFEFF;

    private static WordleDictionary dictionary;

    @BeforeAll
    static void createDictionary() {
        dictionary = new WordleDictionary(Arrays.asList("герой", "гонец", "весна", "колос", "около"));
    }

    @Test
    @DisplayName("Нормализация убирает пробелы, регистр и букву «ё»")
    void normalizeTrimsLowercasesAndReplacesYo() {
        assertEquals("герой", WordleDictionary.normalize("  ГеРоЙ  "));
        assertEquals("ежики", WordleDictionary.normalize("ЁЖИКИ"));
        assertEquals("елочка", WordleDictionary.normalize("Ёлочка"));
        assertEquals("герой", WordleDictionary.normalize(BOM + "герой"));
        assertEquals("", WordleDictionary.normalize(null));
        assertEquals("", WordleDictionary.normalize("   "));
    }

    @Test
    @DisplayName("Для игры подходят только слова из пяти русских букв")
    void isGameWordAcceptsOnlyFiveRussianLetters() {
        assertTrue(WordleDictionary.isGameWord("герой"));
        assertFalse(WordleDictionary.isGameWord("гонг"), "четыре буквы");
        assertFalse(WordleDictionary.isGameWord("мостик"), "шесть букв");
        assertFalse(WordleDictionary.isGameWord("hello"), "латиница");
        assertFalse(WordleDictionary.isGameWord("ай-ай"), "дефис");
        assertFalse(WordleDictionary.isGameWord("а ля "), "пробел");
        assertFalse(WordleDictionary.isGameWord("ГЕРОЙ"), "ненормализованное слово");
        assertFalse(WordleDictionary.isGameWord("ёжики"), "буква «ё» должна быть заменена");
        assertFalse(WordleDictionary.isGameWord(null));
    }

    @Test
    @DisplayName("Пример из технического задания: герой и гонец дают +^-^-")
    void matchBuildsHintFromTaskExample() {
        assertEquals("+^-^-", WordleDictionary.match("герой", "гонец"));
    }

    @Test
    @DisplayName("Полное совпадение отмечается пятью плюсами")
    void matchOfEqualWordsIsWinningHint() {
        assertEquals("+++++", WordleDictionary.match("весна", "весна"));
        assertEquals(WordleDictionary.winningHint(), WordleDictionary.match("герой", "герой"));
    }

    @Test
    @DisplayName("Если общих букв нет, подсказка состоит из минусов")
    void matchWithoutCommonLettersIsAllMinuses() {
        assertEquals("-----", WordleDictionary.match("шпиль", "багет"));
    }

    @Test
    @DisplayName("Повторяющиеся буквы не отмечаются лишний раз")
    void matchCountsRepeatedLettersOnce() {
        // в ответе «колос» две буквы «о», в слове «около» — три, третья остаётся без отметки
        assertEquals("^^^^-", WordleDictionary.match("колос", "около"));
        // в ответе «полка» буква «о» одна, отмечается только первая «о» слова
        assertEquals("^^-^-", WordleDictionary.match("полка", "около"));
        // единственную «с» ответа забирает точное совпадение, первой «с» слова не достаётся
        assertEquals("--+++", WordleDictionary.match("весна", "сосна"));
        // трём буквам «о» слова соответствует одна «о» ответа
        assertEquals("^^--+", WordleDictionary.match("сосна", "осока"));
    }

    @Test
    @DisplayName("Сравнивать можно только слова правильной длины")
    void matchRejectsWordsOfWrongLength() {
        assertThrows(WordleStateException.class, () -> WordleDictionary.match("герой", "гонг"));
        assertThrows(WordleStateException.class, () -> WordleDictionary.match("мостик", "герой"));
        assertThrows(WordleStateException.class, () -> WordleDictionary.match(null, "герой"));
    }

    @Test
    @DisplayName("Слово из одинаковых букв сравнивается корректно")
    void matchHandlesWordOfSameLetters() {
        assertEquals("+----", WordleDictionary.match("ааааа", "агент"));
        assertEquals("+++++", WordleDictionary.match("ооооо", "ооооо"));
    }

    @Test
    @DisplayName("Словарь знает свои слова и не знает чужих")
    void containsFindsOnlyDictionaryWords() {
        assertTrue(dictionary.contains("герой"));
        assertTrue(dictionary.contains("около"));
        assertFalse(dictionary.contains("зебра"));
        assertFalse(dictionary.contains("ГЕРОЙ"), "поиск идёт по нормализованным словам");
        assertEquals(5, dictionary.size());
        assertFalse(dictionary.isEmpty());
    }

    @Test
    @DisplayName("Дубликаты при создании словаря отбрасываются, порядок сохраняется")
    void constructorRemovesDuplicates() {
        WordleDictionary withDuplicates =
                new WordleDictionary(Arrays.asList("герой", "гонец", "герой", "гонец"));

        assertEquals(2, withDuplicates.size());
        assertEquals(Arrays.asList("герой", "гонец"), withDuplicates.getWords());
    }

    @Test
    @DisplayName("Список слов недоступен для изменения снаружи")
    void wordListIsUnmodifiable() {
        List<String> words = dictionary.getWords();
        assertThrows(UnsupportedOperationException.class, () -> words.add("зебра"));
    }

    @Test
    @DisplayName("Случайное слово всегда берётся из словаря")
    void randomWordBelongsToDictionary() {
        Random random = new Random(7);
        for (int i = 0; i < 50; i++) {
            assertTrue(dictionary.contains(dictionary.randomWord(random)));
        }
    }

    @Test
    @DisplayName("Из пустого словаря слово выбрать нельзя")
    void randomWordOnEmptyDictionaryFails() {
        WordleDictionary empty = new WordleDictionary(new ArrayList<>());

        assertTrue(empty.isEmpty());
        assertThrows(WordleStateException.class, () -> empty.randomWord(new Random()));
    }

    @Test
    @DisplayName("Словарь нельзя создать из null")
    void constructorRejectsNull() {
        assertThrows(WordleStateException.class, () -> new WordleDictionary(null));
    }

    @Test
    @DisplayName("Фильтр по подсказке оставляет только совместимые слова и не теряет ответ")
    void filterByHintKeepsSuitableWords() {
        WordleDictionary suitable = dictionary.filterByHint("гонец", "+^-^-");

        assertTrue(suitable.contains("герой"), "правильный ответ обязан остаться");
        assertFalse(suitable.contains("весна"));
        assertFalse(suitable.contains("гонец"), "названное слово больше не вариант");
        for (String word : suitable.getWords()) {
            assertEquals("+^-^-", WordleDictionary.match(word, "гонец"));
        }
    }

    @Test
    @DisplayName("Исключение слов удаляет их из словаря")
    void excludeRemovesWords() {
        WordleDictionary rest = dictionary.exclude(Arrays.asList("герой", "весна"));

        assertEquals(3, rest.size());
        assertFalse(rest.contains("герой"));
        assertTrue(rest.contains("гонец"));
    }

    @Test
    @DisplayName("Номер буквы считается только для русского алфавита")
    void letterIndexRejectsForeignCharacters() {
        assertEquals(0, WordleDictionary.letterIndex('а'));
        assertEquals(WordleDictionary.ALPHABET_SIZE - 1, WordleDictionary.letterIndex('я'));
        assertThrows(WordleStateException.class, () -> WordleDictionary.letterIndex('z'));
        assertThrows(WordleStateException.class, () -> WordleDictionary.letterIndex('ё'));
    }
}

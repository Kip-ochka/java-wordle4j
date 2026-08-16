package ru.yandex.practicum;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.yandex.practicum.exception.DictionaryFileException;
import ru.yandex.practicum.exception.EmptyDictionaryException;
import ru.yandex.practicum.exception.WordleApplicationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Загрузка словаря из файла")
class WordleDictionaryLoaderTest {

    /** В тестах лог пишется в консоль, а не в файл. */
    private static PrintWriter log;

    private static WordleDictionaryLoader loader;

    @TempDir
    Path directory;

    @BeforeAll
    static void createLoader() {
        log = new PrintWriter(System.out);
        loader = new WordleDictionaryLoader(log);
    }

    @Test
    @DisplayName("Из файла отбираются только слова из пяти русских букв")
    void loadKeepsOnlyGameWords() throws IOException, WordleApplicationException {
        Path file = writeDictionary(
                "аба",
                "абажур",
                "герой",
                "гонец",
                "мост",
                "а-ля фуршет",
                "hello",
                "весна");

        WordleDictionary dictionary = loader.load(file.toString());

        assertEquals(3, dictionary.size());
        assertEquals(Arrays.asList("герой", "гонец", "весна"), dictionary.getWords());
        assertFalse(dictionary.contains("мост"));
        assertFalse(dictionary.contains("hello"));
    }

    @Test
    @DisplayName("Слова приводятся к нижнему регистру, «ё» заменяется на «е»")
    void loadNormalizesWords() throws IOException, WordleApplicationException {
        Path file = writeDictionary("ГЕРОЙ", "  Гонец  ", "ЁЖИКИ");

        WordleDictionary dictionary = loader.load(file.toString());

        assertEquals(3, dictionary.size());
        assertTrue(dictionary.contains("герой"));
        assertTrue(dictionary.contains("гонец"));
        assertTrue(dictionary.contains("ежики"));
    }

    @Test
    @DisplayName("Слова, ставшие одинаковыми после нормализации, не дублируются")
    void loadRemovesDuplicates() throws IOException, WordleApplicationException {
        Path file = writeDictionary("ёжики", "ежики", "ЕЖИКИ", "герой", "Герой");

        WordleDictionary dictionary = loader.load(file.toString());

        assertEquals(2, dictionary.size());
    }

    @Test
    @DisplayName("Пустые строки в файле пропускаются")
    void loadSkipsBlankLines() throws IOException, WordleApplicationException {
        Path file = writeDictionary("", "   ", "герой", "");

        WordleDictionary dictionary = loader.load(file.toString());

        assertEquals(1, dictionary.size());
    }

    @Test
    @DisplayName("Отсутствие файла словаря — ошибка работы программы")
    void loadOfMissingFileFails() {
        String missing = directory.resolve("нет-такого-файла.txt").toString();

        DictionaryFileException exception =
                assertThrows(DictionaryFileException.class, () -> loader.load(missing));
        assertEquals(missing, exception.getFileName());
        assertTrue(exception instanceof WordleApplicationException);
    }

    @Test
    @DisplayName("Файл без подходящих слов — ошибка работы программы")
    void loadOfFileWithoutGameWordsFails() throws IOException {
        Path file = writeDictionary("мост", "абажур", "hello", "");

        assertThrows(EmptyDictionaryException.class, () -> loader.load(file.toString()));
    }

    @Test
    @DisplayName("Загрузчику обязательно нужен лог")
    void loaderRequiresLog() {
        assertThrows(IllegalArgumentException.class, () -> new WordleDictionaryLoader(null));
    }

    /** Записывает временный файл словаря в кодировке UTF-8. */
    private Path writeDictionary(String... words) throws IOException {
        Path file = directory.resolve("dictionary-" + System.nanoTime() + ".txt");
        Files.write(file, List.of(words), StandardCharsets.UTF_8);
        return file;
    }
}

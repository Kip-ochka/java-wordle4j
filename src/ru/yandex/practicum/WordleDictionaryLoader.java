package ru.yandex.practicum;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;

import ru.yandex.practicum.exception.DictionaryFileException;
import ru.yandex.practicum.exception.EmptyDictionaryException;
import ru.yandex.practicum.exception.WordleApplicationException;

/**
 * Загрузчик словарей: всё, что связано с файлами и кодировками.
 * <p>
 * Файл словаря — текстовый, в кодировке UTF-8, по одному существительному в строке.
 * Слова отсортированы по алфавиту, а не по длине, поэтому файл читается целиком,
 * и уже после этого из него отбираются слова, подходящие для игры.
 * <p>
 * Результат работы загрузчика — готовый {@link WordleDictionary}.
 */
public class WordleDictionaryLoader {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Лог-файл, общий для всех классов программы. */
    private final PrintWriter log;

    public WordleDictionaryLoader(PrintWriter log) {
        if (log == null) {
            throw new IllegalArgumentException("Лог обязателен для загрузчика словарей");
        }
        this.log = log;
    }

    /**
     * Читает файл словаря и оставляет из него только слова, пригодные для игры.
     * Слова нормализуются и складываются в {@link LinkedHashSet}, поэтому
     * дубликаты, возникшие после замены «ё» на «е» и приведения регистра, отбрасываются.
     *
     * @param fileName путь к файлу словаря
     * @return словарь игровых слов
     * @throws DictionaryFileException  файл не найден или не читается
     * @throws EmptyDictionaryException в файле нет ни одного подходящего слова
     */
    public WordleDictionary load(String fileName) throws WordleApplicationException {
        writeLog("Загрузка словаря из файла " + fileName);

        Set<String> gameWords = new LinkedHashSet<>();
        int totalLines = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName, StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            while (line != null) {
                totalLines++;
                String word = WordleDictionary.normalize(line);
                if (WordleDictionary.isGameWord(word)) {
                    gameWords.add(word);
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            writeLog("Ошибка чтения словаря: " + e);
            throw new DictionaryFileException(fileName, e.getMessage(), e);
        }

        writeLog("Прочитано строк: " + totalLines + ", отобрано игровых слов: " + gameWords.size());

        if (gameWords.isEmpty()) {
            throw new EmptyDictionaryException(fileName);
        }

        return new WordleDictionary(gameWords);
    }

    private void writeLog(String message) {
        log.println(LocalDateTime.now().format(TIMESTAMP) + " [Loader] " + message);
    }
}

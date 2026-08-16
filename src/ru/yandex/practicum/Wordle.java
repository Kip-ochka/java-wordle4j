package ru.yandex.practicum;

import java.io.BufferedWriter;
import java.io.Console;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Scanner;

import ru.yandex.practicum.exception.WordleApplicationException;
import ru.yandex.practicum.exception.WordleGameException;

/**
 * Главный класс игры Wordle.
 * <p>
 * Задаёт общий ход работы программы: создаёт лог-файл, загружает словарь,
 * создаёт партию и крутит игровой цикл. Это единственный класс, который
 * работает с консолью; все служебные сообщения и стектрейсы уходят в лог-файл.
 */
public class Wordle {

    /** Файл словаря по умолчанию, лежит в корне проекта. */
    private static final String DEFAULT_DICTIONARY_FILE = "words_ru.txt";

    /** Файл журнала работы программы. */
    private static final String LOG_FILE = "wordle.log";

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        String dictionaryFile = args.length > 0 ? args[0] : DEFAULT_DICTIONARY_FILE;

        // Лог открывается через try-with-resources и закрывается при любом исходе.
        try (PrintWriter log = new PrintWriter(
                new BufferedWriter(new FileWriter(LOG_FILE, StandardCharsets.UTF_8, true)), true)) {

            // Один общий catch на всю программу: игрок не должен видеть стектрейсы.
            try (Scanner scanner = new Scanner(System.in, consoleCharset())) {
                writeLog(log, "=== Запуск программы, словарь: " + dictionaryFile + " ===");

                WordleDictionaryLoader loader = new WordleDictionaryLoader(log);
                WordleDictionary dictionary = loader.load(dictionaryFile);
                WordleGame game = new WordleGame(dictionary, log);

                printGreeting(dictionary);
                play(game, scanner);
                printResult(game);

                writeLog(log, "=== Программа завершена, состояние: " + game.getState() + " ===");
            } catch (WordleApplicationException e) {
                System.out.println("Не удалось запустить игру: " + e.getMessage());
                System.out.println("Подробности записаны в файл " + LOG_FILE);
                writeLog(log, "Ошибка приложения: " + e.getMessage());
                e.printStackTrace(log);
            } catch (Exception e) {
                System.out.println("Произошла непредвиденная ошибка, игра остановлена.");
                System.out.println("Подробности записаны в файл " + LOG_FILE);
                writeLog(log, "Непредвиденная ошибка: " + e);
                e.printStackTrace(log);
            }
        } catch (IOException e) {
            // Единственная ситуация, о которой приходится сообщать в консоль: лога ещё нет.
            System.out.println("Не удалось создать лог-файл " + LOG_FILE + ": " + e.getMessage());
        }
    }

    /**
     * Игровой цикл. Крутится, пока игра не закончится победой или исчерпанием ходов.
     * Пустая строка означает просьбу о подсказке: компьютер сам выбирает слово и делает им ход.
     */
    private static void play(WordleGame game, Scanner scanner) {
        while (!game.isFinished()) {
            System.out.print("Ход " + game.getStepNumber() + " из " + WordleGame.TOTAL_STEPS + "> ");
            if (!scanner.hasNextLine()) {
                System.out.println();
                System.out.println("Ввод закончился, игра прервана.");
                return;
            }

            String input = scanner.nextLine();
            try {
                String word = input;
                if (word.trim().isEmpty()) {
                    word = game.suggest();
                    System.out.println("Подсказка компьютера: " + word);
                }
                MoveResult result = game.makeMove(word);
                System.out.println(render(result));
            } catch (WordleGameException e) {
                // Игровая ошибка — ход не засчитан, просто просим ввести слово заново.
                System.out.println(e.getMessage());
            }
        }
    }

    private static void printGreeting(WordleDictionary dictionary) {
        StringBuilder greeting = new StringBuilder();
        greeting.append("Игра «Wordle»").append(System.lineSeparator());
        greeting.append("Загадано существительное из ").append(WordleDictionary.WORD_LENGTH)
                .append(" букв, у вас ").append(WordleGame.TOTAL_STEPS).append(" попыток.")
                .append(System.lineSeparator());
        greeting.append("Обозначения подсказки: ")
                .append(WordleDictionary.MARK_EXACT).append(" — буква на своём месте, ")
                .append(WordleDictionary.MARK_PRESENT).append(" — буква есть, но в другом месте, ")
                .append(WordleDictionary.MARK_ABSENT).append(" — такой буквы нет.")
                .append(System.lineSeparator());
        greeting.append("Слов в словаре: ").append(dictionary.size())
                .append(". Нажмите Enter на пустой строке, чтобы компьютер подсказал слово.");
        System.out.println(greeting);
    }

    /** Слово и подсказка под ним: в консоли символы одинаковой ширины, буквы совпадут по столбцам. */
    private static String render(MoveResult result) {
        StringBuilder line = new StringBuilder();
        line.append(result.getWord()).append(System.lineSeparator()).append(result.getHint());
        return line.toString();
    }

    private static void printResult(WordleGame game) {
        StringBuilder result = new StringBuilder();
        result.append(System.lineSeparator());

        if (game.getState() == GameState.WON) {
            result.append("Победа! Слово отгадано за ").append(game.getStepsMade()).append(" ход(а/ов).");
        } else if (game.getState() == GameState.LOST) {
            result.append("Попытки закончились, слово так и не отгадано.");
        } else {
            result.append("Игра прервана.");
        }
        result.append(System.lineSeparator());

        result.append("Загаданное слово: ").append(game.getAnswer()).append(System.lineSeparator());
        result.append("Ваши ходы:").append(System.lineSeparator());
        for (Map.Entry<String, String> move : game.getHistory().entrySet()) {
            result.append("  ").append(move.getKey()).append("  ").append(move.getValue())
                    .append(System.lineSeparator());
        }

        System.out.println(result);
    }

    /**
     * Кодировка, в которой консоль отдаёт введённый текст.
     * Без этого русские буквы из терминала Windows могут прочитаться неверно.
     */
    private static Charset consoleCharset() {
        Console console = System.console();
        return console != null ? console.charset() : Charset.defaultCharset();
    }

    private static void writeLog(PrintWriter log, String message) {
        log.println(LocalDateTime.now().format(TIMESTAMP) + " [Wordle] " + message);
    }
}

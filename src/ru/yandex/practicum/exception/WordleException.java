package ru.yandex.practicum.exception;

/**
 * Общий предок всех проверяемых исключений игры Wordle.
 * <p>
 * Наследники делятся на две группы:
 * {@link WordleApplicationException} — ошибки работы программы,
 * {@link WordleGameException} — игровые ситуации.
 */
public class WordleException extends Exception {

    public WordleException(String message) {
        super(message);
    }

    public WordleException(String message, Throwable cause) {
        super(message, cause);
    }
}

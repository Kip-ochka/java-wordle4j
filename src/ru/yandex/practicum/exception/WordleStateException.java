package ru.yandex.practicum.exception;

/**
 * Внутренняя ошибка игры: состояние, которого не должно возникать.
 * Например, отрицательное число ходов или потерянный из списка вариантов ответ.
 * <p>
 * Непроверяемое исключение — его не нужно обрабатывать в игровом цикле,
 * оно должно долететь до общего {@code catch} главного класса и попасть в лог.
 */
public class WordleStateException extends RuntimeException {

    public WordleStateException(String message) {
        super(message);
    }
}

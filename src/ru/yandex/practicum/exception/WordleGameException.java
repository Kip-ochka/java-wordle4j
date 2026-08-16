package ru.yandex.practicum.exception;

/**
 * Ошибки игровых ситуаций: некорректный ввод игрока, слово не из словаря и т. п.
 * Такие ошибки не прерывают игру — главный класс превращает их
 * в понятное сообщение для игрока, и ход не засчитывается.
 */
public class WordleGameException extends WordleException {

    public WordleGameException(String message) {
        super(message);
    }

    public WordleGameException(String message, Throwable cause) {
        super(message, cause);
    }
}

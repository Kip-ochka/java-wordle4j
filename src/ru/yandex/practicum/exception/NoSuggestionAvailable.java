package ru.yandex.practicum.exception;

/**
 * Компьютер не может предложить новую подсказку:
 * все подходящие слова уже были предложены.
 */
public class NoSuggestionAvailable extends WordleGameException {

    public NoSuggestionAvailable(String message) {
        super(message);
    }
}

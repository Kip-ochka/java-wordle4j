package ru.yandex.practicum.exception;

/**
 * Ошибки, связанные с работой программы, а не с игровым процессом:
 * не найден файл словаря, словарь пуст, не удалось создать лог-файл.
 * Такие ошибки пишутся в лог и завершают программу.
 */
public class WordleApplicationException extends WordleException {

    public WordleApplicationException(String message) {
        super(message);
    }

    public WordleApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}

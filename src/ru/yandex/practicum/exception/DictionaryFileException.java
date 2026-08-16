package ru.yandex.practicum.exception;

/**
 * Файл словаря не найден или не может быть прочитан.
 */
public class DictionaryFileException extends WordleApplicationException {

    private final String fileName;

    public DictionaryFileException(String fileName, String message, Throwable cause) {
        super("Не удалось прочитать файл словаря «" + fileName + "»: " + message, cause);
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}

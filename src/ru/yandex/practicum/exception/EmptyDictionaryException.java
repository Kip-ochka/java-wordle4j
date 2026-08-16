package ru.yandex.practicum.exception;

import ru.yandex.practicum.WordleDictionary;

/**
 * В файле словаря не нашлось ни одного слова, подходящего для игры.
 */
public class EmptyDictionaryException extends WordleApplicationException {

    public EmptyDictionaryException(String fileName) {
        super("В файле словаря «" + fileName + "» нет ни одного слова из "
                + WordleDictionary.WORD_LENGTH + " букв русского алфавита");
    }
}

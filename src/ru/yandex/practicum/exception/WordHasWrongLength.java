package ru.yandex.practicum.exception;

import ru.yandex.practicum.WordleDictionary;

/**
 * Длина введённого слова не равна {@link WordleDictionary#WORD_LENGTH}.
 */
public class WordHasWrongLength extends WordleGameException {

    private final String word;

    private final int actualLength;

    public WordHasWrongLength(String word) {
        super("В слове «" + word + "» " + word.length() + " букв(ы), а нужно ровно "
                + WordleDictionary.WORD_LENGTH);
        this.word = word;
        this.actualLength = word.length();
    }

    public String getWord() {
        return word;
    }

    public int getActualLength() {
        return actualLength;
    }
}

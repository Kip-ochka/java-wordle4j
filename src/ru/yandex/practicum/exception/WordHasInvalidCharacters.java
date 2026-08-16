package ru.yandex.practicum.exception;

/**
 * В слове есть символы, которых не может быть в игровом слове:
 * цифры, знаки препинания, пробелы или буквы других алфавитов.
 */
public class WordHasInvalidCharacters extends WordleGameException {

    private final String word;

    public WordHasInvalidCharacters(String word) {
        super("Слово «" + word + "» должно состоять только из букв русского алфавита");
        this.word = word;
    }

    public String getWord() {
        return word;
    }
}

package ru.yandex.practicum.exception;

/**
 * Игрок ввёл слово, которого нет в игровом словаре. Ход не засчитывается.
 */
public class WordNotFoundInDictionary extends WordleGameException {

    private final String word;

    public WordNotFoundInDictionary(String word) {
        super("Слова «" + word + "» нет в словаре, попробуйте другое");
        this.word = word;
    }

    public String getWord() {
        return word;
    }
}

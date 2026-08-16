package ru.yandex.practicum;

/**
 * Результат одного засчитанного хода: что было названо, какая получилась подсказка
 * и в каком состоянии оказалась игра после хода.
 */
public class MoveResult {

    private final String word;

    private final String hint;

    private final int stepsLeft;

    private final GameState state;

    public MoveResult(String word, String hint, int stepsLeft, GameState state) {
        this.word = word;
        this.hint = hint;
        this.stepsLeft = stepsLeft;
        this.state = state;
    }

    /** Нормализованное слово игрока. */
    public String getWord() {
        return word;
    }

    /** Строка-подсказка из символов «+», «^» и «-». */
    public String getHint() {
        return hint;
    }

    /** Сколько ходов осталось после этого хода. */
    public int getStepsLeft() {
        return stepsLeft;
    }

    /** Состояние игры после хода. */
    public GameState getState() {
        return state;
    }

    /** Отгадано ли слово этим ходом. */
    public boolean isWin() {
        return state == GameState.WON;
    }

    @Override
    public String toString() {
        return word + " -> " + hint + " (осталось ходов: " + stepsLeft + ", состояние: " + state + ")";
    }
}

package battleship;

/**
 * A finished game stored in the scoreboard.
 * <p>
 * The date is kept as an ISO-8601 string (yyyy-MM-dd) so that it is stored
 * in the JSON file as plain text. Hit and miss rates are not stored,
 * they are computed from these values when the scoreboard is shown.
 *
 * @param date       the day the game was played (ISO-8601)
 * @param result     {@link #VICTORY}, {@link #DEFEAT} or {@link #ABANDONED}
 * @param moves      the number of bursts fired
 * @param hits       valid shots that hit a ship
 * @param misses     valid shots that fell in the water
 * @param repeated   shots at positions already hit
 * @param outside    shots outside the board
 */
public record ScoreboardEntry(String date, String result, int moves,
                              int hits, int misses, int repeated, int outside) {

    /** Result of a game that was won. */
    public static final String VICTORY = "VITORIA";

    /** Result of a game that was lost. */
    public static final String DEFEAT = "DERROTA";

    /** Result of a game that was left unfinished. */
    public static final String ABANDONED = "ABANDONADO";

    /**
     * Valid shots are the ones that hit a ship or fell in the water.
     *
     * @return the number of valid shots
     */
    public int validShots() {
        return hits + misses;
    }
}
package battleship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Scoreboard}.
 */
public class ScoreboardTest {

    @TempDir
    Path dir;

    private Path file;

    /** Uses a fresh temporary file for each test. */
    @BeforeEach
    void setUp() {
        file = dir.resolve("scoreboard.json");
    }

    private static ScoreboardEntry entry(String result, int moves, int hits, int misses) {
        return new ScoreboardEntry("2026-10-17", result, moves, hits, misses, 0, 0);
    }

    /** A missing file is an empty scoreboard. */
    @Test
    void loadMissingFileReturnsEmptyList() {
        assertTrue(new Scoreboard(file).load().isEmpty());
    }

    /** Entries survive a new Scoreboard instance, like a new program execution. */
    @Test
    void entriesArePersistedBetweenInstances() {
        ScoreboardEntry e = entry(ScoreboardEntry.VICTORY, 14, 27, 15);
        assertTrue(new Scoreboard(file).add(e));

        List<ScoreboardEntry> loaded = new Scoreboard(file).load();
        assertEquals(List.of(e), loaded);
    }

    /** The best games have the fewest moves and the list respects the limit. */
    @Test
    void topIsOrderedByFewestMovesAndLimited() {
        Scoreboard sb = new Scoreboard(file);
        sb.add(entry(ScoreboardEntry.VICTORY, 30, 27, 40));
        sb.add(entry(ScoreboardEntry.VICTORY, 14, 27, 15));
        sb.add(entry(ScoreboardEntry.VICTORY, 22, 27, 30));

        List<ScoreboardEntry> top = sb.top(2);
        assertEquals(2, top.size());
        assertEquals(14, top.get(0).moves());
        assertEquals(22, top.get(1).moves());
    }

    /** Games that were not won do not appear in the ranking. */
    @Test
    void topIgnoresGamesThatWereNotWon() {
        Scoreboard sb = new Scoreboard(file);
        sb.add(entry(ScoreboardEntry.ABANDONED, 3, 1, 8));
        sb.add(entry(ScoreboardEntry.VICTORY, 20, 27, 20));

        assertEquals(1, sb.top(10).size());
    }

    /** Rates are computed over valid shots, win rate over registered games. */
    @Test
    void statsComputeRates() {
        Scoreboard sb = new Scoreboard(file);
        sb.add(entry(ScoreboardEntry.VICTORY, 14, 27, 15));
        sb.add(entry(ScoreboardEntry.DEFEAT, 19, 27, 30));

        Scoreboard.Stats stats = sb.stats();
        assertEquals(2, stats.games());
        assertEquals(1, stats.wins());
        assertEquals(50.0, stats.winRate(), 0.001);
        assertEquals(100.0 * 54 / 99, stats.hitRate(), 0.001);
        assertEquals(100.0 * 45 / 99, stats.missRate(), 0.001);
        assertEquals(100.0, stats.hitRate() + stats.missRate(), 0.001);
    }

    /** An empty scoreboard must not divide by zero. */
    @Test
    void statsOfEmptyScoreboardAreZero() {
        Scoreboard.Stats stats = new Scoreboard(file).stats();
        assertEquals(0, stats.games());
        assertEquals(0.0, stats.hitRate(), 0.001);
        assertEquals(0.0, stats.missRate(), 0.001);
        assertEquals(0.0, stats.winRate(), 0.001);
    }
}
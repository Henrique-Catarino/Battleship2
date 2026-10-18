package battleship;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Keeps the results of past games in a JSON file (read and written with Gson)
 * and shows the best games plus global statistics.
 */
public class Scoreboard {

    /** Default location of the scoreboard file. */
    public static final String DEFAULT_FILE = "data/scoreboard.json";

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Type LIST_TYPE = new TypeToken<List<ScoreboardEntry>>() {}.getType();
    private static final Locale PT = Locale.of("pt", "PT");

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;

    /**
     * Global statistics over all the games in the scoreboard.
     * Hit and miss rates are computed over valid shots only.
     *
     * @param games  number of games registered
     * @param wins   number of games won
     * @param hits   total valid shots that hit a ship
     * @param misses total valid shots that fell in the water
     */
    public record Stats(int games, int wins, int hits, int misses) {

        /** @return hits as a percentage of valid shots, or 0 if there are none */
        public double hitRate() {
            int valid = hits + misses;
            return valid == 0 ? 0 : 100.0 * hits / valid;
        }

        /** @return misses as a percentage of valid shots, or 0 if there are none */
        public double missRate() {
            int valid = hits + misses;
            return valid == 0 ? 0 : 100.0 * misses / valid;
        }

        /** @return wins as a percentage of registered games, or 0 if there are none */
        public double winRate() {
            return games == 0 ? 0 : 100.0 * wins / games;
        }
    }

    /** Creates a scoreboard that uses {@link #DEFAULT_FILE}. */
    public Scoreboard() {
        this(Path.of(DEFAULT_FILE));
    }

    /**
     * Creates a scoreboard that uses the given file.
     *
     * @param file the JSON file where entries are stored
     */
    public Scoreboard(Path file) {
        this.file = file;
    }

    /**
     * Reads all entries from the file.
     *
     * @return the stored entries, or an empty list if the file is missing or unreadable
     */
    public List<ScoreboardEntry> load() {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            List<ScoreboardEntry> entries = gson.fromJson(json, LIST_TYPE);
            return entries != null ? new ArrayList<>(entries) : new ArrayList<>();
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.warn("Could not read scoreboard {}: {}", file, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Appends an entry and saves the file.
     *
     * @param entry the game to register
     * @return true if the entry was saved
     */
    public boolean add(ScoreboardEntry entry) {
        List<ScoreboardEntry> entries = load();
        entries.add(entry);
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, gson.toJson(entries, LIST_TYPE), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            LOGGER.error("Could not save scoreboard {}: {}", file, e.getMessage());
            return false;
        }
    }

    /**
     * Registers a game played today.
     *
     * @param result   one of the result constants in {@link ScoreboardEntry}
     * @param moves    number of bursts fired
     * @param hits     valid shots that hit a ship
     * @param misses   valid shots that fell in the water
     * @param repeated shots at positions already hit
     * @param outside  shots outside the board
     * @return true if the entry was saved
     */
    public boolean record(String result, int moves, int hits, int misses, int repeated, int outside) {
        return add(new ScoreboardEntry(LocalDate.now().toString(), result,
                moves, hits, misses, repeated, outside));
    }

    /**
     * Returns the best games: only victories, ordered by the fewest moves.
     *
     * @param n maximum number of games to return
     * @return the best games, best first
     */
    public List<ScoreboardEntry> top(int n) {
        return load().stream()
                .filter(e -> ScoreboardEntry.VICTORY.equals(e.result()))
                .sorted(Comparator.comparingInt(ScoreboardEntry::moves))
                .limit(n)
                .toList();
    }

    /**
     * Computes the global statistics over all stored games.
     *
     * @return the statistics
     */
    public Stats stats() {
        List<ScoreboardEntry> entries = load();
        int wins = 0;
        int hits = 0;
        int misses = 0;
        for (ScoreboardEntry e : entries) {
            if (ScoreboardEntry.VICTORY.equals(e.result())) {
                wins++;
            }
            hits += e.hits();
            misses += e.misses();
        }
        return new Stats(entries.size(), wins, hits, misses);
    }

    /**
     * Prints the best games and the global statistics to the console.
     *
     * @param n maximum number of games to list
     */
    public void print(int n) {
        Stats stats = stats();
        System.out.println("=============== SCOREBOARD ===============");
        if (stats.games() == 0) {
            System.out.println("Ainda não há jogos registados.");
            System.out.println("==========================================");
            return;
        }
        System.out.printf(PT, "%-3s %-11s %8s %8s %6s  %s%n",
                "#", "Data", "Jogadas", "Acertos", "Água", "Resultado");
        int rank = 1;
        for (ScoreboardEntry e : top(n)) {
            System.out.printf(PT, "%-3d %-11s %8d %8d %6d  %s%n",
                    rank++, e.date(), e.moves(), e.hits(), e.misses(), e.result());
        }
        System.out.printf(PT, "%nJogos: %d | Taxa de vitória: %.1f%%%n",
                stats.games(), stats.winRate());
        System.out.printf(PT, "Taxa de acerto: %.1f%% | Taxa de tiros na água: %.1f%%%n",
                stats.hitRate(), stats.missRate());
        System.out.println("==========================================");
    }
}
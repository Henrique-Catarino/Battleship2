package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoreboardCoverageTest {

    @TempDir
    Path tempDir;

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream captured;
    private Path file;
    private Scoreboard board;

    @BeforeEach
    void setUp() {
        file = tempDir.resolve("scoreboard.json");
        board = new Scoreboard(file);
        captured = new ByteArrayOutputStream();
    }

    @AfterEach
    void restoreOut() {
        System.setOut(originalOut);
    }

    private static ScoreboardEntry entry(String date, String result, int moves, int hits, int misses) {
        return new ScoreboardEntry(date, result, moves, hits, misses, 0, 0);
    }

    private String capturePrint(int n) {
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        board.print(n);
        System.out.flush();
        return captured.toString(StandardCharsets.UTF_8);
    }

    // ---------- construtores ----------

    @Test
    @DisplayName("O construtor sem argumentos usa o ficheiro por omissão")
    void defaultConstructorUsesDefaultFile() {
        assertEquals("data/scoreboard.json", Scoreboard.DEFAULT_FILE);
        assertNotNull(new Scoreboard());
    }

    // ---------- load ----------

    @Test
    @DisplayName("load devolve lista vazia se o ficheiro não existe")
    void loadReturnsEmptyWhenFileMissing() {
        assertTrue(board.load().isEmpty());
    }

    @Test
    @DisplayName("load devolve lista vazia se o ficheiro está vazio")
    void loadReturnsEmptyWhenFileIsEmpty() throws IOException {
        Files.writeString(file, "");
        assertTrue(board.load().isEmpty());
    }

    @Test
    @DisplayName("load devolve lista vazia se o JSON é inválido")
    void loadReturnsEmptyWhenJsonIsInvalid() throws IOException {
        Files.writeString(file, "{\"not\": \"a list\"}");
        assertTrue(board.load().isEmpty());
    }

    @Test
    @DisplayName("load devolve lista vazia se não consegue ler o ficheiro")
    void loadReturnsEmptyWhenFileCannotBeRead() {
        // um diretório existe, mas não pode ser lido como ficheiro
        Scoreboard directoryBoard = new Scoreboard(tempDir);
        assertTrue(directoryBoard.load().isEmpty());
    }

    // ---------- add ----------

    @Test
    @DisplayName("add guarda a entrada e load devolve-a")
    void addThenLoadRoundTrip() {
        ScoreboardEntry e = new ScoreboardEntry("2026-11-22", ScoreboardEntry.VICTORY, 10, 7, 13, 2, 1);
        assertTrue(board.add(e));
        assertTrue(Files.exists(file));
        assertEquals(List.of(e), board.load());
    }

    @Test
    @DisplayName("add acrescenta às entradas existentes, pela mesma ordem")
    void addAppendsToExistingEntries() {
        ScoreboardEntry first = entry("2026-01-01", ScoreboardEntry.VICTORY, 10, 5, 5);
        ScoreboardEntry second = entry("2026-01-02", ScoreboardEntry.DEFEAT, 20, 3, 9);
        assertTrue(board.add(first));
        assertTrue(board.add(second));
        assertEquals(List.of(first, second), board.load());
    }

    @Test
    @DisplayName("add cria as diretorias que faltam")
    void addCreatesMissingParentDirectories() {
        Path nested = tempDir.resolve("a").resolve("b").resolve("scoreboard.json");
        Scoreboard nestedBoard = new Scoreboard(nested);
        assertTrue(nestedBoard.add(entry("2026-01-01", ScoreboardEntry.VICTORY, 10, 5, 5)));
        assertTrue(Files.exists(nested));
    }

    @Test
    @DisplayName("add funciona com um caminho sem diretoria pai")
    void addWorksWithPathWithoutParent() throws IOException {
        Path noParent = Path.of("scoreboard-no-parent-test.json");
        try {
            Scoreboard relativeBoard = new Scoreboard(noParent);
            assertTrue(relativeBoard.add(entry("2026-01-01", ScoreboardEntry.VICTORY, 10, 5, 5)));
            assertEquals(1, relativeBoard.load().size());
        } finally {
            Files.deleteIfExists(noParent);
        }
    }

    @Test
    @DisplayName("add devolve false se não consegue guardar o ficheiro")
    void addReturnsFalseWhenSaveFails() throws IOException {
        Path blocker = tempDir.resolve("blocker");
        Files.writeString(blocker, "isto é um ficheiro, não uma diretoria");
        Scoreboard failing = new Scoreboard(blocker.resolve("scoreboard.json"));
        assertFalse(failing.add(entry("2026-01-01", ScoreboardEntry.VICTORY, 10, 5, 5)));
    }

    // ---------- record ----------

    @Test
    @DisplayName("record regista um jogo com a data de hoje")
    void recordStoresGameWithTodaysDate() {
        assertTrue(board.record(new GameRecord(ScoreboardEntry.VICTORY, 12, 8, 20, 1, 2)));
        List<ScoreboardEntry> entries = board.load();
        assertEquals(1, entries.size());
        ScoreboardEntry e = entries.get(0);
        assertTrue(e.date().matches("\\d{4}-\\d{2}-\\d{2}"));
        assertEquals(ScoreboardEntry.VICTORY, e.result());
        assertEquals(12, e.moves());
        assertEquals(8, e.hits());
        assertEquals(20, e.misses());
        assertEquals(1, e.repeated());
        assertEquals(2, e.outside());
    }

    // ---------- top ----------

    @Test
    @DisplayName("top só devolve vitórias, ordenadas por menos jogadas")
    void topReturnsOnlyVictoriesSortedByMoves() {
        board.add(entry("2026-01-01", ScoreboardEntry.VICTORY, 30, 10, 20));
        board.add(entry("2026-01-02", ScoreboardEntry.DEFEAT, 2, 1, 1));
        board.add(entry("2026-01-03", ScoreboardEntry.VICTORY, 15, 12, 8));
        board.add(entry("2026-01-04", ScoreboardEntry.ABANDONED, 1, 0, 1));
        board.add(entry("2026-01-05", ScoreboardEntry.VICTORY, 20, 11, 9));

        List<ScoreboardEntry> top = board.top(10);
        assertEquals(3, top.size());
        assertEquals(15, top.get(0).moves());
        assertEquals(20, top.get(1).moves());
        assertEquals(30, top.get(2).moves());
    }

    @Test
    @DisplayName("top respeita o limite pedido")
    void topRespectsLimit() {
        board.add(entry("2026-01-01", ScoreboardEntry.VICTORY, 30, 10, 20));
        board.add(entry("2026-01-02", ScoreboardEntry.VICTORY, 15, 12, 8));
        assertEquals(1, board.top(1).size());
        assertEquals(15, board.top(1).get(0).moves());
        assertTrue(board.top(0).isEmpty());
    }

    @Test
    @DisplayName("top devolve lista vazia se não há vitórias")
    void topIsEmptyWithoutVictories() {
        board.add(entry("2026-01-01", ScoreboardEntry.DEFEAT, 30, 10, 20));
        assertTrue(board.top(5).isEmpty());
    }

    // ---------- stats ----------

    @Test
    @DisplayName("stats de um scoreboard vazio são todas zero")
    void statsOfEmptyScoreboardAreZero() {
        Scoreboard.Stats stats = board.stats();
        assertEquals(new Scoreboard.Stats(0, 0, 0, 0), stats);
        assertEquals(0.0, stats.hitRate());
        assertEquals(0.0, stats.missRate());
        assertEquals(0.0, stats.winRate());
    }

    @Test
    @DisplayName("stats soma jogos, vitórias, acertos e falhas")
    void statsSumsGamesWinsHitsAndMisses() {
        board.add(entry("2026-01-01", ScoreboardEntry.VICTORY, 30, 10, 20));
        board.add(entry("2026-01-02", ScoreboardEntry.DEFEAT, 40, 5, 30));
        board.add(entry("2026-01-03", ScoreboardEntry.VICTORY, 15, 12, 8));
        assertEquals(new Scoreboard.Stats(3, 2, 27, 58), board.stats());
    }

    @Test
    @DisplayName("Stats calcula as taxas em percentagem")
    void statsComputesRates() {
        Scoreboard.Stats stats = new Scoreboard.Stats(4, 1, 25, 75);
        assertEquals(25.0, stats.hitRate(), 0.0001);
        assertEquals(75.0, stats.missRate(), 0.0001);
        assertEquals(25.0, stats.winRate(), 0.0001);
    }

    // ---------- print ----------

    @Test
    @DisplayName("print indica que não há jogos quando o scoreboard está vazio")
    void printShowsMessageWhenEmpty() {
        String out = capturePrint(5);
        assertTrue(out.contains("SCOREBOARD"));
        assertTrue(out.contains("registados"));
        assertFalse(out.contains("Jogadas"));
    }

    @Test
    @DisplayName("print mostra as melhores vitórias e as estatísticas")
    void printShowsBestGamesAndStats() {
        board.add(entry("2026-01-01", ScoreboardEntry.VICTORY, 30, 10, 20));
        board.add(entry("2026-02-02", ScoreboardEntry.VICTORY, 15, 12, 8));
        board.add(entry("2026-03-03", ScoreboardEntry.DEFEAT, 40, 5, 30));
        board.add(entry("2026-04-04", ScoreboardEntry.ABANDONED, 3, 1, 2));

        String out = capturePrint(10);
        assertTrue(out.contains("Jogadas"));
        assertTrue(out.contains("2026-01-01"));
        assertTrue(out.contains("2026-02-02"));
        assertTrue(out.contains(ScoreboardEntry.VICTORY));
        assertTrue(out.contains("Jogos: 4"));
        assertFalse(out.contains("2026-03-03"), "derrotas não entram na lista");
    }

    @Test
    @DisplayName("print lista só as n melhores vitórias")
    void printListsOnlyTopN() {
        board.add(entry("2026-01-01", ScoreboardEntry.VICTORY, 30, 10, 20));
        board.add(entry("2026-02-02", ScoreboardEntry.VICTORY, 15, 12, 8));

        String out = capturePrint(1);
        assertTrue(out.contains("2026-02-02"));
        assertFalse(out.contains("2026-01-01"));
    }
}
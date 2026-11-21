package battleship;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Test class for Move.
 * Author: Henrique Catarino (129799)
 * Date: 2026-11-21
 * Cyclomatic Complexity for each method:
 * - Constructor: 1
 * - toString: 1
 * - getNumber: 1
 * - getShots: 1
 * - getShotResults: 1
 * - processEnemyFire: alta (muitos ramos if/else/ternários)
 */
public class MoveTest {

    private PrintStream originalOut;
    private ByteArrayOutputStream outContent;

    private IShip barge1;
    private IShip barge2;
    private IShip caravel;

    @BeforeEach
    void setUp() {
        // Captura o System.out para poder verificar a mensagem "Jogada nº..." e o JSON
        originalOut = System.out;
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));

        barge1 = new Barge(Compass.NORTH, new Position(1, 1));
        barge2 = new Barge(Compass.NORTH, new Position(5, 5));
        caravel = new Caravel(Compass.NORTH, new Position(8, 8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    /**
     * Único sítio onde se constrói um ShotResult.
     * Se o record IGame.ShotResult tiver os componentes noutra ordem, é só ajustar aqui.
     * Ordem assumida: (valid, repeated, ship, sunk).
     */
    private IGame.ShotResult shot(boolean valid, boolean repeated, IShip ship, boolean sunk) {
        return new IGame.ShotResult(valid, repeated, ship, sunk);
    }

    private IGame.ShotResult miss() {
        return shot(true, false, null, false);
    }

    private IGame.ShotResult hit(IShip ship) {
        return shot(true, false, ship, false);
    }

    private IGame.ShotResult sunk(IShip ship) {
        return shot(true, false, ship, true);
    }

    private IGame.ShotResult repeated() {
        return shot(true, true, null, false);
    }

    private IGame.ShotResult invalid() {
        return shot(false, false, null, false);
    }

    private List<IGame.ShotResult> list(IGame.ShotResult... results) {
        return new ArrayList<>(Arrays.asList(results));
    }

    private Move moveWith(List<IGame.ShotResult> results) {
        return new Move(1, new ArrayList<>(), results);
    }

    private String output() {
        return outContent.toString(StandardCharsets.UTF_8);
    }

    /** Devolve o texto que vem depois de "Jogada nº... -> " na linha impressa em modo verbose. */
    private String summary(List<IGame.ShotResult> results) {
        moveWith(results).processEnemyFire(true);
        String out = output();
        int start = out.indexOf("-> ") + 3;
        int end = out.indexOf(System.lineSeparator(), start);
        return out.substring(start, end);
    }

    // ------------------------------------------------------------------
    // Construtor, getters e toString
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Construtor e getters devolvem os valores recebidos")
    void testConstructorAndGetters() {
        List<IPosition> shots = new ArrayList<>(List.of(new Position(1, 1), new Position(2, 2)));
        List<IGame.ShotResult> results = list(miss(), hit(barge1));

        Move move = new Move(7, shots, results);

        assertEquals(7, move.getNumber());
        assertSame(shots, move.getShots());
        assertSame(results, move.getShotResults());
    }

    @Test
    @DisplayName("toString mostra número, nº de tiros e nº de resultados")
    void testToString() {
        List<IPosition> shots = new ArrayList<>(List.of(new Position(1, 1), new Position(2, 2)));
        Move move = new Move(3, shots, list(miss(), miss(), miss()));

        assertEquals("Move{number=3, shots=2, results=3}", move.toString());
    }

    // ------------------------------------------------------------------
    // processEnemyFire: JSON devolvido
    // ------------------------------------------------------------------

    @Test
    @DisplayName("processEnemyFire devolve JSON com todas as contagens e listas de barcos")
    void testProcessEnemyFireJson() throws Exception {
        List<IGame.ShotResult> results = list(sunk(barge1), hit(caravel), miss(), repeated(), invalid());

        String json = moveWith(results).processEnemyFire(false);
        JsonNode root = new ObjectMapper().readTree(json);

        assertEquals(3, root.get("validShots").asInt());
        assertEquals(1, root.get("repeatedShots").asInt());
        assertEquals(1, root.get("missedShots").asInt());
        assertEquals(Game.NUMBER_SHOTS - 3 - 1, root.get("outsideShots").asInt());

        JsonNode sunkBoats = root.get("sunkBoats");
        assertEquals(1, sunkBoats.size());
        assertEquals(barge1.getCategory(), sunkBoats.get(0).get("type").asText());
        assertEquals(1, sunkBoats.get(0).get("count").asInt());

        // Só aparecem os barcos atingidos que NÃO foram afundados
        JsonNode hitsOnBoats = root.get("hitsOnBoats");
        assertEquals(1, hitsOnBoats.size());
        assertEquals(caravel.getCategory(), hitsOnBoats.get(0).get("type").asText());
        assertEquals(1, hitsOnBoats.get(0).get("hits").asInt());
    }

    @Test
    @DisplayName("processEnemyFire ignora tiros inválidos")
    void testProcessEnemyFireInvalidIgnored() throws Exception {
        String json = moveWith(list(invalid(), invalid())).processEnemyFire(false);
        JsonNode root = new ObjectMapper().readTree(json);

        assertEquals(0, root.get("validShots").asInt());
        assertEquals(0, root.get("repeatedShots").asInt());
        assertEquals(0, root.get("missedShots").asInt());
        assertEquals(Game.NUMBER_SHOTS, root.get("outsideShots").asInt());
        assertEquals(0, root.get("sunkBoats").size());
        assertEquals(0, root.get("hitsOnBoats").size());
    }

    @Test
    @DisplayName("processEnemyFire sem verbose não imprime a mensagem da jogada")
    void testProcessEnemyFireNotVerbose() {
        String json = moveWith(list(miss())).processEnemyFire(false);

        assertNotNull(json);
        assertFalse(output().contains("Jogada"), "Error: non-verbose mode should not print the move summary.");
        assertTrue(output().contains("JSON PARA O LLM"), "Error: the JSON block is always printed.");
    }

    @Test
    @DisplayName("processEnemyFire em modo verbose imprime a mensagem da jogada")
    void testProcessEnemyFireVerbosePrints() {
        moveWith(list(miss())).processEnemyFire(true);

        assertTrue(output().contains("Jogada"), "Error: verbose mode should print the move summary.");
    }

    // ------------------------------------------------------------------
    // processEnemyFire: mensagem verbose
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Verbose: sem resultados, só mostra tiros exteriores")
    void testVerboseNoResults() {
        String s = summary(list());

        assertTrue(s.contains("exterior"));
        assertFalse(s.startsWith(","), "Error: there should be no leading separator.");
    }

    @Test
    @DisplayName("Verbose: um tiro repetido (singular)")
    void testVerboseOneRepeated() {
        assertTrue(summary(list(repeated())).startsWith("1 tiro repetido"));
    }

    @Test
    @DisplayName("Verbose: vários tiros repetidos (plural)")
    void testVerboseManyRepeated() {
        assertTrue(summary(list(repeated(), repeated())).startsWith("2 tiros repetidos"));
    }

    @Test
    @DisplayName("Verbose: um tiro na água (singular)")
    void testVerboseOneMiss() {
        assertTrue(summary(list(miss())).startsWith("1 tiro: 1 tiro na água"));
    }

    @Test
    @DisplayName("Verbose: vários tiros na água (plural)")
    void testVerboseManyMisses() {
        assertTrue(summary(list(miss(), miss())).startsWith("2 tiros: 2 tiros na água"));
    }

    @Test
    @DisplayName("Verbose: um acerto num barco (singular)")
    void testVerboseOneHit() {
        String expected = "1 tiro: 1 tiro num(a) " + barge1.getCategory();
        assertTrue(summary(list(hit(barge1))).startsWith(expected));
    }

    @Test
    @DisplayName("Verbose: vários acertos no mesmo tipo de barco (plural)")
    void testVerboseManyHitsSameBoat() {
        String expected = "2 tiros: 2 tiros num(a) " + barge1.getCategory();
        assertTrue(summary(list(hit(barge1), hit(barge2))).startsWith(expected));
    }

    @Test
    @DisplayName("Verbose: um barco afundado (singular)")
    void testVerboseOneSunk() {
        String expected = "1 tiro: 1 " + barge1.getCategory() + " ao fundo";
        assertTrue(summary(list(sunk(barge1))).startsWith(expected));
    }

    @Test
    @DisplayName("Verbose: vários barcos do mesmo tipo afundados (plural)")
    void testVerboseManySunkSameType() {
        String expected = "2 tiros: 2 " + barge1.getCategory() + "s ao fundo";
        assertTrue(summary(list(sunk(barge1), sunk(barge2))).startsWith(expected));
    }

    @Test
    @DisplayName("Verbose: barco afundado e outro só atingido, mais um tiro na água")
    void testVerboseSunkHitAndMiss() {
        String s = summary(list(sunk(barge1), hit(caravel), miss()));

        assertTrue(s.startsWith("3 tiros: "));
        assertTrue(s.contains("1 " + barge1.getCategory() + " ao fundo"));
        assertTrue(s.contains("1 tiro num(a) " + caravel.getCategory()));
        assertTrue(s.contains("1 tiro na água"));
    }

    @Test
    @DisplayName("Verbose: tiro na água seguido de um tiro repetido")
    void testVerboseMissAndOneRepeated() {
        assertTrue(summary(list(miss(), repeated())).startsWith("1 tiro: 1 tiro na água, 1 tiro repetido"));
    }

    @Test
    @DisplayName("Verbose: tiro na água seguido de vários tiros repetidos")
    void testVerboseMissAndManyRepeated() {
        assertTrue(summary(list(miss(), repeated(), repeated()))
                .startsWith("1 tiro: 1 tiro na água, 2 tiros repetidos"));
    }

    @Test
    @DisplayName("Verbose: acerto sem tiros na água, seguido de tiro repetido")
    void testVerboseHitAndRepeated() {
        String s = summary(list(hit(barge1), repeated()));

        assertTrue(s.startsWith("1 tiro: 1 tiro num(a) " + barge1.getCategory()));
        assertTrue(s.contains("1 tiro repetido"));
    }

    @Test
    @DisplayName("Verbose: tiros inválidos não aparecem no resumo")
    void testVerboseInvalidIgnored() {
        String s = summary(list(miss(), invalid()));

        assertTrue(s.startsWith("1 tiro: 1 tiro na água"));
        assertFalse(s.contains("repetido"));
    }

    @Test
    @DisplayName("Verbose: exatamente um tiro exterior (singular)")
    void testVerboseOneOutsideShot() {
        List<IGame.ShotResult> results = new ArrayList<>();
        for (int i = 0; i < Game.NUMBER_SHOTS - 1; i++) {
            results.add(miss());
        }

        String s = summary(results);

        assertTrue(s.endsWith(", 1 tiro exterior"), "Error: summary was: " + s);
        assertFalse(s.contains("exteriores"));
    }

    @Test
    @DisplayName("Verbose: vários tiros exteriores (plural)")
    void testVerboseManyOutsideShots() {
        Assumptions.assumeTrue(Game.NUMBER_SHOTS >= 3, "Precisa de pelo menos 3 tiros por jogada");

        // Um único tiro na água deixa NUMBER_SHOTS - 1 (>= 2) tiros exteriores
        String s = summary(list(miss()));

        assertTrue(s.contains("exteriores"), "Error: summary was: " + s);
    }

    @Test
    @DisplayName("Verbose: sem tiros exteriores quando todos os tiros foram dados")
    void testVerboseNoOutsideShots() {
        List<IGame.ShotResult> results = new ArrayList<>();
        for (int i = 0; i < Game.NUMBER_SHOTS; i++) {
            results.add(miss());
        }

        assertFalse(summary(results).contains("exterior"));
    }
}

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
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Test class for Game.
 * Author: Henrique Catarino (129799)
 * Date: 2026-11-21
 * Cyclomatic Complexity for each method:
 * - Game (constructor): 1
 * - printBoard: 3
 * - printShips (private): 4
 * - printBoardShots (private): 7
 * - printBoardFrame (private): 4
 * - jsonShots: 3
 * - randomEnemyFire: 9
 * - readEnemyFire: 7
 * - fireShots: 3
 * - fireSingleShot: 6
 * - getters (getMyFleet, getAlienFleet, getMyMoves, getAlienMoves): 1
 * - getRepeatedShots, getInvalidShots, getHits, getSunkShips, getRemainingShips: 1
 * - repeatedShot: 3
 * - printMyBoard, printAlienBoard, over: 1
 */
public class GameTest {

	private Game game;
	private PrintStream originalOut;
	private ByteArrayOutputStream outContent;

	@BeforeEach
	void setUp() {
		game = new Game(new Fleet()); // Assuming Fleet is a concrete implementation of IFleet
		// Captura o System.out para poder verificar o que os métodos print* escrevem
		originalOut = System.out;
		outContent = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
	}

	@AfterEach
	void tearDown() {
		System.setOut(originalOut);
		game = null;
	}

	// ------------------------------------------------------------------
	// Auxiliares
	// ------------------------------------------------------------------

	private String output() {
		return outContent.toString(StandardCharsets.UTF_8);
	}

	/** Devolve o caracter desenhado no tabuleiro impresso, na linha e coluna indicadas (0-9). */
	private char cell(int row, int col) {
		String label = " " + new Position(row, 0).getClassicRow() + " |";
		for (String line : output().split("\\R")) {
			if (line.startsWith(label)) {
				return line.charAt(5 + 2 * col);
			}
		}
		fail("Row " + row + " not found in the printed board:\n" + output());
		return '?';
	}

	/** Lista de moves (IMove) com um único move que contém os tiros indicados. */
	private List<IMove> movesOf(IPosition... shots) {
		List<IMove> moves = new ArrayList<>();
		moves.add(new Move(1, new ArrayList<>(Arrays.asList(shots)), new ArrayList<>()));
		return moves;
	}

	/** Todas as posições do tabuleiro, exceto as indicadas. */
	private List<IPosition> allPositionsExcept(IPosition... free) {
		List<IPosition> freeList = Arrays.asList(free);
		List<IPosition> blocked = new ArrayList<>();
		for (int r = 0; r < Game.BOARD_SIZE; r++) {
			for (int c = 0; c < Game.BOARD_SIZE; c++) {
				Position p = new Position(r, c);
				if (!freeList.contains(p)) {
					blocked.add(p);
				}
			}
		}
		return blocked;
	}

	/** Bloqueia, num move antigo do adversário, todas as posições exceto as indicadas. */
	private void blockAllExcept(IPosition... free) {
		game.getAlienMoves().add(new Move(1, allPositionsExcept(free), new ArrayList<>()));
	}

	// ------------------------------------------------------------------
	// Testes originais
	// ------------------------------------------------------------------

	@Test
	@DisplayName("Construtor inicia o jogo com listas vazias e contadores a zero")
	void constructor() {
		assertNotNull(game, "Game instance should not be null after construction.");
		assertNotNull(game.getAlienMoves(), "Shots list should not be null after initialization.");
		assertTrue(game.getAlienMoves().isEmpty(), "Shots list should be empty upon initialization.");
		assertEquals(0, game.getInvalidShots(), "Invalid shots count should be zero upon initialization.");
		assertEquals(0, game.getRepeatedShots(), "Repeated shots count should be zero upon initialization.");
		assertEquals(0, game.getHits(), "Hits count should be zero upon initialization.");
		assertEquals(0, game.getSunkShips(), "Sunk ships count should be zero upon initialization.");
	}

	@Test
	@DisplayName("fireSingleShot conta um tiro fora do tabuleiro como inválido")
	void fire2() {
		Position invalidPosition = new Position(-1, 5);
		game.fireSingleShot(invalidPosition, false);
		assertEquals(1, game.getInvalidShots(), "Invalid shots counter should increase for an invalid shot.");
	}

	@Test
	@DisplayName("fireSingleShot conta um tiro repetido")
	void fire3() {
		Position position = new Position(2, 3);
		game.fireSingleShot(position, false);
		game.fireSingleShot(position, true);
		assertEquals(1, game.getRepeatedShots(), "Repeated shots counter should increase for a repeated shot.");
	}

	@Test
	@DisplayName("repeatedShot é true para uma posição já disparada")
	void repeatedShot1() {
		List<IPosition> positions = List.of(new Position(2, 3), new Position(2, 4), new Position(2, 5));
		game.fireShots(positions);
		Position position = new Position(2, 3);
		assertTrue(game.repeatedShot(position), "Position (2,3) should be marked as repeated after firing.");
	}

	@Test
	@DisplayName("repeatedShot é false para uma posição nunca disparada")
	void repeatedShot2() {
		Position position = new Position(2, 3);
		assertFalse(game.repeatedShot(position), "Position (2,3) should not be marked as repeated before firing.");
	}

	@Test
	@DisplayName("fireShots regista um move do adversário")
	void getAlienMoves() {
		List<IPosition> positions = List.of(new Position(2, 3), new Position(2, 4), new Position(2, 5));
		game.fireShots(positions);
		assertEquals(1, game.getAlienMoves().size(), "Shots list should contain one shot after firing once.");
	}

	@Test
	@DisplayName("getRemainingShips conta os navios que ainda flutuam")
	void getRemainingShips() {
		IFleet fleet = game.getMyFleet();
		Ship ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		Ship ship2 = new Frigate(Compass.EAST, new Position(5, 5));

		fleet.addShip(ship1);
		assertEquals(1, game.getRemainingShips(), "Just one ship was created!");
		fleet.addShip(ship2);
		assertEquals(2, game.getRemainingShips(), "Two ships were created!");
		ship2.sink();
		assertEquals(1, game.getRemainingShips(), "Remaining ships count should be 1 after sinking one of two ships.");
	}

	// ------------------------------------------------------------------
	// Getters
	// ------------------------------------------------------------------

	@Test
	@DisplayName("getMyFleet devolve a frota recebida no construtor")
	void testGetMyFleet() {
		IFleet fleet = new Fleet();
		assertSame(fleet, new Game(fleet).getMyFleet());
	}

	@Test
	@DisplayName("getAlienFleet e getMyMoves começam vazios")
	void testAlienFleetAndMyMovesStartEmpty() {
		assertNotNull(game.getAlienFleet());
		assertTrue(game.getAlienFleet().getShips().isEmpty());
		assertNotNull(game.getMyMoves());
		assertTrue(game.getMyMoves().isEmpty());
	}

	// ------------------------------------------------------------------
	// fireSingleShot
	// ------------------------------------------------------------------

	@Test
	@DisplayName("fireSingleShot devolve resultado inválido para posição fora do tabuleiro")
	void testFireSingleShotInvalidResult() {
		IGame.ShotResult r = game.fireSingleShot(new Position(10, 10), false);

		assertFalse(r.valid());
		assertFalse(r.repeated());
		assertNull(r.ship());
		assertFalse(r.sunk());
		assertEquals(1, game.getInvalidShots());
	}

	@Test
	@DisplayName("fireSingleShot com isRepeated=true devolve resultado repetido")
	void testFireSingleShotRepeatedFlag() {
		IGame.ShotResult r = game.fireSingleShot(new Position(2, 3), true);

		assertTrue(r.valid());
		assertTrue(r.repeated());
		assertNull(r.ship());
		assertEquals(1, game.getRepeatedShots());
	}

	@Test
	@DisplayName("fireSingleShot deteta repetição por moves anteriores")
	void testFireSingleShotRepeatedByPreviousMove() {
		game.fireShots(List.of(new Position(2, 3), new Position(2, 4), new Position(2, 5)));

		IGame.ShotResult r = game.fireSingleShot(new Position(2, 4), false);

		assertTrue(r.valid());
		assertTrue(r.repeated());
		assertEquals(1, game.getRepeatedShots());
	}

	@Test
	@DisplayName("fireSingleShot na água devolve resultado sem navio")
	void testFireSingleShotMiss() {
		IGame.ShotResult r = game.fireSingleShot(new Position(9, 9), false);

		assertTrue(r.valid());
		assertFalse(r.repeated());
		assertNull(r.ship());
		assertFalse(r.sunk());
		assertEquals(0, game.getHits());
		assertEquals(0, game.getInvalidShots());
		assertEquals(0, game.getRepeatedShots());
	}

	@Test
	@DisplayName("fireSingleShot afunda uma barca com um só tiro")
	void testFireSingleShotHitAndSunk() {
		Ship barge = new Barge(Compass.NORTH, new Position(1, 1));
		game.getMyFleet().addShip(barge);

		IGame.ShotResult r = game.fireSingleShot(new Position(1, 1), false);

		assertTrue(r.valid());
		assertFalse(r.repeated());
		assertSame(barge, r.ship());
		assertTrue(r.sunk());
		assertEquals(1, game.getHits());
		assertEquals(1, game.getSunkShips());
		assertEquals(0, game.getRemainingShips());
	}

	@Test
	@DisplayName("fireSingleShot acerta num navio sem o afundar e depois afunda-o")
	void testFireSingleShotHitNotSunkThenSunk() {
		Ship caravel = new Caravel(Compass.NORTH, new Position(4, 4));
		game.getMyFleet().addShip(caravel);
		Assumptions.assumeTrue(caravel.getSize() > 1, "Precisa de um navio com mais de uma posição");

		IGame.ShotResult r = game.fireSingleShot(caravel.getPositions().get(0), false);

		assertTrue(r.valid());
		assertSame(caravel, r.ship());
		assertFalse(r.sunk(), "Error: the ship still has intact positions.");
		assertEquals(1, game.getHits());
		assertEquals(0, game.getSunkShips());

		for (int i = 1; i < caravel.getSize(); i++) {
			r = game.fireSingleShot(caravel.getPositions().get(i), false);
		}

		assertTrue(r.sunk(), "Error: the last hit should sink the ship.");
		assertEquals(caravel.getSize(), game.getHits());
		assertEquals(1, game.getSunkShips());
	}

	// ------------------------------------------------------------------
	// fireShots
	// ------------------------------------------------------------------

	@Test
	@DisplayName("fireShots lança exceção se o número de tiros for inferior ao exigido")
	void testFireShotsTooFew() {
		assertThrows(IllegalArgumentException.class, () -> game.fireShots(List.of(new Position(1, 1))));
	}

	@Test
	@DisplayName("fireShots lança exceção se o número de tiros for superior ao exigido")
	void testFireShotsTooMany() {
		List<IPosition> shots = new ArrayList<>();
		for (int i = 0; i <= Game.NUMBER_SHOTS; i++) {
			shots.add(new Position(0, i));
		}
		assertThrows(IllegalArgumentException.class, () -> game.fireShots(shots));
	}

	@Test
	@DisplayName("fireShots rejeita lista null (assertions ativas)")
	void testFireShotsNull() {
		assertThrows(AssertionError.class, () -> game.fireShots(null));
	}

	@Test
	@DisplayName("fireShots conta como repetido um tiro duplicado no mesmo move")
	void testFireShotsDuplicateInSameMove() {
		Position p = new Position(2, 3);
		game.fireShots(List.of(p, p, new Position(5, 5)));

		assertEquals(1, game.getRepeatedShots());
	}

	@Test
	@DisplayName("fireShots numera os moves sequencialmente")
	void testFireShotsMoveNumbers() {
		game.fireShots(List.of(new Position(0, 0), new Position(0, 1), new Position(0, 2)));
		game.fireShots(List.of(new Position(1, 0), new Position(1, 1), new Position(1, 2)));

		assertEquals(2, game.getAlienMoves().size());
		assertEquals(1, game.getAlienMoves().get(0).getNumber());
		assertEquals(2, game.getAlienMoves().get(1).getNumber());
	}

	@Test
	@DisplayName("fireShots atualiza acertos e afundamentos")
	void testFireShotsHitsAndSinks() {
		game.getMyFleet().addShip(new Barge(Compass.NORTH, new Position(1, 1)));

		game.fireShots(List.of(new Position(1, 1), new Position(2, 2), new Position(3, 3)));

		assertEquals(1, game.getHits());
		assertEquals(1, game.getSunkShips());
		assertEquals(0, game.getRemainingShips());
	}

	@Test
	@DisplayName("repeatedShot rejeita null (assertions ativas)")
	void testRepeatedShotNull() {
		assertThrows(AssertionError.class, () -> game.repeatedShot(null));
	}

	@Test
	@DisplayName("fireSingleShot rejeita null (assertions ativas)")
	void testFireSingleShotNull() {
		assertThrows(AssertionError.class, () -> game.fireSingleShot(null, false));
	}

	// ------------------------------------------------------------------
	// jsonShots
	// ------------------------------------------------------------------

	@Test
	@DisplayName("jsonShots serializa linha e coluna clássicas de cada tiro")
	void testJsonShots() throws Exception {
		List<IPosition> shots = List.of(new Position(0, 0), new Position(3, 4), new Position(9, 9));

		String json = Game.jsonShots(shots);
		JsonNode root = new ObjectMapper().readTree(json);

		assertTrue(root.isArray());
		assertEquals(shots.size(), root.size());
		for (int i = 0; i < shots.size(); i++) {
			assertEquals(String.valueOf(shots.get(i).getClassicRow()), root.get(i).get("row").asText());
			assertEquals(String.valueOf(shots.get(i).getClassicColumn()), root.get(i).get("column").asText());
		}
	}

	@Test
	@DisplayName("jsonShots com lista vazia devolve um array vazio")
	void testJsonShotsEmpty() throws Exception {
		JsonNode root = new ObjectMapper().readTree(Game.jsonShots(new ArrayList<>()));

		assertTrue(root.isArray());
		assertEquals(0, root.size());
	}

	@Test
	@DisplayName("jsonShots rejeita null (assertions ativas)")
	void testJsonShotsNull() {
		assertThrows(AssertionError.class, () -> Game.jsonShots(null));
	}

	// ------------------------------------------------------------------
	// readEnemyFire
	// ------------------------------------------------------------------

	@Test
	@DisplayName("readEnemyFire lê posições no formato compacto (ex.: A1 B2 C3)")
	void testReadEnemyFireCompact() {
		String json = game.readEnemyFire(new Scanner("A1 B2 C3\n"));

		assertNotNull(json);
		assertEquals(1, game.getAlienMoves().size());
		assertEquals(Game.NUMBER_SHOTS, game.getAlienMoves().get(0).getShots().size());
		assertTrue(json.contains("row"));
	}

	@Test
	@DisplayName("readEnemyFire lê posições em tokens separados (ex.: A 1 B 2 C 3)")
	void testReadEnemyFireSeparateTokens() {
		game.readEnemyFire(new Scanner("A 1 B 2 C 3\n"));

		assertEquals(1, game.getAlienMoves().size());
		assertEquals(Game.NUMBER_SHOTS, game.getAlienMoves().get(0).getShots().size());
	}

	@Test
	@DisplayName("readEnemyFire aceita formatos misturados e minúsculas")
	void testReadEnemyFireMixedFormats() {
		game.readEnemyFire(new Scanner("a 1 B2 c 3\n"));

		assertEquals(1, game.getAlienMoves().size());
		assertEquals(Game.NUMBER_SHOTS, game.getAlienMoves().get(0).getShots().size());
	}

	@Test
	@DisplayName("readEnemyFire ignora posições a mais que o número de tiros")
	void testReadEnemyFireExtraTokensIgnored() {
		game.readEnemyFire(new Scanner("A1 B2 C3 D4 E5\n"));

		assertEquals(Game.NUMBER_SHOTS, game.getAlienMoves().get(0).getShots().size());
	}

	@Test
	@DisplayName("readEnemyFire lança exceção se a coluna não for seguida de linha")
	void testReadEnemyFireIncompletePosition() {
		assertThrows(IllegalArgumentException.class, () -> game.readEnemyFire(new Scanner("A\n")));
		assertThrows(IllegalArgumentException.class, () -> game.readEnemyFire(new Scanner("A B 3\n")));
		assertTrue(game.getAlienMoves().isEmpty());
	}

	@Test
	@DisplayName("readEnemyFire lança exceção se houver menos posições do que o exigido")
	void testReadEnemyFireTooFewPositions() {
		assertThrows(IllegalArgumentException.class, () -> game.readEnemyFire(new Scanner("A1 B2\n")));
		assertThrows(IllegalArgumentException.class, () -> game.readEnemyFire(new Scanner("\n")));
		assertTrue(game.getAlienMoves().isEmpty());
	}

	@Test
	@DisplayName("readEnemyFire rejeita scanner null (assertions ativas)")
	void testReadEnemyFireNull() {
		assertThrows(AssertionError.class, () -> game.readEnemyFire(null));
	}

	// ------------------------------------------------------------------
	// randomEnemyFire
	// ------------------------------------------------------------------

	@Test
	@DisplayName("randomEnemyFire dispara NUMBER_SHOTS tiros distintos e devolve JSON")
	void testRandomEnemyFire() {
		String json = game.randomEnemyFire();

		assertNotNull(json);
		assertTrue(json.contains("row"));
		assertEquals(1, game.getAlienMoves().size());

		List<IPosition> shots = game.getAlienMoves().get(0).getShots();
		assertEquals(Game.NUMBER_SHOTS, shots.size());
		assertEquals(Game.NUMBER_SHOTS, new HashSet<>(shots).size(), "Error: shots should be distinct.");
	}

	@Test
	@DisplayName("randomEnemyFire não dispara para posições adjacentes a navios afundados")
	void testRandomEnemyFireAvoidsSunkAdjacent() {
		Ship barge = new Barge(Compass.NORTH, new Position(4, 4));
		game.getMyFleet().addShip(barge);
		barge.sink();
		List<IPosition> adjacent = barge.getAdjacentPositions();

		game.randomEnemyFire();

		for (IPosition shot : game.getAlienMoves().get(0).getShots()) {
			assertFalse(adjacent.contains(shot), "Error: shot " + shot + " is adjacent to a sunk ship.");
		}
	}

	@Test
	@DisplayName("randomEnemyFire não repete posições de moves anteriores")
	void testRandomEnemyFireAvoidsPreviousShots() {
		game.randomEnemyFire();
		game.randomEnemyFire();

		List<IPosition> first = game.getAlienMoves().get(0).getShots();
		List<IPosition> second = game.getAlienMoves().get(1).getShots();
		for (IPosition shot : second) {
			assertFalse(first.contains(shot), "Error: shot " + shot + " was already fired in the first move.");
		}
	}

	@Test
	@DisplayName("randomEnemyFire usa exatamente as posições livres quando só há NUMBER_SHOTS")
	void testRandomEnemyFireExactlyEnoughCandidates() {
		Assumptions.assumeTrue(Game.NUMBER_SHOTS == 3, "Teste escrito para 3 tiros por jogada");
		Position p1 = new Position(0, 0);
		Position p2 = new Position(0, 1);
		Position p3 = new Position(0, 2);
		blockAllExcept(p1, p2, p3);

		game.randomEnemyFire();

		List<IPosition> shots = game.getAlienMoves().get(1).getShots();
		assertEquals(Game.NUMBER_SHOTS, shots.size());
		assertTrue(shots.containsAll(List.of(p1, p2, p3)), "Error: all free positions should be used.");
		assertEquals(0, game.getRepeatedShots());
	}

	@Test
	@DisplayName("randomEnemyFire repete um tiro quando há menos posições livres do que tiros")
	void testRandomEnemyFireFewerCandidates() {
		Assumptions.assumeTrue(Game.NUMBER_SHOTS == 3, "Teste escrito para 3 tiros por jogada");
		Position p1 = new Position(0, 0);
		Position p2 = new Position(0, 1);
		blockAllExcept(p1, p2);

		game.randomEnemyFire();

		List<IPosition> shots = game.getAlienMoves().get(1).getShots();
		assertEquals(Game.NUMBER_SHOTS, shots.size());
		assertTrue(shots.contains(p1));
		assertTrue(shots.contains(p2));
		assertEquals(1, game.getRepeatedShots(), "Error: the extra shot must be a repeat.");
	}

	@Test
	@DisplayName("randomEnemyFire repete o único tiro possível quando só há uma posição livre")
	void testRandomEnemyFireSingleCandidate() {
		Assumptions.assumeTrue(Game.NUMBER_SHOTS == 3, "Teste escrito para 3 tiros por jogada");
		Position p1 = new Position(0, 0);
		blockAllExcept(p1);

		game.randomEnemyFire();

		List<IPosition> shots = game.getAlienMoves().get(1).getShots();
		assertEquals(Game.NUMBER_SHOTS, shots.size());
		assertTrue(shots.stream().allMatch(p1::equals));
		assertEquals(Game.NUMBER_SHOTS - 1, game.getRepeatedShots());
	}

	// ------------------------------------------------------------------
	// printBoard (estático)
	// ------------------------------------------------------------------

	@Test
	@DisplayName("printBoard com frota vazia desenha só água e a moldura")
	void testPrintBoardEmpty() {
		Game.printBoard(new Fleet(), new ArrayList<>(), false, false);

		for (int r = 0; r < Game.BOARD_SIZE; r++) {
			for (int c = 0; c < Game.BOARD_SIZE; c++) {
				assertEquals('.', cell(r, c), "Error: cell (" + r + "," + c + ") should be water.");
			}
		}
		assertTrue(output().contains("+-"), "Error: board frame should be printed.");
		assertFalse(output().contains("LEGENDA"), "Error: legend should not be printed.");
	}

	@Test
	@DisplayName("printBoard com legenda imprime a legenda")
	void testPrintBoardLegend() {
		Game.printBoard(new Fleet(), new ArrayList<>(), false, true);

		assertTrue(output().contains("LEGENDA"));
		assertTrue(output().contains("Tiro certeiro"));
	}

	@Test
	@DisplayName("printBoard desenha navios a flutuar sem marcar as posições adjacentes")
	void testPrintBoardFloatingShip() {
		Fleet fleet = new Fleet();
		fleet.addShip(new Barge(Compass.NORTH, new Position(4, 4)));

		Game.printBoard(fleet, new ArrayList<>(), false, false);

		assertEquals('#', cell(4, 4));
		assertEquals('.', cell(3, 3), "Error: adjacent cells are only marked for sunk ships.");
	}

	@Test
	@DisplayName("printBoard marca as posições adjacentes de navios afundados")
	void testPrintBoardSunkShip() {
		Fleet fleet = new Fleet();
		Ship barge = new Barge(Compass.NORTH, new Position(4, 4));
		fleet.addShip(barge);
		barge.sink();

		Game.printBoard(fleet, new ArrayList<>(), false, false);

		assertEquals('#', cell(4, 4));
		assertEquals('-', cell(3, 3));
		assertEquals('-', cell(5, 5));
		assertEquals('.', cell(0, 0));
	}

	private Fleet fleetWithFloatingAndSunk() {
		Fleet fleet = new Fleet();
		fleet.addShip(new Barge(Compass.NORTH, new Position(4, 4)));   // a flutuar
		Ship sunk = new Barge(Compass.NORTH, new Position(7, 7));      // afundada
		fleet.addShip(sunk);
		sunk.sink();
		return fleet;
	}

	@Test
	@DisplayName("printBoard com tiros marca acertos e tiros na água")
	void testPrintBoardWithShots() {
		List<IMove> moves = movesOf(
				new Position(4, 4), new Position(4, 4),     // acerto, depois repetido sobre '*'
				new Position(0, 0), new Position(0, 0),     // água, depois repetido sobre 'o'
				new Position(6, 6),                          // adjacente a navio afundado
				new Position(7, 7),                          // posição do navio afundado
				new Position(-1, -1), new Position(10, 10)); // fora do tabuleiro, ignorados

		Game.printBoard(fleetWithFloatingAndSunk(), moves, true, false);

		assertEquals('*', cell(4, 4), "Error: shot on a ship should be '*'.");
		assertEquals('o', cell(0, 0), "Error: shot on water should be 'o'.");
		assertEquals('o', cell(6, 6), "Error: shot on an adjacent cell should be 'o'.");
		assertEquals('*', cell(7, 7), "Error: shot on a sunk ship should be '*'.");
		assertEquals('-', cell(8, 8), "Error: unshot adjacent cell of a sunk ship should stay '-'.");
		assertEquals('.', cell(5, 5), "Error: cell next to a floating ship should stay water.");
	}

	@Test
	@DisplayName("printBoard sem show_shots não desenha os tiros")
	void testPrintBoardShotsHidden() {
		List<IMove> moves = movesOf(new Position(4, 4), new Position(0, 0), new Position(6, 6));

		Game.printBoard(fleetWithFloatingAndSunk(), moves, false, false);

		assertEquals('#', cell(4, 4));
		assertEquals('.', cell(0, 0));
		assertEquals('-', cell(6, 6));
	}

	@Test
	@DisplayName("printBoard rejeita frota null (assertions ativas)")
	void testPrintBoardNullFleet() {
		assertThrows(AssertionError.class, () -> Game.printBoard(null, new ArrayList<>(), false, false));
	}

	@Test
	@DisplayName("printBoard rejeita lista de moves null (assertions ativas)")
	void testPrintBoardNullMoves() {
		assertThrows(AssertionError.class, () -> Game.printBoard(new Fleet(), null, false, false));
	}

	// ------------------------------------------------------------------
	// printMyBoard / printAlienBoard / over
	// ------------------------------------------------------------------

	@Test
	@DisplayName("printMyBoard desenha a minha frota e os tiros do adversário")
	void testPrintMyBoard() {
		game.getMyFleet().addShip(new Barge(Compass.NORTH, new Position(4, 4)));
		game.getAlienMoves().add(new Move(1, new ArrayList<>(List.of(new Position(4, 4), new Position(0, 0))), new ArrayList<>()));

		game.printMyBoard(true, false);

		assertEquals('*', cell(4, 4));
		assertEquals('o', cell(0, 0));
	}

	@Test
	@DisplayName("printMyBoard com legenda e sem tiros")
	void testPrintMyBoardLegendNoShots() {
		game.getMyFleet().addShip(new Barge(Compass.NORTH, new Position(4, 4)));

		game.printMyBoard(false, true);

		assertEquals('#', cell(4, 4));
		assertTrue(output().contains("LEGENDA"));
	}

	@Test
	@DisplayName("printAlienBoard desenha a frota adversária e os meus tiros")
	void testPrintAlienBoard() {
		game.getAlienFleet().addShip(new Barge(Compass.NORTH, new Position(2, 2)));
		game.getMyMoves().add(new Move(1, new ArrayList<>(List.of(new Position(2, 2), new Position(9, 9))), new ArrayList<>()));

		game.printAlienBoard(true, false);

		assertEquals('*', cell(2, 2));
		assertEquals('o', cell(9, 9));
	}

	@Test
	@DisplayName("printAlienBoard sem navios nem tiros desenha só água")
	void testPrintAlienBoardEmpty() {
		game.printAlienBoard(false, false);

		assertEquals('.', cell(0, 0));
		assertEquals('.', cell(9, 9));
	}

	@Test
	@DisplayName("over imprime a mensagem de fim de jogo")
	void testOver() {
		game.over();

		assertTrue(output().contains("Maldito sejas"));
	}
}

package battleship;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class for Fleet.
 * Author: Henrique Catarino (129799)
 * Date: 2026-11-21
 * Cyclomatic Complexity for each method:
 * - Constructor: 1
 * - createRandom: 4
 * - addShip: 4
 * - getShips: 1
 * - getShipsLike: 2
 * - getFloatingShips: 2
 * - getSunkShips: 2
 * - shipAt: 2
 * - colisionRisk: 2
 * - printShips: 2
 * - printStatus: 1
 * - printShipsByCategory: 1
 * - printFloatingShips: 1
 * - printAllShips: 1
 */
public class FleetTest {

	private Fleet fleet;
	private PrintStream originalOut;
	private ByteArrayOutputStream outContent;

	@BeforeEach
	void setUp() {
		fleet = new Fleet();
		// Captura o System.out para poder verificar o que os métodos print* escrevem
		originalOut = System.out;
		outContent = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
	}

	@AfterEach
	void tearDown() {
		System.setOut(originalOut);
		fleet = null;
	}

	private String output() {
		return outContent.toString(StandardCharsets.UTF_8);
	}

	// ------------------------------------------------------------------
	// Testes originais
	// ------------------------------------------------------------------

	/**
	 * Test for the Fleet constructor.
	 * Cyclomatic Complexity: 1
	 */
	@Test
	@DisplayName("Construtor cria uma frota vazia")
	void testConstructor() {
		assertNotNull(fleet, "Error: Instance of Fleet should not be null.");
		assertTrue(fleet.getShips().isEmpty(), "Error: Fleet should be initialized with empty ships list.");
	}

	@Test
	@DisplayName("createRandom cria uma frota com FLEET_SIZE navios")
	void testCreateRandom() {
		IFleet r = Fleet.createRandom();
		assertNotNull(r, "Error: createRandom should return a non-null fleet instance.");
		assertEquals(Fleet.FLEET_SIZE, r.getShips().size(), "Error: createRandom should create a fleet with the correct number of ships.");
	}

	/**
	 * Test for the addShip method (all conditions true).
	 * Cyclomatic Complexity: 3
	 */
	@Test
	@DisplayName("addShip adiciona um navio válido")
	void testAddShip1() {
		IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
		assertTrue(fleet.addShip(ship), "Error: Valid ship should be added successfully.");
		assertEquals(1, fleet.getShips().size(), "Error: Fleet should contain one ship after addition.");
	}

	/**
	 * Test for the addShip method (ship outside the board, at the limit).
	 */
	@Test
	@DisplayName("addShip rejeita navio em (10,10), fora do tabuleiro")
	void testAddShip2() {
		for (int i = 0; i < Fleet.FLEET_SIZE; i++) {
			fleet.addShip(new Barge(Compass.NORTH, new Position(i, 0)));
		}
		IShip anotherShip = new Barge(Compass.NORTH, new Position(10, 10));
		assertFalse(fleet.addShip(anotherShip), "Error: Should not add ship outside the board.");
	}

	/**
	 * Test for the addShip method (ship outside the board).
	 */
	@Test
	@DisplayName("addShip rejeita navio fora do tabuleiro (99,99)")
	void testAddShip3() {
		IShip shipOutside = new Barge(Compass.NORTH, new Position(99, 99));
		assertFalse(fleet.addShip(shipOutside), "Error: Should not add ship outside the board.");
	}

	/**
	 * Test for the addShip method (collision risk).
	 */
	@Test
	@DisplayName("addShip rejeita navio sobreposto a outro")
	void testAddShip4() {
		IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		IShip ship2 = new Barge(Compass.NORTH, new Position(1, 1));  // Overlapping position
		fleet.addShip(ship1);
		assertFalse(fleet.addShip(ship2), "Error: Should not add ship with a collision risk.");
	}

	/**
	 * Test for the getShips method.
	 * Cyclomatic Complexity: 1
	 */
	@Test
	@DisplayName("getShips devolve a lista de navios da frota")
	void testGetShips() {
		assertTrue(fleet.getShips().isEmpty(), "Error: Fleet's ships list should initially be empty.");
		IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
		fleet.addShip(ship);
		assertEquals(1, fleet.getShips().size(), "Error: Fleet should have size 1 after adding a ship.");
		assertEquals(ship, fleet.getShips().get(0), "Error: Fleet's first ship should match the added ship.");
	}

	/**
	 * Test for the getShipsLike method (ships of specific category).
	 * Cyclomatic Complexity: 2
	 */
	@Test
	@DisplayName("getShipsLike devolve só os navios da categoria pedida")
	void testGetShipsLike() {
		IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		IShip ship2 = new Caravel(Compass.NORTH, new Position(2, 1));
		fleet.addShip(ship1);
		fleet.addShip(ship2);

		List<IShip> barges = fleet.getShipsLike("Barca");
		assertEquals(1, barges.size(), "Error: There should be exactly one ship of category 'Barca'.");
		assertEquals(ship1, barges.get(0), "Error: The ship of category 'Barca' does not match.");
	}

	/**
	 * Test for the getFloatingShips method.
	 * Cyclomatic Complexity: 2
	 */
	@Test
	@DisplayName("getFloatingShips devolve só os navios que ainda flutuam")
	void testGetFloatingShips() {
		IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		IShip ship2 = new Caravel(Compass.NORTH, new Position(4, 4));
		fleet.addShip(ship1);
		fleet.addShip(ship2);

		List<IShip> floatingShips = fleet.getFloatingShips();
		assertEquals(2, floatingShips.size(), "Error: All ships should be floating initially.");

		ship1.getPositions().get(0).shoot();  // Sink ship1
		floatingShips = fleet.getFloatingShips();
		assertEquals(1, floatingShips.size(), "Error: Only one ship should be floating after sinking one.");
		assertEquals(ship2, floatingShips.get(0), "Error: The floating ship should match the expected result.");
	}

	/**
	 * Test for the shipAt method.
	 * Cyclomatic Complexity: 2
	 */
	@Test
	@DisplayName("shipAt devolve o navio na posição, ou null se estiver vazia")
	void testShipAt() {
		IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
		fleet.addShip(ship);

		assertEquals(ship, fleet.shipAt(new Position(1, 1)), "Error: Should return the correct ship at the position.");
		assertNull(fleet.shipAt(new Position(5, 5)), "Error: Should return null for empty positions in the fleet.");
	}

	/**
	 * Test for private method colisionRisk.
	 * Cyclomatic Complexity: 2
	 */
	@Test
	@DisplayName("colisionRisk (privado) deteta navios sobrepostos")
	void testCollisionRisk() throws Exception {
		var method = Fleet.class.getDeclaredMethod("collisionRisk", IShip.class);
		method.setAccessible(true);

		IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		IShip ship2 = new Barge(Compass.NORTH, new Position(1, 1));  // Overlapping position
		fleet.addShip(ship1);

		assertTrue((Boolean) method.invoke(fleet, ship2), "Error: Overlapping ships should be at collision risk.");
		assertFalse((Boolean) method.invoke(fleet, new Barge(Compass.NORTH, new Position(5, 5))),
				"Error: Ships at non-overlapping positions should not have a collision risk.");
	}

	/**
	 * Test for the printStatus method.
	 * Cyclomatic Complexity: 1
	 */
	@Test
	@DisplayName("printStatus não lança exceções")
	void testPrintStatus() {
		IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
		fleet.addShip(ship);
		assertDoesNotThrow(fleet::printStatus, "Error: printStatus should not throw any exceptions.");
	}

	// ------------------------------------------------------------------
	// Testes adicionados para maximizar a cobertura
	// ------------------------------------------------------------------

	// ---------- addShip: limite da frota ----------
	@Test
	@DisplayName("addShip rejeita um navio válido quando a frota já atingiu o limite")
	void testAddShipFleetFull() {
		// Grelha de barcas separadas por 2 posições, para não haver risco de colisão
		for (int i = 0; i < 25; i++) {
			fleet.addShip(new Barge(Compass.NORTH, new Position(2 * (i / 5), 2 * (i % 5))));
		}
		int size = fleet.getShips().size();
		assertTrue(size >= Fleet.FLEET_SIZE, "Error: Fleet should have reached the size limit.");
		assertTrue(size <= Fleet.FLEET_SIZE + 1, "Error: Fleet should not grow beyond the size limit.");

		// (8,8) está dentro do tabuleiro e livre, por isso só o limite pode causar a rejeição
		assertFalse(fleet.addShip(new Barge(Compass.NORTH, new Position(8, 8))),
				"Error: Should not add ship when the fleet is full.");
		assertEquals(size, fleet.getShips().size(), "Error: Fleet size should not change.");
	}

	// ---------- Assertions internas (requerem -ea, ativo por defeito no Surefire/IntelliJ) ----------
	@Test
	@DisplayName("addShip rejeita null (assertions ativas)")
	void testAddShipNull() {
		assertThrows(AssertionError.class, () -> fleet.addShip(null));
	}

	@Test
	@DisplayName("getShipsLike rejeita categoria null (assertions ativas)")
	void testGetShipsLikeNull() {
		assertThrows(AssertionError.class, () -> fleet.getShipsLike(null));
	}

	@Test
	@DisplayName("shipAt rejeita posição null (assertions ativas)")
	void testShipAtNull() {
		assertThrows(AssertionError.class, () -> fleet.shipAt(null));
	}

	@Test
	@DisplayName("colisionRisk (privado) rejeita null (assertions ativas)")
	void testCollisionRiskNull() throws Exception {
		var method = Fleet.class.getDeclaredMethod("collisionRisk", IShip.class);
		method.setAccessible(true);

		InvocationTargetException ex = assertThrows(InvocationTargetException.class,
				() -> method.invoke(fleet, (Object) null));
		assertInstanceOf(AssertionError.class, ex.getCause());
	}

	@Test
	@DisplayName("printShips rejeita lista null (assertions ativas)")
	void testPrintShipsNull() {
		assertThrows(AssertionError.class, () -> fleet.printShips(null));
	}

	@Test
	@DisplayName("printShipsByCategory rejeita categoria null (assertions ativas)")
	void testPrintShipsByCategoryNull() {
		assertThrows(AssertionError.class, () -> fleet.printShipsByCategory(null));
	}

	// ---------- getSunkShips ----------
	@Test
	@DisplayName("getSunkShips devolve só os navios afundados")
	void testGetSunkShips() {
		IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		IShip ship2 = new Caravel(Compass.NORTH, new Position(4, 4));
		fleet.addShip(ship1);
		fleet.addShip(ship2);

		assertTrue(fleet.getSunkShips().isEmpty(), "Error: No ship should be sunk initially.");

		ship1.sink();
		List<IShip> sunk = fleet.getSunkShips();
		assertEquals(1, sunk.size(), "Error: Exactly one ship should be sunk.");
		assertEquals(ship1, sunk.get(0), "Error: The sunk ship should be the first one.");
	}

	// ---------- printShips ----------
	@Test
	@DisplayName("printShips imprime cada navio da lista")
	void testPrintShips() {
		IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		IShip ship2 = new Caravel(Compass.NORTH, new Position(4, 4));
		fleet.addShip(ship1);
		fleet.addShip(ship2);

		fleet.printShips(fleet.getShips());

		assertTrue(output().contains(ship1.toString()), "Error: First ship should be printed.");
		assertTrue(output().contains(ship2.toString()), "Error: Second ship should be printed.");
	}

	@Test
	@DisplayName("printShips não imprime nada com lista vazia")
	void testPrintShipsEmptyList() {
		fleet.printShips(new ArrayList<IShip>());
		assertEquals("", output(), "Error: Nothing should be printed for an empty list.");
	}

	// ---------- printStatus ----------
	@Test
	@DisplayName("printStatus mostra o número de navios a flutuar e afundados")
	void testPrintStatusOutput() {
		IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		IShip ship2 = new Caravel(Compass.NORTH, new Position(4, 4));
		fleet.addShip(ship1);
		fleet.addShip(ship2);
		ship1.sink();

		fleet.printStatus();

		assertTrue(output().contains("Estado da Frota: 1 a flutuar, 1 afundados!"),
				"Error: printStatus should report 1 floating and 1 sunk ship.");
	}

	// ---------- printShipsByCategory ----------
	@Test
	@DisplayName("printShipsByCategory imprime só os navios da categoria pedida")
	void testPrintShipsByCategory() {
		IShip barge = new Barge(Compass.NORTH, new Position(1, 1));
		IShip caravel = new Caravel(Compass.NORTH, new Position(4, 4));
		fleet.addShip(barge);
		fleet.addShip(caravel);

		fleet.printShipsByCategory(barge.getCategory());

		assertTrue(output().contains(barge.toString()), "Error: The barge should be printed.");
		assertFalse(output().contains(caravel.toString()), "Error: The caravel should not be printed.");
	}

	// ---------- printFloatingShips ----------
	@Test
	@DisplayName("printFloatingShips imprime só os navios que ainda flutuam")
	void testPrintFloatingShips() {
		IShip sunkShip = new Barge(Compass.NORTH, new Position(1, 1));
		IShip floatingShip = new Caravel(Compass.NORTH, new Position(4, 4));
		fleet.addShip(sunkShip);
		fleet.addShip(floatingShip);
		sunkShip.sink();

		fleet.printFloatingShips();

		assertTrue(output().contains(floatingShip.toString()), "Error: The floating ship should be printed.");
		assertFalse(output().contains(sunkShip.toString()), "Error: The sunk ship should not be printed.");
	}

	// ---------- printAllShips ----------
	@Test
	@DisplayName("printAllShips imprime todos os navios da frota")
	void testPrintAllShips() {
		IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		IShip ship2 = new Caravel(Compass.NORTH, new Position(4, 4));
		fleet.addShip(ship1);
		fleet.addShip(ship2);
		ship1.sink();

		fleet.printAllShips();

		assertTrue(output().contains(ship1.toString()), "Error: First ship should be printed.");
		assertTrue(output().contains(ship2.toString()), "Error: Second ship should be printed.");
	}
}
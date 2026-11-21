package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class TasksTest {

	private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
	private final PrintStream standardOut = System.out;
	private final InputStream standardIn = System.in;

	@BeforeEach
	void setUp() {
		System.setOut(new PrintStream(outputStreamCaptor));
	}

	@AfterEach
	void tearDown() {
		System.setOut(standardOut);
		System.setIn(standardIn);
		File pdfFile = new File("data/jogadas.pdf");
		if (pdfFile.exists()) {
			pdfFile.delete();
		}
	}

	private void provideInput(String data) {
		System.setIn(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	@DisplayName("Deve instanciar Tasks sem erros")
	void testConstructor() {
		assertNotNull(new Tasks());
	}

	@Test
	@DisplayName("Deve imprimir a mensagem de ajuda do menu corretamente")
	void testMenuHelp() {
		Tasks.menuHelp();
		String output = outputStreamCaptor.toString();
		assertTrue(output.contains("AJUDA DO MENU"));
		assertTrue(output.contains("gerafrota"));
		assertTrue(output.contains("desisto"));
	}

	@Test
	@DisplayName("Deve ler uma posição numérica corretamente com readPosition")
	void testReadPosition() {
		Scanner scanner = new Scanner("3 5");
		Position pos = Tasks.readPosition(scanner);
		assertNotNull(pos);
		assertEquals(3, pos.getRow());
		assertEquals(5, pos.getColumn());
	}

	@Test
	@DisplayName("Deve lançar AssertionError quando readPosition recebe null")
	void testReadPositionNull() {
		assertThrows(AssertionError.class, () -> Tasks.readPosition(null));
	}

	@Test
	@DisplayName("Deve ler posições clássicas em formatos válidos ('A3' e 'B 4')")
	void testReadClassicPositionValid() {
		IPosition pos1 = Tasks.readClassicPosition(new Scanner("A3"));
		assertEquals('A', pos1.getClassicRow());
		assertEquals(3, pos1.getClassicColumn());

		IPosition pos2 = Tasks.readClassicPosition(new Scanner("b 4"));
		assertEquals('B', pos2.getClassicRow());
		assertEquals(4, pos2.getClassicColumn());
	}

	@Test
	@DisplayName("Deve lançar IllegalArgumentException para entradas inválidas em readClassicPosition")
	void testReadClassicPositionInvalid() {
		// Scanner vazio
		assertThrows(IllegalArgumentException.class, () -> Tasks.readClassicPosition(new Scanner("")));

		// Formato inválido sem número
		assertThrows(IllegalArgumentException.class, () -> Tasks.readClassicPosition(new Scanner("ABC")));

		// Formato inválido com duas partes não numéricas
		assertThrows(IllegalArgumentException.class, () -> Tasks.readClassicPosition(new Scanner("12 34")));
	}

	@Test
	@DisplayName("Deve ler um navio válido e retornar null para tipo desconhecido em readShip")
	void testReadShip() {
		Ship validShip = Tasks.readShip(new Scanner("barca 1 1 n"));
		assertNotNull(validShip);

		Ship invalidShip = Tasks.readShip(new Scanner("submarino_inexistente 1 1 n"));
		assertNull(invalidShip);
	}

	@Test
	@DisplayName("Deve lançar AssertionError quando readShip ou buildFleet recebem null")
	void testNullAssertions() {
		assertThrows(AssertionError.class, () -> Tasks.readShip(null));
		assertThrows(AssertionError.class, () -> Tasks.buildFleet(null));
	}

	@Test
	@DisplayName("Deve construir uma frota completa ignorando navios desconhecidos ou posições inválidas/colisões")
	void testBuildFleetWithErrorsAndSuccess() {
		String fleetInput = String.join("\n",
				"desconhecido 0 0 n",
				"galeao 0 0 e",
				"galeao 0 0 e", // Colisão (falha addShip)
				"fragata 2 0 e",
				"fragata 2 5 e",
				"nau 4 0 e",
				"nau 4 4 e",
				"nau 6 0 e",
				"caravela 6 4 e",
				"caravela 6 7 e",
				"caravela 8 0 e",
				"caravela 8 3 e",
				"barca 8 6 n",
				"barca 8 8 n"
		);
		Fleet fleet = Tasks.buildFleet(new Scanner(fleetInput));
		assertNotNull(fleet);
		assertEquals(Fleet.FLEET_SIZE, fleet.getShips().size());
	}

	@Test
	@DisplayName("Deve executar menu sem jogo iniciado e sair com desisto")
	void testMenuBeforeGameStarts() {
		String commands = String.join("\n",
				"ajuda",
				"estado",
				"mapa",
				"rajada",
				"simula",
				"tiros",
				"tempo",
				"scoreboard",
				"pdf",
				"comando_invalido",
				"desisto"
		);
		provideInput(commands);

		Tasks.menu();

		String output = outputStreamCaptor.toString();
		assertTrue(output.contains("Ainda não existe jogo iniciado."));
		assertTrue(output.contains("Que comando é esse??? Repete ..."));
		assertTrue(output.contains("Bons ventos!"));
	}

	@Test
	@DisplayName("Deve gerar frota aleatória, executar comandos durante o jogo e registar jogo abandonado ao desistir")
	void testMenuWithRandomFleetAndAbandonedGame() {
		// Rajada precisa de 3 tiros válidos (ex: A1 A2 A3)
		String commands = String.join("\n",
				"gerafrota",
				"estado",
				"mapa",
				"rajada A1 A2 A3",
				"tiros",
				"tempo",
				"pdf",
				"desisto"
		);
		provideInput(commands);

		Tasks.menu();

		String output = outputStreamCaptor.toString();
		assertTrue(output.contains("PDF gerado em: data/jogadas.pdf"));
		assertTrue(output.contains("Bons ventos!"));
	}

	@Test
	@DisplayName("Deve permitir carregar frota manual com lefrota e desistir sem jogadas")
	void testMenuWithManualFleet() {
		String commands = String.join("\n",
				"lefrota",
				"galeao 0 0 e",
				"fragata 2 0 e",
				"fragata 2 5 e",
				"nau 4 0 e",
				"nau 4 4 e",
				"nau 6 0 e",
				"caravela 6 4 e",
				"caravela 6 7 e",
				"caravela 8 0 e",
				"caravela 8 3 e",
				"barca 8 6 n",
				"barca 8 8 n",
				"desisto"
		);
		provideInput(commands);

		Tasks.menu();

		String output = outputStreamCaptor.toString();
		assertTrue(output.contains("Bons ventos!"));
	}
}
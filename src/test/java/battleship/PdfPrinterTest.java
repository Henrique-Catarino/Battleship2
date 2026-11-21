package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfPrinterTest {

    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final PrintStream standardOut = System.out;
    private final File pdfFile = new File("data/jogadas.pdf");
    private final File dataDir = new File("data");

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outputStreamCaptor));
        cleanUpFiles();
    }

    @AfterEach
    void tearDown() {
        System.setOut(standardOut);
        cleanUpFiles();
    }

    private void cleanUpFiles() {
        if (pdfFile.exists()) {
            pdfFile.delete();
        }
        if (dataDir.exists() && dataDir.list() != null && dataDir.list().length == 0) {
            dataDir.delete();
        }
    }

    private IGame createMockGame(List<IMove> moves, int hits, int sunkShips) {
        return (IGame) Proxy.newProxyInstance(
                IGame.class.getClassLoader(),
                new Class<?>[]{IGame.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAlienMoves" -> moves;
                    case "getHits" -> hits;
                    case "getSunkShips" -> sunkShips;
                    default -> null;
                }
        );
    }

    private IMove createMockMove(int number, List<IPosition> shots, List<IGame.ShotResult> results) {
        return (IMove) Proxy.newProxyInstance(
                IMove.class.getClassLoader(),
                new Class<?>[]{IMove.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getNumber" -> number;
                    case "getShots" -> shots;
                    case "getShotResults" -> results;
                    default -> null;
                }
        );
    }

    private IPosition createMockPosition(char row, int col) {
        return (IPosition) Proxy.newProxyInstance(
                IPosition.class.getClassLoader(),
                new Class<?>[]{IPosition.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getClassicRow" -> row;
                    case "getClassicColumn" -> col;
                    default -> null;
                }
        );
    }

    private IShip createMockShip() {
        return (IShip) Proxy.newProxyInstance(
                IShip.class.getClassLoader(),
                new Class<?>[]{IShip.class},
                (proxy, method, args) -> null
        );
    }

    @Test
    @DisplayName("Deve instanciar PdfPrinter sem erros")
    void testConstructor() {
        assertNotNull(new PdfPrinter());
    }

    @Test
    @DisplayName("Deve gerar PDF com sucesso quando não existem jogadas e diretoria data não existe")
    void testGeneratePdfEmptyMovesAndNewDir() {
        IGame game = createMockGame(new ArrayList<>(), 0, 0);

        PdfPrinter.generatePdf(game);

        assertTrue(pdfFile.exists());
        assertTrue(outputStreamCaptor.toString().contains("PDF gerado em: data/jogadas.pdf"));
    }

    @Test
    @DisplayName("Deve gerar PDF com múltiplas páginas e cobrir todos os tipos de resultados de tiros")
    void testGeneratePdfMultiplePagesAndShotResults() {
        dataDir.mkdirs(); // Garante que a diretoria existe antes de chamar generatePdf

        IShip mockShip = createMockShip();
        List<IPosition> shots = List.of(
                createMockPosition('A', 1),
                createMockPosition('B', 2),
                createMockPosition('C', 3)
        );

        // Cobrir todos os ramos dos filtros:
        // 1. Válido, não repetido, com navio e afundado (hit + sunk)
        // 2. Válido, não repetido, sem navio (miss)
        // 3. Válido, repetido, com navio (ignorado nos hits/misses, mas válido no sunk)
        // 4. Inválido (ignorado em todos)
        List<IGame.ShotResult> results = List.of(
                new IGame.ShotResult(true, false, mockShip, true),
                new IGame.ShotResult(true, false, null, false),
                new IGame.ShotResult(true, true, mockShip, false),
                new IGame.ShotResult(false, false, mockShip, true)
        );

        // Criar 16 jogadas para ultrapassar o limite vertical de uma página A4 e forçar nova página
        List<IMove> moves = new ArrayList<>();
        for (int i = 1; i <= 16; i++) {
            moves.add(createMockMove(i, shots, results));
        }

        IGame game = createMockGame(moves, 16, 16);

        PdfPrinter.generatePdf(game);

        assertTrue(pdfFile.exists());
        assertTrue(pdfFile.length() > 0);
        assertTrue(outputStreamCaptor.toString().contains("PDF gerado em: data/jogadas.pdf"));
    }

    @Test
    @DisplayName("Deve capturar IOException e imprimir mensagem de erro se não conseguir gravar o ficheiro")
    void testGeneratePdfIOException() {
        // Criar uma pasta com o mesmo nome do ficheiro PDF para forçar IOException ao guardar
        assertTrue(pdfFile.mkdirs());

        IGame game = createMockGame(new ArrayList<>(), 0, 0);

        PdfPrinter.generatePdf(game);

        assertTrue(outputStreamCaptor.toString().contains("Erro ao gerar o PDF:"));
    }
}
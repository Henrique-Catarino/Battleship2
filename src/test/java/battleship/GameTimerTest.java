package battleship;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class GameTimerTest {

    private GameTimer gameTimer;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final PrintStream standardOut = System.out;

    @BeforeEach
    void setUp() {
        gameTimer = new GameTimer();
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @AfterEach
    void tearDown() {
        System.setOut(standardOut);
    }

    @Test
    @DisplayName("Deve iniciar o jogo e o primeiro turno corretamente")
    void testStartGame() {
        assertDoesNotThrow(() -> gameTimer.startGame());
        gameTimer.showStatus();
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("ESTATÍSTICAS DE TEMPO"));
    }

    @Test
    @DisplayName("Não deve fazer nada se endTurn for chamado sem um turno iniciado")
    void testEndTurnWithoutStarting() {
        gameTimer.endTurn();
        gameTimer.showStatus();
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("(0 jogadas)"));
    }

    @Test
    @DisplayName("Deve terminar o turno sem aviso quando o tempo é inferior ou igual a 30 segundos")
    void testEndTurnFast() {
        gameTimer.startGame();
        gameTimer.endTurn();
        String output = outputStreamCaptor.toString();
        assertFalse(output.contains("[AVISO DE TEMPO]"));
    }

    @Test
    @DisplayName("Deve exibir aviso de tempo quando o turno demora mais de 30 segundos")
    void testEndTurnSlowWithWarning() throws Exception {
        gameTimer.startTurn();

        // Simular que o turnWatch iniciou há 35 segundos (35_000_000_000 nanossegundos)
        Field turnWatchField = GameTimer.class.getDeclaredField("turnWatch");
        turnWatchField.setAccessible(true);
        StopWatch turnWatch = (StopWatch) turnWatchField.get(gameTimer);

        Field startTimeField = StopWatch.class.getDeclaredField("startTimeNanos");
        startTimeField.setAccessible(true);
        long currentStart = (long) startTimeField.get(turnWatch);
        startTimeField.set(turnWatch, currentStart - 35_000_000_000L);

        gameTimer.endTurn();

        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("[AVISO DE TEMPO]"));
        assertTrue(output.contains("Demorou"));
    }

    @Test
    @DisplayName("Deve mostrar estatísticas com valores a zero antes do jogo iniciar")
    void testShowStatusBeforeStart() {
        gameTimer.showStatus();
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("Tempo no turno atual: 0 s"));
        assertTrue(output.contains("Tempo total de jogo:  0 s"));
        assertTrue(output.contains("0.0 s (0 jogadas)") || output.contains("0,0 s (0 jogadas)"));
    }

    @Test
    @DisplayName("Deve mostrar estatísticas corretamente após completar pelo menos uma jogada")
    void testShowStatusWithTurns() {
        gameTimer.startGame();
        gameTimer.endTurn();
        gameTimer.startTurn();
        outputStreamCaptor.reset();

        gameTimer.showStatus();
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("(1 jogadas)"));
    }

    @Test
    @DisplayName("Deve mostrar resumo final mesmo se o cronómetro total não tiver sido iniciado")
    void testShowFinalSummaryWithoutStart() {
        gameTimer.showFinalSummary();
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("[RESUMO FINAL DE TEMPO]"));
        assertTrue(output.contains("0.0 segundos (0 jogadas efetuadas)") || output.contains("0,0 segundos (0 jogadas efetuadas)"));
    }

    @Test
    @DisplayName("Deve parar o cronómetro total e calcular média no resumo final após jogadas")
    void testShowFinalSummaryAfterGame() {
        gameTimer.startGame();
        gameTimer.endTurn();
        outputStreamCaptor.reset();

        gameTimer.showFinalSummary();
        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("[RESUMO FINAL DE TEMPO]"));
        assertTrue(output.contains("(1 jogadas efetuadas)"));
    }
}
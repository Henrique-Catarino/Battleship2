package battleship;

import org.apache.commons.lang3.time.StopWatch;
import java.util.concurrent.TimeUnit;

public class GameTimer {
    private final StopWatch totalWatch;
    private final StopWatch turnWatch;
    private int totalTurns;

    public GameTimer() {
        this.totalWatch = new StopWatch();
        this.turnWatch = new StopWatch();
        this.totalTurns = 0;
    }

    public void startGame() {
        totalWatch.reset();
        totalWatch.start();
        startTurn();
    }

    public void startTurn() {
        turnWatch.reset();
        turnWatch.start();
    }

    public void endTurn() {
        if (turnWatch.isStarted()) {
            turnWatch.stop();
            totalTurns++;
            long seconds = turnWatch.getTime(TimeUnit.SECONDS);
            if (seconds > 30) {
                System.out.printf("[AVISO DE TEMPO] Demorou %d segundos a disparar! Seja mais rápido na próxima jogada.%n", seconds);
            }
        }
    }

    public void showStatus() {
        long totalSec = totalWatch.isStarted() ? totalWatch.getTime(TimeUnit.SECONDS) : 0;
        long turnSec = turnWatch.isStarted() ? turnWatch.getTime(TimeUnit.SECONDS) : 0;
        double avgSec = totalTurns > 0 ? (double) totalSec / totalTurns : 0.0;

        System.out.println("========== ESTATÍSTICAS DE TEMPO ==========");
        System.out.printf("Tempo no turno atual: %d s%n", turnSec);
        System.out.printf("Tempo total de jogo:  %d s%n", totalSec);
        System.out.printf("Tempo médio / rajada: %.1f s (%d jogadas)%n", avgSec, totalTurns);
        System.out.println("===========================================");
    }

    public void showFinalSummary() {
        if (totalWatch.isStarted()) {
            totalWatch.stop();
        }
        long totalSec = totalWatch.getTime(TimeUnit.SECONDS);
        double avgSec = totalTurns > 0 ? (double) totalSec / totalTurns : 0.0;

        System.out.println("\n[RESUMO FINAL DE TEMPO]");
        System.out.printf("Duração da partida: %d segundos%n", totalSec);
        System.out.printf("Média por jogada:   %.1f segundos (%d jogadas efetuadas)%n", avgSec, totalTurns);
    }
}
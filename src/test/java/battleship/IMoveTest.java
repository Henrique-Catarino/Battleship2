package battleship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.InputMismatchException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class IMoveTest {

    @Test
    @DisplayName("readMove lê o número de tiros e as respetivas posições")
    void readMoveReadsShotsAndPositions() {
        Scanner sc = new Scanner("3 1 2 3 4 5 6");

        Move move = IMove.readMove(7, sc);

        List<IPosition> shots = move.getShots();
        assertEquals(3, shots.size());
        assertEquals(new Position(1, 2), shots.get(0));
        assertEquals(new Position(3, 4), shots.get(1));
        assertEquals(new Position(5, 6), shots.get(2));
    }

    @Test
    @DisplayName("readMove guarda o número da jogada recebido")
    void readMoveKeepsMoveNumber() {
        Move move = IMove.readMove(42, new Scanner("1 0 0"));
        assertEquals(42, move.getNumber());
    }

    @Test
    @DisplayName("readMove devolve a lista de resultados vazia")
    void readMoveReturnsEmptyShotResults() {
        Move move = IMove.readMove(1, new Scanner("2 1 1 2 2"));
        assertNotNull(move.getShotResults());
        assertTrue(move.getShotResults().isEmpty());
    }

    @Test
    @DisplayName("readMove com zero tiros devolve uma jogada sem tiros")
    void readMoveWithZeroShots() {
        Move move = IMove.readMove(1, new Scanner("0"));
        assertTrue(move.getShots().isEmpty());
        assertTrue(move.getShotResults().isEmpty());
    }

    @Test
    @DisplayName("readMove com um só tiro")
    void readMoveWithSingleShot() {
        Move move = IMove.readMove(1, new Scanner("1 9 9"));
        assertEquals(1, move.getShots().size());
        assertEquals(new Position(9, 9), move.getShots().get(0));
    }

    @Test
    @DisplayName("readMove só consome o que precisa do Scanner")
    void readMoveLeavesRemainingInputUntouched() {
        Scanner sc = new Scanner("1 3 4 99");
        IMove.readMove(1, sc);
        assertEquals(99, sc.nextInt());
    }

    @Test
    @DisplayName("readMove lança InputMismatchException se a entrada não é um número")
    void readMoveThrowsOnNonNumericInput() {
        assertThrows(InputMismatchException.class,
                () -> IMove.readMove(1, new Scanner("abc")));
    }

    @Test
    @DisplayName("readMove lança NoSuchElementException se a entrada está vazia")
    void readMoveThrowsOnEmptyInput() {
        assertThrows(NoSuchElementException.class,
                () -> IMove.readMove(1, new Scanner("")));
    }

    @Test
    @DisplayName("readMove lança NoSuchElementException se faltam coordenadas")
    void readMoveThrowsWhenCoordinatesAreMissing() {
        assertThrows(NoSuchElementException.class,
                () -> IMove.readMove(1, new Scanner("2 1 2 3")));
    }
}
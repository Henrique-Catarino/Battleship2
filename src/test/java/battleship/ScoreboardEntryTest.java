package battleship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScoreboardEntryTest {

    private ScoreboardEntry entry() {
        return new ScoreboardEntry("2026-11-22", ScoreboardEntry.VICTORY, 10, 7, 13, 2, 1);
    }

    @Test
    @DisplayName("validShots soma acertos e falhas")
    void validShotsSumsHitsAndMisses() {
        assertEquals(20, entry().validShots());
    }

    @Test
    @DisplayName("validShots é zero quando não há acertos nem falhas")
    void validShotsIsZeroWhenNoHitsOrMisses() {
        ScoreboardEntry e = new ScoreboardEntry("2026-11-22", ScoreboardEntry.ABANDONED, 0, 0, 0, 0, 0);
        assertEquals(0, e.validShots());
    }

    @Test
    @DisplayName("validShots ignora tiros repetidos e fora do tabuleiro")
    void validShotsIgnoresRepeatedAndOutside() {
        ScoreboardEntry e = new ScoreboardEntry("2026-11-22", ScoreboardEntry.DEFEAT, 5, 3, 4, 50, 60);
        assertEquals(7, e.validShots());
    }

    @Test
    @DisplayName("Os acessores devolvem os valores do construtor")
    void accessorsReturnConstructorValues() {
        ScoreboardEntry e = entry();
        assertEquals("2026-11-22", e.date());
        assertEquals(ScoreboardEntry.VICTORY, e.result());
        assertEquals(10, e.moves());
        assertEquals(7, e.hits());
        assertEquals(13, e.misses());
        assertEquals(2, e.repeated());
        assertEquals(1, e.outside());
    }

    @Test
    @DisplayName("As constantes de resultado têm os valores esperados")
    void resultConstantsHaveExpectedValues() {
        assertEquals("VITORIA", ScoreboardEntry.VICTORY);
        assertEquals("DERROTA", ScoreboardEntry.DEFEAT);
        assertEquals("ABANDONADO", ScoreboardEntry.ABANDONED);
    }

    @Test
    @DisplayName("Entradas com os mesmos valores são iguais e têm o mesmo hashCode")
    void equalEntriesAreEqualAndShareHashCode() {
        ScoreboardEntry a = entry();
        ScoreboardEntry b = entry();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("Entradas com valores diferentes não são iguais")
    void differentEntriesAreNotEqual() {
        ScoreboardEntry other = new ScoreboardEntry("2026-11-23", ScoreboardEntry.VICTORY, 10, 7, 13, 2, 1);
        assertNotEquals(entry(), other);
        assertNotEquals(entry(), null);
        assertNotEquals(entry(), "not an entry");
    }

    @Test
    @DisplayName("toString inclui os valores dos campos")
    void toStringContainsFieldValues() {
        String text = entry().toString();
        assertTrue(text.contains("2026-11-22"));
        assertTrue(text.contains("VITORIA"));
    }
}
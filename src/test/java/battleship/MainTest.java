package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    private static final String INPUT = "0\n";

    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream captured;

    @BeforeEach
    void setUp() {
        captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("main imprime o título e chama o menu")
    void mainPrintsTitleAndCallsMenu() {
        System.setIn(new ByteArrayInputStream(INPUT.getBytes(StandardCharsets.UTF_8)));

        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            try {
                Main.main(new String[0]);
            } catch (NoSuchElementException expected) {
                // a entrada simulada acabou antes de o menu terminar
            }
        });

        assertTrue(captured.toString(StandardCharsets.UTF_8).contains("***  Battleship  ***"));
    }

    @Test
    @DisplayName("Main pode ser instanciada (construtor por omissão)")
    void mainCanBeInstantiated() {
        assertNotNull(new Main());
    }
}
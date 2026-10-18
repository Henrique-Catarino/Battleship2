package battleship;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Scoreboard {

    private static final String SCOREBOARD_FILE = "data/scoreboard.md";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public record GameResult(
            String date,
            int totalShots,
            int shipsHit,
            int shipsSunk,
            String result
    ) {}

    public static void saveResult(int totalShots, int shipsHit, int shipsSunk, String result) {
        String date = LocalDateTime.now().format(FORMATTER);
        String newRow = "| " + date + " | " + totalShots + " | " +
                shipsHit + " | " + shipsSunk + " | " + result + " |";

        try {
            File file = new File(SCOREBOARD_FILE);
            file.getParentFile().mkdirs();

            if (!file.exists()) {
                try (PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
                    writer.println("# Scoreboard - Batalha Naval");
                    writer.println();
                    writer.println("| Data | Tiros | Acertos | Afundados | Resultado |");
                    writer.println("|------|-------|---------|-----------|-----------|");
                    writer.println(newRow);
                }
            } else {
                try (PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8"))) {
                    writer.println(newRow);
                }
            }
            System.out.println("Resultado guardado em: " + SCOREBOARD_FILE);
        } catch (IOException e) {
            System.out.println("Erro ao guardar o scoreboard: " + e.getMessage());
        }
    }

    public static void printScoreboard() {
        File file = new File(SCOREBOARD_FILE);
        if (!file.exists()) {
            System.out.println("Ainda não existem jogos registados.");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(file.toPath());
            List<GameResult> results = new ArrayList<>();

            int bestShots = Integer.MAX_VALUE;
            String bestDate = "-";
            int totalGames = 0;

            for (String line : lines) {
                if (line.startsWith("|") && !line.startsWith("| Data") && !line.startsWith("|---")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 6) {
                        String date = parts[1].trim();
                        int shots = Integer.parseInt(parts[2].trim());
                        int hits = Integer.parseInt(parts[3].trim());
                        int sunk = Integer.parseInt(parts[4].trim());
                        String res = parts[5].trim();
                        results.add(new GameResult(date, shots, hits, sunk, res));

                        totalGames++;
                        if (res.equals("Vitória") && shots < bestShots) {
                            bestShots = shots;
                            bestDate = date;
                        }
                    }
                }
            }

            System.out.println();
            System.out.println("==================== SCOREBOARD ====================");
            System.out.printf("%-22s %-8s %-10s %-12s %-12s%n",
                    "Data", "Tiros", "Acertos", "Afundados", "Resultado");
            System.out.println("-----------------------------------------------------");
            for (GameResult r : results) {
                System.out.printf("%-22s %-8d %-10d %-12d %-12s%n",
                        r.date(), r.totalShots(), r.shipsHit(), r.shipsSunk(), r.result());
            }
            System.out.println("-----------------------------------------------------");
            System.out.println("Total de partidas: " + totalGames);
            if (bestShots != Integer.MAX_VALUE)
                System.out.println("Melhor jogo: " + bestDate + " com " + bestShots + " tiros");
            System.out.println("=====================================================");
            System.out.println();

        } catch (IOException e) {
            System.out.println("Erro ao ler o scoreboard: " + e.getMessage());
        }
    }
}
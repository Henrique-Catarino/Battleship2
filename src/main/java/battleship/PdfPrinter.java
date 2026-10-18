package battleship;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PdfPrinter {

    private static final String PDF_FILE = "data/jogadas.pdf";
    private static final float MARGIN = 50;
    private static final float FONT_SIZE = 11;
    private static final float LINE_HEIGHT = 16;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static void generatePdf(IGame game) {
        File file = new File(PDF_FILE);
        File parentDir = file.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            System.out.println("Erro ao criar a diretoria data/");
            return;
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            PDPageContentStream content = new PDPageContentStream(document, page);
            float pageHeight = page.getMediaBox().getHeight();
            float y = pageHeight - MARGIN;

            // Título
            content.beginText();
            content.setFont(fontBold, 16);
            content.newLineAtOffset(MARGIN, y);
            content.showText("Batalha Naval - Registo de Jogadas");
            content.endText();
            y -= LINE_HEIGHT * 2;

            // Data
            content.beginText();
            content.setFont(fontNormal, FONT_SIZE);
            content.newLineAtOffset(MARGIN, y);
            content.showText("Data: " + LocalDateTime.now().format(FORMATTER));
            content.endText();
            y -= LINE_HEIGHT * 2;

            // Jogadas
            List<IMove> moves = game.getAlienMoves();
            for (IMove move : moves) {
                // Nova página se necessário
                if (y < MARGIN + LINE_HEIGHT * 4) {
                    content.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    y = pageHeight - MARGIN;
                }

                // Número da jogada
                content.beginText();
                content.setFont(fontBold, FONT_SIZE);
                content.newLineAtOffset(MARGIN, y);
                content.showText("Jogada #" + move.getNumber() + ":");
                content.endText();
                y -= LINE_HEIGHT;

                // Tiros
                content.beginText();
                content.setFont(fontNormal, FONT_SIZE);
                content.newLineAtOffset(MARGIN + 10, y);
                StringBuilder shots = new StringBuilder("Tiros: ");
                for (IPosition shot : move.getShots()) {
                    shots.append(shot.getClassicRow())
                            .append(shot.getClassicColumn())
                            .append(" ");
                }
                content.showText(shots.toString());
                content.endText();
                y -= LINE_HEIGHT;

                // Resultado dos tiros
                List<IGame.ShotResult> results = move.getShotResults();
                long hits = results.stream().filter(r -> r.valid() && !r.repeated() && r.ship() != null).count();
                long misses = results.stream().filter(r -> r.valid() && !r.repeated() && r.ship() == null).count();
                long sunk = results.stream().filter(r -> r.valid() && r.sunk()).count();

                content.beginText();
                content.setFont(fontNormal, FONT_SIZE);
                content.newLineAtOffset(MARGIN + 10, y);
                content.showText("Acertos: " + hits + " | Agua: " + misses + " | Afundados: " + sunk);
                content.endText();
                y -= LINE_HEIGHT * 1.5f;
            }

            // Linha separadora
            y -= LINE_HEIGHT;
            content.moveTo(MARGIN, y);
            content.lineTo(page.getMediaBox().getWidth() - MARGIN, y);
            content.stroke();
            y -= LINE_HEIGHT;

            // Resumo final
            content.beginText();
            content.setFont(fontBold, FONT_SIZE);
            content.newLineAtOffset(MARGIN, y);
            content.showText("Resumo Final:");
            content.endText();
            y -= LINE_HEIGHT;

            content.beginText();
            content.setFont(fontNormal, FONT_SIZE);
            content.newLineAtOffset(MARGIN + 10, y);
            content.showText("Total de rajadas: " + moves.size());
            content.endText();
            y -= LINE_HEIGHT;

            content.beginText();
            content.setFont(fontNormal, FONT_SIZE);
            content.newLineAtOffset(MARGIN + 10, y);
            content.showText("Total de tiros: " + moves.size() * Game.NUMBER_SHOTS);
            content.endText();
            y -= LINE_HEIGHT;

            content.beginText();
            content.setFont(fontNormal, FONT_SIZE);
            content.newLineAtOffset(MARGIN + 10, y);
            content.showText("Acertos: " + game.getHits());
            content.endText();
            y -= LINE_HEIGHT;

            content.beginText();
            content.setFont(fontNormal, FONT_SIZE);
            content.newLineAtOffset(MARGIN + 10, y);
            content.showText("Navios afundados: " + game.getSunkShips());
            content.endText();

            content.close();
            document.save(PDF_FILE);
            System.out.println("PDF gerado em: " + PDF_FILE);

        } catch (IOException e) {
            System.out.println("Erro ao gerar o PDF: " + e.getMessage());
        }
    }
}
package inpg.sn.lms.recrutement.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xddf.usermodel.chart.AxisCrosses;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 * Génère des classeurs .xlsx génériques (en-tête en gras, colonnes
 * auto-dimensionnées) à partir de lignes déjà formatées en objets Java —
 * réutilisable par n'importe quel export du back-office (sessions,
 * candidatures, etc.) sans dupliquer la mise en forme POI.
 */
@Service
public class ExcelExportService {

    public byte[] genererClasseur(String nomFeuille, List<String> entetes, List<List<Object>> lignes) {
        return genererClasseur(nomFeuille, entetes, lignes, null, null, null, null);
    }

    /**
     * Génère un classeur .xlsx avec, en plus du tableau de données, un
     * graphique en barres natif (lisible/modifiable dans Excel) sur une
     * seconde feuille — utilisé pour les exports "Candidatures" et "Sessions"
     * du back-office (TDR : export configurable avec graphiques intégrés).
     * Si {@code titreGraphique} est null, aucun graphique n'est ajouté.
     */
    public byte[] genererClasseur(String nomFeuille, List<String> entetes, List<List<Object>> lignes,
            String titreGraphique, List<String> categoriesGraphique, List<Double> valeursGraphique,
            String legendeSerie) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(nomFeuille);

            CellStyle enteteStyle = workbook.createCellStyle();
            Font enteteFont = workbook.createFont();
            enteteFont.setBold(true);
            enteteStyle.setFont(enteteFont);
            enteteStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            enteteStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < entetes.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(entetes.get(i));
                cell.setCellStyle(enteteStyle);
            }

            int rowIdx = 1;
            for (List<Object> ligne : lignes) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < ligne.size(); i++) {
                    ecrireCellule(row.createCell(i), ligne.get(i));
                }
            }

            for (int i = 0; i < entetes.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            if (titreGraphique != null && categoriesGraphique != null && !categoriesGraphique.isEmpty()) {
                ajouterGraphiqueBarres(workbook, titreGraphique, categoriesGraphique, valeursGraphique, legendeSerie);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du fichier Excel.", e);
        }
    }

    /** Ajoute une feuille "Graphique" contenant les données sources + un graphique en barres natif Excel. */
    private void ajouterGraphiqueBarres(Workbook workbook, String titre, List<String> categories,
            List<Double> valeurs, String legendeSerie) {
        XSSFSheet feuilleGraphique = (XSSFSheet) workbook.createSheet("Graphique");

        Row headerRow = feuilleGraphique.createRow(0);
        headerRow.createCell(0).setCellValue("Catégorie");
        headerRow.createCell(1).setCellValue(legendeSerie != null ? legendeSerie : "Valeur");
        for (int i = 0; i < categories.size(); i++) {
            Row row = feuilleGraphique.createRow(i + 1);
            row.createCell(0).setCellValue(categories.get(i));
            row.createCell(1).setCellValue(valeurs != null && i < valeurs.size() ? valeurs.get(i) : 0d);
        }

        XSSFDrawing drawing = feuilleGraphique.createDrawingPatriarch();
        ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 3, 0, 12, 20);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(titre);
        chart.setTitleOverlay(false);

        XDDFCategoryAxis axeCategories = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis axeValeurs = chart.createValueAxis(AxisPosition.LEFT);
        axeValeurs.setCrosses(AxisCrosses.AUTO_ZERO);

        XDDFDataSource<String> sourceCategories = XDDFDataSourcesFactory.fromStringCellRange(feuilleGraphique,
                new org.apache.poi.ss.util.CellRangeAddress(1, categories.size(), 0, 0));
        XDDFNumericalDataSource<Double> sourceValeurs = XDDFDataSourcesFactory.fromNumericCellRange(feuilleGraphique,
                new org.apache.poi.ss.util.CellRangeAddress(1, categories.size(), 1, 1));

        XDDFChartData data = chart.createData(ChartTypes.BAR, axeCategories, axeValeurs);
        XDDFChartData.Series series = data.addSeries(sourceCategories, sourceValeurs);
        series.setTitle(legendeSerie != null ? legendeSerie : "Valeur", null);
        ((XDDFBarChartData) data).setBarDirection(BarDirection.COL);
        chart.plot(data);
        chart.getOrAddLegend().setPosition(LegendPosition.BOTTOM);
    }

    private void ecrireCellule(Cell cell, Object valeur) {
        if (valeur == null) {
            cell.setBlank();
        } else if (valeur instanceof Number nombre) {
            cell.setCellValue(nombre.doubleValue());
        } else if (valeur instanceof Boolean booleen) {
            cell.setCellValue(booleen);
        } else {
            cell.setCellValue(valeur.toString());
        }
    }
}

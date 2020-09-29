package ru.tecon.dNet.report;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.PropertyTemplate;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.tecon.dNet.report.model.ConsumerModel;
import ru.tecon.dNet.report.model.DataModel;
import ru.tecon.dNet.sBean.ReportBeanLocal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class Report {

    private static final DateTimeFormatter FORMATTER_DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter FORMATTER_MONTH = DateTimeFormatter.ofPattern("MM.yyyy");

    public static Workbook createMonthReport(int object, LocalDate reportDate, ReportBeanLocal loader) {
        Workbook wb = new XSSFWorkbook();

        PropertyTemplate pt = new PropertyTemplate();

        CellStyle boldStyle = getBoldStyle(wb);
        CellStyle boldCenterStyle = getBoldCenterStyle(wb);
        CellStyle centerStyle = getCenterStyle(wb);
        CellStyle borderStyle = getBorderStyle(wb);

        LocalDate start = reportDate.withDayOfMonth(1);
        LocalDate end = Stream.of(YearMonth.from(reportDate).atEndOfMonth(), LocalDate.now()).min(LocalDate::compareTo).get();

        System.out.println("start month");
        createSheet(wb, FORMATTER_MONTH, reportDate, object, loader, pt, boldStyle, borderStyle, boldCenterStyle, centerStyle);
        System.out.println("end month");

        for (LocalDate date = start; date.isBefore(end.plusDays(1)); date = date.plusDays(1)) {
            System.out.println("start " + date);
            createSheet(wb, FORMATTER_DAY, date, object, loader, pt, boldStyle, borderStyle, boldCenterStyle, centerStyle);
            System.out.println("end " + date);
        }

        return wb;
    }

    public static Workbook createDayReport(int object, LocalDate reportDate, ReportBeanLocal loader) {
        Workbook wb = new XSSFWorkbook();

        createSheet(wb, FORMATTER_DAY, reportDate, object, loader, new PropertyTemplate(),
                getBoldStyle(wb), getBorderStyle(wb), getBoldCenterStyle(wb), getCenterStyle(wb));

        return wb;
    }

    private static void createSheet(Workbook wb, DateTimeFormatter formatter, LocalDate date, int object, ReportBeanLocal loader,
                                    PropertyTemplate pt, CellStyle boldStyle, CellStyle borderStyle,
                                    CellStyle boldCenterStyle, CellStyle centerStyle) {
        CellRangeAddress cellAddresses;

        Sheet sheet = wb.createSheet("Баланс по ЦТП " + formatter.format(date));

        sheet.setDefaultColumnWidth(15);
        sheet.setColumnWidth(0, 2 * 256);

        Row row = sheet.createRow(1);
        createStyledCell(row, 2, "Период:", boldStyle);
        row.createCell(3).setCellValue(formatter.format(date));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 3, 4));

        row = sheet.createRow(2);
        createStyledCell(row, 2, "ЦТП №:", boldStyle);
        row.createCell(3).setCellValue(loader.getCTP(object));
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 3, 4));
        createStyledCell(row, 5, "Схема присоединения:", boldStyle);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 5, 6));
        row.createCell(7).setCellValue(loader.getConnectSchema(object));
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 7, 8));

        row = sheet.createRow(3);
        createStyledCell(row, 2, "Адрес ЦТП", boldStyle);
        row.createCell(3).setCellValue(loader.getAddress(object));
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 3, 8));

        row = sheet.createRow(4);
        createStyledCell(row, 2, "Филиал №:", boldStyle);
        row.createCell(3).setCellValue(loader.getFilial(object));
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 3, 4));
        createStyledCell(row, 5, "Предприятие №:", boldStyle);
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 5, 6));
        row.createCell(7).setCellValue(loader.getCompany(object));
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 7, 8));

        row = sheet.createRow(5);
        createStyledCell(row, 2, "Источник:", boldStyle);
        row.createCell(3).setCellValue(loader.getSource(object));
        sheet.addMergedRegion(new CellRangeAddress(5, 5, 3, 4));

        createStyledCell(sheet.createRow(7), 1, "Суммарный баланс тепла по ЦТП", boldCenterStyle);
        cellAddresses = new CellRangeAddress(7, 7, 1, 11);
        sheet.addMergedRegion(cellAddresses);
        pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        row = sheet.createRow(8);
        createStyledCell(row, 1, "Qвход в ЦТП", boldCenterStyle);
        cellAddresses = new CellRangeAddress(8, 8, 1, 1);
        pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        createStyledCell(row, 2, "Qвыход из ЦТП", boldCenterStyle);
        cellAddresses = new CellRangeAddress(8, 8, 2, 3);
        sheet.addMergedRegion(cellAddresses);
        pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        createStyledCell(row, 4, "Q всех строений", boldCenterStyle);
        cellAddresses = new CellRangeAddress(8, 8, 4, 5);
        sheet.addMergedRegion(cellAddresses);
        pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        createStyledCell(row, 6, "Q потерь", boldCenterStyle);
        cellAddresses = new CellRangeAddress(8, 8, 6, 7);
        sheet.addMergedRegion(cellAddresses);
        pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        createStyledCell(row, 8, "Q нормативных потерь", boldCenterStyle);
        cellAddresses = new CellRangeAddress(8, 8, 8, 9);
        sheet.addMergedRegion(cellAddresses);
        pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        createStyledCell(row, 10, "Q сверхнормативных потерь", boldCenterStyle);
        cellAddresses = new CellRangeAddress(8, 8, 10, 11);
        sheet.addMergedRegion(cellAddresses);
        pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        row = sheet.createRow(9);
        createStyledCell(row, 1, "Гкал", centerStyle);
        pt.drawBorders(new CellRangeAddress(9, 9, 1, 1), BorderStyle.THIN, BorderExtent.OUTSIDE);
        for (int i = 2; i <= 10; i+=2) {
            createStyledCell(row, i, "Гкал", centerStyle);
            pt.drawBorders(new CellRangeAddress(9, 9, i, i), BorderStyle.THIN, BorderExtent.OUTSIDE);
            createStyledCell(row, i + 1, "%", centerStyle);
            pt.drawBorders(new CellRangeAddress(9, 9, i + 1, i + 1), BorderStyle.THIN, BorderExtent.OUTSIDE);
        }

        row = sheet.createRow(12);
        createStyledCell(row, 1, "Входные параметры теплоносителя в ЦТП", boldCenterStyle);
        cellAddresses = new CellRangeAddress(12, 14, 1, 2);
        sheet.addMergedRegion(cellAddresses);
        pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        createStyledCell(row, 3, "Выходные параметры теплоносителя из ЦТП", boldCenterStyle);
        cellAddresses = new CellRangeAddress(12, 14, 3, 4);
        sheet.addMergedRegion(cellAddresses);
        pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        createStyledCell(row, 5, "Параметры теплоносителя на строениях", boldCenterStyle);
        cellAddresses = new CellRangeAddress(12, 14, 5, 5);
        sheet.addMergedRegion(cellAddresses);
        pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        List<ConsumerModel> objectNames = loader.getObjectNames(object);
        for (int i = 0; i < objectNames.size(); i++) {
            createStyledCell(row, 6 + i, objectNames.get(i).getName(), boldCenterStyle);
            cellAddresses = new CellRangeAddress(12, 14, 6 + i, 6 + i);
            sheet.addMergedRegion(cellAddresses);
            pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        }

        row = sheet.createRow(15);
        for (int i = 0; i < 2; i++) {
            createStyledCell(row, (2 * i) + 1, "Параметры", boldCenterStyle);
            createStyledCell(row, (2 * i) + 2, "Значения", boldCenterStyle);
            pt.drawBorders(new CellRangeAddress(15, 15, (i * 2) + 1, (i * 2) + 1), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
            pt.drawBorders(new CellRangeAddress(15, 15, (i * 2) + 2, (i * 2) + 2), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        }
        createStyledCell(row, 5, "Параметры", boldCenterStyle);
        pt.drawBorders(new CellRangeAddress(15, 15, 5, 5), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        for (int i = 0; i < objectNames.size(); i++) {
            createStyledCell(row, i + 6, "Значения", boldCenterStyle);
            pt.drawBorders(new CellRangeAddress(15, 15, i + 6, i + 6), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        }

        List<DataModel> inParams = loader.getInParameters(object, formatter.format(date));
        List<DataModel> outParams = loader.getOutParameters(object, formatter.format(date));
        int rowCount = Math.max(inParams.size(), outParams.size());

        List<BigDecimal> calcQ = new ArrayList<>();
        for (int i = 0; i < objectNames.size(); i++) {
            calcQ.add(new BigDecimal("0"));
        }

        for (int i = 0; i < rowCount; i++) {
            row = sheet.createRow(16 + i);

            if (i < inParams.size()) {
                createStyledCell(row, 1, inParams.get(i).getName(), borderStyle);
                createStyledCell(row, 2, inParams.get(i).getValue(), borderStyle);
            }

            if (i < outParams.size()) {
                createStyledCell(row, 3, outParams.get(i).getName(), borderStyle);
                createStyledCell(row, 4, outParams.get(i).getValue(), borderStyle);
                createStyledCell(row, 5, outParams.get(i).getName(), borderStyle);

                for (int j = 0; j < objectNames.size(); j++) {
                    String value = loader.getValue(object, objectNames.get(j).getId(), outParams.get(i).getId(),
                            outParams.get(i).getStatId(), formatter.format(date));
                    createStyledCell(row, 6 + j, value, borderStyle);
                    if (outParams.get(i).getName().startsWith("Q")) {
                        try {
                            calcQ.set(j, calcQ.get(j).add(new BigDecimal(value)));
                        } catch (Exception ignore) {
                        }
                    }
                }
            }
        }

        row = sheet.createRow(16 + rowCount);
        createStyledCell(row, 5, "Итого Q", borderStyle);
        for (int i = 0; i < objectNames.size(); i++) {
            createStyledCell(row, 6 + i, calcQ.get(i).toString(), borderStyle);
        }

        pt.drawBorders(new CellRangeAddress(16, 16 + rowCount - 1, 1, 2), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        pt.drawBorders(new CellRangeAddress(16, 16 + rowCount - 1, 3, 4), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        pt.drawBorders(new CellRangeAddress(16, 16 + rowCount, 5, 5 + objectNames.size()), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        row = sheet.createRow(10);
        createStyledCell(row, 1, calculateQ(inParams).toString(), borderStyle);
        createStyledCell(row, 2, calculateQ(outParams).toString(), borderStyle);
        createStyledCell(row, 3, calculatePercent(row.getCell(1).getStringCellValue(),
                row.getCell(2).getStringCellValue()), borderStyle);
        BigDecimal qConsumers = new BigDecimal("0");
        for (BigDecimal value: calcQ) {
            qConsumers = qConsumers.add(value);
        }
        createStyledCell(row, 4, qConsumers.toString(), borderStyle);
        createStyledCell(row, 5, calculatePercent(row.getCell(1).getStringCellValue(),
                row.getCell(4).getStringCellValue()), borderStyle);
        createStyledCell(row, 6, subtract(row.getCell(1).getStringCellValue(),
                row.getCell(4).getStringCellValue()), borderStyle);
        createStyledCell(row, 7, calculatePercent(row.getCell(1).getStringCellValue(),
                row.getCell(6).getStringCellValue()), borderStyle);
        for (int i = 0; i < 4; i++) {
            createStyledCell(row, 8 + i, "", borderStyle);
        }

        pt.drawBorders(new CellRangeAddress(7, 10, 1, 11), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        pt.applyBorders(sheet);
    }

    private static CellStyle getBorderStyle(Workbook wb) {
        CellStyle borderStyle = wb.createCellStyle();
        borderStyle.setBorderBottom(BorderStyle.THIN);
        borderStyle.setBorderLeft(BorderStyle.THIN);
        borderStyle.setBorderTop(BorderStyle.THIN);
        borderStyle.setBorderRight(BorderStyle.THIN);
        return borderStyle;
    }

    private static CellStyle getCenterStyle(Workbook wb) {
        CellStyle centerStyle = wb.createCellStyle();
        centerStyle.setAlignment(HorizontalAlignment.CENTER);
        centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        centerStyle.setWrapText(true);
        return centerStyle;
    }

    private static CellStyle getBoldCenterStyle(Workbook wb) {
        XSSFFont font = (XSSFFont) wb.createFont();
        font.setBold(true);

        CellStyle boldCenterStyle = wb.createCellStyle();
        boldCenterStyle.setFont(font);
        boldCenterStyle.setAlignment(HorizontalAlignment.CENTER);
        boldCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        boldCenterStyle.setWrapText(true);

        return boldCenterStyle;
    }

    private static CellStyle getBoldStyle(Workbook wb) {
        XSSFFont font = (XSSFFont) wb.createFont();
        font.setBold(true);

        CellStyle boldStyle = wb.createCellStyle();
        boldStyle.setFont(font);

        return boldStyle;
    }

    private static BigDecimal calculateQ(List<DataModel> params) {
        BigDecimal result = new BigDecimal("0");
        for (DataModel model: params) {
            if (model.getName().startsWith("Q")) {
                try {
                    result = result.add(new BigDecimal(model.getValue()));
                } catch (Exception ignore) {
                }
            }
        }

        return result;
    }

    private static String calculatePercent(String val1, String val2) {
        try {
            return new BigDecimal(val2).abs()
                    .multiply(new BigDecimal(100))
                    .divide(new BigDecimal(val1).abs(), 2, RoundingMode.HALF_EVEN)
                    .toString();
        } catch (Exception ignore) {
        }
        return "";
    }

    private static void createStyledCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static String subtract(String val1, String val2) {
        try {
            return new BigDecimal(val1).subtract(new BigDecimal(val2)).toString();
        } catch (Exception ignore) {
        }
        return "";
    }
}

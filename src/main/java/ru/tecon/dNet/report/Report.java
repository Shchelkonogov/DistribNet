package ru.tecon.dNet.report;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.PropertyTemplate;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.tecon.dNet.report.model.CellValue;
import ru.tecon.dNet.report.model.ConsumerModel;
import ru.tecon.dNet.report.model.DataModel;
import ru.tecon.dNet.sBean.ReportBeanLocal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

public final class Report {

    private static final Logger LOGGER = Logger.getLogger(Report.class.getName());

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public static Workbook createMonthReport(int object, LocalDate startDate, LocalDate endDate, ReportBeanLocal loader) {
        Workbook wb = new XSSFWorkbook();

        PropertyTemplate pt = new PropertyTemplate();

        LOGGER.info("start month");
        createSheet(wb, startDate, endDate, object, loader, pt, getStyles(wb));
        LOGGER.info("end month");

        LocalDate end = Stream.of(endDate, LocalDate.now()).min(LocalDate::compareTo).get();

        for (LocalDate date = startDate; date.isBefore(end.plusDays(1)); date = date.plusDays(1)) {
            LOGGER.info("start " + date);
            createSheet(wb, date, date, object, loader, pt, getStyles(wb));
            LOGGER.info("end " + date);
        }

        return wb;
    }

    public static Workbook createDayReport(int object, LocalDate reportDate, ReportBeanLocal loader) {
        Workbook wb = new XSSFWorkbook();
        createSheet(wb, reportDate, reportDate, object, loader, new PropertyTemplate(), getStyles(wb));
        return wb;
    }

    private static void createSheet(Workbook wb, LocalDate startDate, LocalDate endDate, int object, ReportBeanLocal loader,
                                    PropertyTemplate pt, Map<MyStyleName, CellStyle> styleMap) {
        CellRangeAddress cellAddresses;

        String durationName;
        if (startDate.isEqual(endDate)) {
            durationName = FORMATTER.format(startDate);
        } else {
            durationName = FORMATTER.format(startDate) + " - " + FORMATTER.format(endDate);
        }

        Sheet sheet = wb.createSheet("Баланс по ЦТП " + durationName);

        sheet.setDefaultColumnWidth(15);
        sheet.setColumnWidth(0, 2 * 256);

        Row row = sheet.createRow(1);
        createStyledCell(row, 2, "Период:", styleMap.get(MyStyleName.BOLD));
        row.createCell(3).setCellValue(durationName);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 3, 4));

        row = sheet.createRow(2);
        createStyledCell(row, 2, "ЦТП №:", styleMap.get(MyStyleName.BOLD));
        row.createCell(3).setCellValue(loader.getCTP(object));
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 3, 4));
        createStyledCell(row, 5, "Схема присоединения:", styleMap.get(MyStyleName.BOLD));
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 5, 6));
        row.createCell(7).setCellValue(loader.getConnectSchema(object));
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 7, 8));

        row = sheet.createRow(3);
        createStyledCell(row, 2, "Адрес ЦТП", styleMap.get(MyStyleName.BOLD));
        row.createCell(3).setCellValue(loader.getAddress(object));
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 3, 8));

        row = sheet.createRow(4);
        createStyledCell(row, 2, "Филиал №:", styleMap.get(MyStyleName.BOLD));
        row.createCell(3).setCellValue(loader.getFilial(object));
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 3, 4));
        createStyledCell(row, 5, "Предприятие №:", styleMap.get(MyStyleName.BOLD));
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 5, 6));
        row.createCell(7).setCellValue(loader.getCompany(object));
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 7, 8));

        row = sheet.createRow(5);
        createStyledCell(row, 2, "Источник:", styleMap.get(MyStyleName.BOLD));
        row.createCell(3).setCellValue(loader.getSource(object));
        sheet.addMergedRegion(new CellRangeAddress(5, 5, 3, 4));

        createStyledCell(sheet.createRow(7), 1, "Суммарный баланс тепла по ЦТП", styleMap.get(MyStyleName.BOLD_CENTER));
        cellAddresses = new CellRangeAddress(7, 7, 1, 11);
        sheet.addMergedRegion(cellAddresses);
        pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        row = sheet.createRow(8);
        createStyledCell(row, 1, "Qвход в ЦТП", styleMap.get(MyStyleName.BOLD_CENTER));
        cellAddresses = new CellRangeAddress(8, 8, 1, 1);
        pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        createStyledCell(row, 2, "Qвыход из ЦТП", styleMap.get(MyStyleName.BOLD_CENTER));
        cellAddresses = new CellRangeAddress(8, 8, 2, 3);
        sheet.addMergedRegion(cellAddresses);
        pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        createStyledCell(row, 4, "Q всех строений", styleMap.get(MyStyleName.BOLD_CENTER));
        cellAddresses = new CellRangeAddress(8, 8, 4, 5);
        sheet.addMergedRegion(cellAddresses);
        pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        createStyledCell(row, 6, "Q потерь", styleMap.get(MyStyleName.BOLD_CENTER));
        cellAddresses = new CellRangeAddress(8, 8, 6, 7);
        sheet.addMergedRegion(cellAddresses);
        pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        createStyledCell(row, 8, "Q нормативных потерь", styleMap.get(MyStyleName.BOLD_CENTER));
        cellAddresses = new CellRangeAddress(8, 8, 8, 9);
        sheet.addMergedRegion(cellAddresses);
        pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        createStyledCell(row, 10, "Q сверхнормативных потерь", styleMap.get(MyStyleName.BOLD_CENTER));
        cellAddresses = new CellRangeAddress(8, 8, 10, 11);
        sheet.addMergedRegion(cellAddresses);
        pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        row = sheet.createRow(9);
        createStyledCell(row, 1, "Гкал", styleMap.get(MyStyleName.CENTER));
        pt.drawBorders(new CellRangeAddress(9, 9, 1, 1), BorderStyle.THIN, BorderExtent.OUTSIDE);
        for (int i = 2; i <= 10; i+=2) {
            createStyledCell(row, i, "Гкал", styleMap.get(MyStyleName.CENTER));
            pt.drawBorders(new CellRangeAddress(9, 9, i, i), BorderStyle.THIN, BorderExtent.OUTSIDE);
            createStyledCell(row, i + 1, "%", styleMap.get(MyStyleName.CENTER));
            pt.drawBorders(new CellRangeAddress(9, 9, i + 1, i + 1), BorderStyle.THIN, BorderExtent.OUTSIDE);
        }

        row = sheet.createRow(12);
        createStyledCell(row, 1, "Входные параметры теплоносителя в ЦТП", styleMap.get(MyStyleName.BOLD_CENTER));
        cellAddresses = new CellRangeAddress(12, 14, 1, 2);
        sheet.addMergedRegion(cellAddresses);
        pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        createStyledCell(row, 3, "Выходные параметры теплоносителя из ЦТП", styleMap.get(MyStyleName.BOLD_CENTER));
        cellAddresses = new CellRangeAddress(12, 14, 3, 4);
        sheet.addMergedRegion(cellAddresses);
        pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        createStyledCell(row, 5, "Параметры теплоносителя на строениях", styleMap.get(MyStyleName.BOLD_CENTER));
        cellAddresses = new CellRangeAddress(12, 14, 5, 5);
        sheet.addMergedRegion(cellAddresses);
        pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        List<ConsumerModel> objectNames = loader.getObjectNames(object);
        for (int i = 0; i < objectNames.size(); i++) {
            createStyledCell(row, 6 + i, objectNames.get(i).getName(), styleMap.get(MyStyleName.BOLD_CENTER));
            cellAddresses = new CellRangeAddress(12, 14, 6 + i, 6 + i);
            sheet.addMergedRegion(cellAddresses);
            pt.drawBorders(cellAddresses, BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        }

        row = sheet.createRow(15);
        for (int i = 0; i < 2; i++) {
            createStyledCell(row, (2 * i) + 1, "Параметры", styleMap.get(MyStyleName.BOLD_CENTER));
            createStyledCell(row, (2 * i) + 2, "Значения", styleMap.get(MyStyleName.BOLD_CENTER));
            pt.drawBorders(new CellRangeAddress(15, 15, (i * 2) + 1, (i * 2) + 1), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
            pt.drawBorders(new CellRangeAddress(15, 15, (i * 2) + 2, (i * 2) + 2), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        }
        createStyledCell(row, 5, "Параметры", styleMap.get(MyStyleName.BOLD_CENTER));
        pt.drawBorders(new CellRangeAddress(15, 15, 5, 5), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        for (int i = 0; i < objectNames.size(); i++) {
            createStyledCell(row, i + 6, "Значения", styleMap.get(MyStyleName.BOLD_CENTER));
            pt.drawBorders(new CellRangeAddress(15, 15, i + 6, i + 6), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        }

        List<DataModel> inParams = loader.getInParameters(object, startDate, endDate);
        List<DataModel> outParams = loader.getOutParameters(object, startDate, endDate);
        int rowCount = Math.max(inParams.size(), outParams.size());

        List<BigDecimal> calcQ = new ArrayList<>();
        for (int i = 0; i < objectNames.size(); i++) {
            calcQ.add(new BigDecimal("0"));
        }

        for (int i = 0; i < rowCount; i++) {
            row = sheet.createRow(16 + i);

            if (i < inParams.size()) {
                createStyledCell(row, 1, inParams.get(i).getName(), styleMap.get(MyStyleName.BORDER));
                createStyledCell(row, 2, inParams.get(i).getValue(), styleMap.get(getStyleName(inParams.get(i).getColorIndex())));
            }

            if (i < outParams.size()) {
                createStyledCell(row, 3, outParams.get(i).getName(), styleMap.get(MyStyleName.BORDER));
                createStyledCell(row, 4, outParams.get(i).getValue(), styleMap.get(getStyleName(outParams.get(i).getColorIndex())));
                createStyledCell(row, 5, outParams.get(i).getName(), styleMap.get(MyStyleName.BORDER));

                for (int j = 0; j < objectNames.size(); j++) {
                    CellValue cellValue = loader.getValue(object, objectNames.get(j).getId(), outParams.get(i).getId(),
                            outParams.get(i).getStatId(), startDate, endDate);

                    if (cellValue != null) {
                        String value = cellValue.getValue();
                        createStyledCell(row, 6 + j, value, styleMap.get(getStyleName(cellValue.getColorIndex())));

                        if (outParams.get(i).getName().startsWith("Q")) {
                            try {
                                calcQ.set(j, calcQ.get(j).add(new BigDecimal(value)));
                            } catch (Exception ignore) {
                            }
                        }
                    }
                }
            }
        }

        row = sheet.createRow(16 + rowCount);
        createStyledCell(row, 5, "Итого Q", styleMap.get(MyStyleName.BORDER));
        for (int i = 0; i < objectNames.size(); i++) {
            createStyledCell(row, 6 + i, calcQ.get(i).toString(), styleMap.get(MyStyleName.BORDER));
        }

        pt.drawBorders(new CellRangeAddress(16, 16 + rowCount - 1, 1, 2), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        pt.drawBorders(new CellRangeAddress(16, 16 + rowCount - 1, 3, 4), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        pt.drawBorders(new CellRangeAddress(16, 16 + rowCount, 5, 5 + objectNames.size()), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        row = sheet.createRow(10);
        List<String> totalValues = loader.getTotalData(object, startDate, endDate);
        if (totalValues.size() == 22) {
            for (int i = 0; i < 11; i++) {
                createStyledCell(row, i + 1, totalValues.get(2 * i), styleMap.get(getStyleName(Integer.parseInt(totalValues.get(2 * i + 1)))));
            }
        }

        sheet.createRow(18 + rowCount)
                .createCell(1)
                .setCellValue("Отчет сформирован " + LocalDateTime.now().format(DATE_TIME_FORMAT));

        pt.drawBorders(new CellRangeAddress(7, 10, 1, 11), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        pt.applyBorders(sheet);
    }

    /**
     * Получение имени стандартного стиля по индексу цвета
     * @param colorIndex индекс цвета
     * @return имя стиля
     */
    private static MyStyleName getStyleName(int colorIndex) {
        switch (colorIndex) {
            case 1: return MyStyleName.BORDER_COLOR_YELLOW;
            case 2: return MyStyleName.BORDER_COLOR_GREY;
            case 3: return MyStyleName.BORDER_TEXT_COLOR_RED;
            case 0:
            default: return MyStyleName.BORDER;
        }
    }

    /**
     * Создание ячейки с стилем
     * @param row строка
     * @param index колонка
     * @param value значение
     * @param style стиль
     */
    private static void createStyledCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /**
     * Создание стандартных стилей
     * @param wb рабочая книга excel
     * @return карта стилей
     */
    private static Map<MyStyleName, CellStyle> getStyles(Workbook wb) {
        Map<MyStyleName, CellStyle> result = new HashMap<>();

        Font font = wb.createFont();

        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        result.put(MyStyleName.BORDER_COLOR_YELLOW, style);

        style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        result.put(MyStyleName.BORDER_COLOR_GREY, style);

        style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        result.put(MyStyleName.BORDER, style);


        font.setColor(HSSFColor.HSSFColorPredefined.RED.getIndex());

        style = wb.createCellStyle();
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        result.put(MyStyleName.BORDER_TEXT_COLOR_RED, style);

        font = wb.createFont();
        font.setBold(true);

        style = wb.createCellStyle();
        style.setFont(font);
        result.put(MyStyleName.BOLD, style);

        style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        result.put(MyStyleName.CENTER, style);

        font = wb.createFont();
        font.setBold(true);

        style = wb.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        result.put(MyStyleName.BOLD_CENTER, style);
        return result;
    }
}

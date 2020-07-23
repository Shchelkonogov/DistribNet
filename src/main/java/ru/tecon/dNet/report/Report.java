package ru.tecon.dNet.report;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.PropertyTemplate;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.tecon.dNet.report.model.ConsumerModel;
import ru.tecon.dNet.report.model.DataModel;
import ru.tecon.dNet.sBean.ReportBeanLocal;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Report {

    public static void main(String[] args) {
        try (OutputStream stream = new FileOutputStream("C:/Programs/workbook.xlsx")) {
            createReport(464836, "20.10.2015", new Test()).write(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Workbook createReport(int object, String date, ReportBeanLocal loader) {
        Workbook wb = new XSSFWorkbook();

        XSSFFont font = (XSSFFont) wb.createFont();
        font.setBold(true);

        CellStyle boldStyle = wb.createCellStyle();
        boldStyle.setFont(font);

        CellStyle boldCenterStyle = wb.createCellStyle();
        boldCenterStyle.setFont(font);
        boldCenterStyle.setAlignment(HorizontalAlignment.CENTER);
        boldCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        boldCenterStyle.setWrapText(true);

        CellStyle borderStyle = wb.createCellStyle();
        borderStyle.setBorderBottom(BorderStyle.THIN);
        borderStyle.setBorderLeft(BorderStyle.THIN);
        borderStyle.setBorderTop(BorderStyle.THIN);
        borderStyle.setBorderRight(BorderStyle.THIN);

        PropertyTemplate pt = new PropertyTemplate();

        Sheet sheet = wb.createSheet("Баланс по ЦТП");
        sheet.setDefaultColumnWidth(15);

        Row row = sheet.createRow(1);
        createStyledCell(row, 1, "Период:", boldStyle);
        row.createCell(2).setCellValue(date);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 2, 3));

        row = sheet.createRow(2);
        createStyledCell(row, 1, "ЦТП №:", boldStyle);
        row.createCell(2).setCellValue(loader.getCTP(object));
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 2, 3));
        createStyledCell(row, 4, "Схема присоединения:", boldStyle);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 4, 5));
        row.createCell(6).setCellValue(loader.getConnectSchema(object));
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 6, 7));

        row = sheet.createRow(3);
        createStyledCell(row, 1, "Филиал №:", boldStyle);
        row.createCell(2).setCellValue(loader.getFilial(object));
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 2, 3));
        createStyledCell(row, 4, "Предприятие №:", boldStyle);
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 4, 5));
        row.createCell(6).setCellValue(loader.getCompany(object));
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 6, 7));

        row = sheet.createRow(4);
        createStyledCell(row, 1, "Источник:", boldStyle);
        row.createCell(2).setCellValue(loader.getSource(object));
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 2, 3));

        createStyledCell(sheet.createRow(6), 0, "Суммарный баланс тепла по ЦТП", boldCenterStyle);
        sheet.addMergedRegion(new CellRangeAddress(6, 6, 0, 5));
        pt.drawBorders(new CellRangeAddress(6, 6, 0, 5), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        row = sheet.createRow(7);
        createStyledCell(row, 0, "Qвход в ЦТП", borderStyle);
        createStyledCell(row, 2, "Qвыход из ЦТП", borderStyle);
        createStyledCell(row, 4, "Q всех строений", borderStyle);

        row = sheet.createRow(9);
        createStyledCell(row, 0, "Входные параметры теплоносителя в ЦТП", boldCenterStyle);
        sheet.addMergedRegion(new CellRangeAddress(9, 10, 0, 1));
        pt.drawBorders(new CellRangeAddress(9, 10, 0, 1), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        createStyledCell(row, 2, "Выходные параметры теплоносителя из ЦТП", boldCenterStyle);
        sheet.addMergedRegion(new CellRangeAddress(9, 10, 2, 3));
        pt.drawBorders(new CellRangeAddress(9, 10, 2, 3), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);

        List<ConsumerModel> objectNames = loader.getObjectNames(object);
        for (int i = 0; i < objectNames.size(); i++) {
            createStyledCell(row, 4 + (i * 2), "Параметры теплоносителя  по адресу: " + objectNames.get(i).getName(), boldCenterStyle);
            sheet.addMergedRegion(new CellRangeAddress(9, 10, 4 + (i * 2), 5 + (i * 2)));
            pt.drawBorders(new CellRangeAddress(9, 10, 4 + (i * 2), 5 + (i * 2)), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        }

        row = sheet.createRow(11);
        for (int i = 0; i < objectNames.size() + 2; i++) {
            createStyledCell(row, (2 * i), "Параметры", boldCenterStyle);
            createStyledCell(row, (2 * i) + 1, "Значения", boldCenterStyle);
            pt.drawBorders(new CellRangeAddress(11, 11, (i * 2), (i * 2)), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
            pt.drawBorders(new CellRangeAddress(11, 11, (i * 2) + 1, (i * 2) + 1), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        }

        List<DataModel> inParams = loader.getInParameters(object, date);
        List<DataModel> outParams = loader.getOutParameters(object, date);
        int rowCount = Math.max(inParams.size(), outParams.size());

        List<BigDecimal> calcQ = new ArrayList<>();
        for (int i = 0; i < objectNames.size(); i++) {
            calcQ.add(new BigDecimal("0"));
        }

        for (int i = 0; i < rowCount; i++) {
            row = sheet.createRow(12 + i);

            if (i < inParams.size()) {
                createStyledCell(row, 0, inParams.get(i).getName(), borderStyle);
                createStyledCell(row, 1, inParams.get(i).getValue(), borderStyle);
            }

            if (i < outParams.size()) {
                createStyledCell(row, 2, outParams.get(i).getName(), borderStyle);
                createStyledCell(row, 3, outParams.get(i).getValue(), borderStyle);

                for (int j = 0; j < objectNames.size(); j++) {
                    createStyledCell(row, 4 + (j * 2), outParams.get(i).getName(), borderStyle);
                    String value = loader.getValue(objectNames.get(j).getId(), outParams.get(i).getId(),
                            outParams.get(i).getStatId(), date);
                    createStyledCell(row, 5 + (j * 2), value, borderStyle);
                    if (outParams.get(i).getName().startsWith("Q")) {
                        try {
                            calcQ.set(j, calcQ.get(j).add(new BigDecimal(value)));
                        } catch (Exception ignore) {
                        }
                    }
                }
            }
        }

        row = sheet.createRow(12 + rowCount);
        for (int i = 0; i < objectNames.size(); i++) {
            createStyledCell(row, 4 + (i * 2), "Итого Q", borderStyle);
            createStyledCell(row, 5 + (i * 2), calcQ.get(i).toString(), borderStyle);
        }


        pt.drawBorders(new CellRangeAddress(12, 12 + rowCount - 1, 0, 1), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        pt.drawBorders(new CellRangeAddress(12, 12 + rowCount - 1, 2, 3), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        for (int i = 0; i < objectNames.size(); i++) {
            pt.drawBorders(new CellRangeAddress(12, 12 + rowCount, 4 + (2 * i), 4 + (2 * i) + 1), BorderStyle.MEDIUM, BorderExtent.OUTSIDE);
        }

        createStyledCell(sheet.getRow(7), 1, calculateQ(inParams).toString(), borderStyle);
        createStyledCell(sheet.getRow(7), 3, calculateQ(outParams).toString(), borderStyle);
        BigDecimal qConsumers = new BigDecimal("0");
        for (BigDecimal value: calcQ) {
            qConsumers = qConsumers.add(value);
        }
        createStyledCell(sheet.getRow(7), 5, qConsumers.toString(), borderStyle);

        pt.applyBorders(sheet);

        return wb;
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

    private static void createStyledCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
}

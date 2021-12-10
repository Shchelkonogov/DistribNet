package report;

import ru.tecon.dNet.report.Report;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class BalanceCTP {

    private static final DateTimeFormatter FORMATTER_DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public static void main(String[] args) {
        try (OutputStream streamDay = new FileOutputStream("C:/Programs/balanceCTPDay.xlsx");
             OutputStream streamMonth = new FileOutputStream("C:/Programs/balanceCTPMonth.xlsx")) {
            Report.createDayReport(464836, LocalDate.parse("20.10.2015", FORMATTER_DAY), new BalanceCTPData()).write(streamDay);
            Report.createMonthReport(464836, LocalDate.parse("20.10.2015", FORMATTER_DAY).withDayOfMonth(1), LocalDate.parse("20.10.2015", FORMATTER_DAY).withDayOfMonth(1).plusMonths(1), new BalanceCTPData()).write(streamMonth);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package ru.tecon.dNet.servlet;

import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Workbook;
import ru.tecon.dNet.report.Report;
import ru.tecon.dNet.sBean.ReportBeanLocal;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/loadMonth")
public class LoadMonthReport extends HttpServlet {

    private static final Logger logger = Logger.getLogger(LoadMonthReport.class.getName());
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @EJB(name = "report")
    private ReportBeanLocal bean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        int object = Integer.parseInt(req.getParameter("object"));
        String date = req.getParameter("date");

        resp.setContentType("application/vnd.ms-excel; charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" +
                URLEncoder.encode("Баланс", StandardCharsets.UTF_8) + " " +
                URLEncoder.encode("по", StandardCharsets.UTF_8) + " " +
                URLEncoder.encode("ЦТП", StandardCharsets.UTF_8) + " " +
                URLEncoder.encode("(месяц).xlsx", StandardCharsets.UTF_8) + "\"");
        resp.setCharacterEncoding("UTF-8");

        LocalDate startDate = LocalDate.parse(date, FORMATTER).withDayOfMonth(1);
        try (OutputStream output = resp.getOutputStream();
             Workbook workbook = Report.createMonthReport(object, startDate, startDate.plusMonths(1), bean)) {
            workbook.write(output);
            output.flush();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error create month report", e);
        }
    }
}

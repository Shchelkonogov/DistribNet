package ru.tecon.dNet.servlet;

import ru.tecon.dNet.report.Report;
import ru.tecon.dNet.sBean.ReportBeanLocal;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@WebServlet("/loadDay")
public class LoadDayReport extends HttpServlet {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @EJB(name = "report")
    private ReportBeanLocal bean;


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int object = Integer.parseInt(req.getParameter("object"));
        String date = req.getParameter("date");

        resp.setContentType("application/vnd.ms-excel; charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode("Баланс по ЦТП (сутки).xlsx", "UTF-8") + "\"");
        resp.setCharacterEncoding("UTF-8");

        try (OutputStream output = resp.getOutputStream()) {
            Report.createDayReport(object, LocalDate.parse(date,FORMATTER), bean).write(output);
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

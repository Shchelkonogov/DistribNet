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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@WebServlet("/load")
public class LoadReport extends HttpServlet {

    @EJB(name = "report")
    private ReportBeanLocal bean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws UnsupportedEncodingException {
        int object = Integer.parseInt(req.getParameter("object"));
        String date = req.getParameter("date");

        resp.setContentType("application/vnd.ms-excel; charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode("Отчет.xlsx", "UTF-8") + "\"");
        resp.setCharacterEncoding("UTF-8");

        try (OutputStream output = resp.getOutputStream()) {
            Report.createReport(object, date, bean).write(output);
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package com.dv.gtusach.server;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dv.gtusach.server.gae.BookMakerGAE;
import com.dv.gtusach.shared.Book;

@SuppressWarnings("serial")
public class DownloadBookServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(DownloadBookServlet.class
			.getCanonicalName());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		downloadBook(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		downloadBook(req, resp);
	}

	private void downloadBook(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		//resp.setContentType("application/json; charset=utf-8");
		resp.setHeader("Cache-Control", "no-cache");
		final String bookId = req.getParameter("bookId");
		if (bookId == null || bookId.length() == 0) {
			log.log(Level.WARNING, "Missing bookId!");
			resp.setContentType("application/json; charset=utf-8");
			return;
		}

		BookMakerGAE bookMaker = new BookMakerGAE();
		bookMaker.setContext(getServletContext());
		Book book = bookMaker.getPersistence().findBook(bookId);
		byte[] data = bookMaker.getPersistence().loadBookData(bookId);
		if (book != null && data != null) {
			try {
				resp.setContentType("application/epub+zip");
				resp.setContentLength(data.length);
				//resp.setHeader("Content-Disposition", "attachment; filename=\"" + book.getTitle() + ".epub\"");
				//resp.setHeader("Content-Disposition", "attachment; filename*=\"utf-8''"
				//		+ book.getTitle() + ".epub");
				ServletOutputStream op = resp.getOutputStream();
				op.write(data);				
				op.flush();
				op.close();
			} catch (Exception ex) {
				log.log(Level.WARNING, "Failed to download book: " + bookId);
				return;
			}
		}
	}

}
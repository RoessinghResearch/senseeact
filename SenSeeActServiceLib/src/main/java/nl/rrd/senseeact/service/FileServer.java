package nl.rrd.senseeact.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.http.HttpRange;
import nl.rrd.senseeact.service.exception.BadRequestException;
import nl.rrd.senseeact.service.exception.HttpException;

import java.io.*;

public class FileServer {
	public static void readFile(HttpServletRequest request,
			HttpServletResponse response, File file, String contentType)
			throws HttpException, IOException {
		try (InputStream input = new FileInputStream(file)) {
			readFile(request, response, input, file.length(), contentType);
		}
	}

	public static void readFile(HttpServletRequest request,
			HttpServletResponse response, InputStream file, long fileLength,
			String contentType) throws HttpException, IOException {
		response.setHeader("Accept-Ranges", "bytes");
		String rangeValue = request.getHeader("Range");
		HttpRange range = null;
		try {
			if (rangeValue != null)
				range = HttpRange.parse(rangeValue);
		} catch (ParseException ex) {
			throw new BadRequestException("Invalid value for header Range: " +
					rangeValue);
		}
		HttpRange.Interval rangeInterval = null;
		if (range != null) {
			if (!range.getUnit().equals("bytes")) {
				throw new BadRequestException("Invalid unit in header Range: " +
						range.getUnit());
			}
			if (range.getIntervals().size() != 1) {
				throw new BadRequestException(
						"Multi-range request not supported");
			}
			rangeInterval = range.getIntervals().get(0);
		}
		if (range != null) {
			response.setStatus(206);
			response.setHeader("Content-Range", "bytes " + range + "/" +
					fileLength);
		}
		response.setHeader("Content-Length", Long.toString(getContentLength(
				fileLength, rangeInterval)));
		response.setHeader("Content-Type", contentType);
		if (!request.getMethod().equalsIgnoreCase("get"))
			return;
		try (OutputStream output = response.getOutputStream()) {
			copy(file, output, fileLength, rangeInterval);
		}
	}

	private static long getContentLength(long inputLength,
			HttpRange.Interval range) {
		if (range == null)
			return inputLength;
		else if (range.getStart() == null)
			return range.getEnd();
		else if (range.getEnd() == null)
			return inputLength - range.getStart();
		else
			return range.getEnd() - range.getStart();
	}

	private static void copy(InputStream input, OutputStream output,
			long inputLength, HttpRange.Interval range) throws IOException {
		long toSkip = 0;
		if (range != null && range.getStart() == null)
			toSkip = inputLength - range.getEnd();
		else if (range != null && range.getStart() != null)
			toSkip = range.getStart();
		long toRead = inputLength - toSkip;
		if (range != null && range.getStart() != null &&
				range.getEnd() != null) {
			toRead = range.getEnd() - range.getStart();
		}
		if (toRead <= 0)
			return;
		while (toSkip > 0) {
			long skipped = input.skip(toSkip);
			if (skipped == 0)
				throw new EOFException("End of file");
			toSkip -= skipped;
		}
		byte[] bs = new byte[4096];
		while (toRead > 0) {
			int batchSize = bs.length;
			if (toRead < batchSize)
				batchSize = (int)toRead;
			int len = input.read(bs, 0, batchSize);
			if (len == 0)
				throw new EOFException("End of file");
			output.write(bs, 0, len);
			toRead -= len;
		}
	}
}

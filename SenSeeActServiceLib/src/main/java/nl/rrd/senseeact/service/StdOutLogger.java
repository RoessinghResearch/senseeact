package nl.rrd.senseeact.service;

import nl.rrd.utils.datetime.DateTimeUtils;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.utils.xml.AbstractSimpleSAXHandler;
import nl.rrd.utils.xml.SimpleSAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;

import java.io.*;
import java.time.LocalDate;
import java.util.List;

public class StdOutLogger extends PrintStream {
	private StdOutLogger(OutputStream out) throws FileNotFoundException {
		super(out);
	}

	public static StdOutLogger createStdOut(String dataDir)
			throws ParseException, IOException {
		return create(dataDir, true);
	}

	public static StdOutLogger createStdErr(String dataDir)
			throws ParseException, IOException {
		return create(dataDir, false);
	}

	private static StdOutLogger create(String dataDir, boolean isStdOut)
			throws ParseException, IOException {
		String fileNamePattern;
		try (InputStream input = StdOutLogger.class.getClassLoader()
				.getResourceAsStream("logback.xml")) {
			SimpleSAXParser<String> parser = new SimpleSAXParser<>(
					new ExtractFileNamePatternHandler());
			fileNamePattern = parser.parse(new InputSource(input));
		}
		fileNamePattern = fileNamePattern.replace("${dataDir}", dataDir);
		String dateToken = "%d{yyyy-MM-dd}";
		int dateTokenPos = fileNamePattern.indexOf(dateToken);
		if (dateTokenPos == -1) {
			throw new ParseException(
					"Date token not found in file name pattern: " +
					fileNamePattern);
		}
		String beforeDate = fileNamePattern.substring(0, dateTokenPos);
		beforeDate += isStdOut ? "stdout." : "stderr.";
		String afterDate = fileNamePattern.substring(dateTokenPos +
				dateToken.length());
		return new StdOutLogger(new LogOutputStream(beforeDate, afterDate));
	}

	private static class ExtractFileNamePatternHandler
			extends AbstractSimpleSAXHandler<String> {
		private StringBuilder fileNamePattern = null;
		private boolean inFileNamePattern = false;

		@Override
		public void startElement(String name, Attributes atts,
				List<String> parents) throws ParseException {
			if (name.equals("fileNamePattern")) {
				inFileNamePattern = true;
				fileNamePattern = new StringBuilder();
			} else {
				inFileNamePattern = false;
			}
		}

		@Override
		public void endElement(String name, List<String> parents)
				throws ParseException {
			inFileNamePattern = parents.size() > 0 && parents.get(
					parents.size() - 1).equals("fileNamePattern");
			if (parents.isEmpty()) {
				if (fileNamePattern == null) {
					throw new ParseException(
							"Element \"fileNamePattern\" not found");
				}
			}
		}

		@Override
		public void characters(String ch, List<String> parents)
				throws ParseException {
			if (inFileNamePattern)
				fileNamePattern.append(ch);
		}

		@Override
		public String getObject() {
			return fileNamePattern.toString();
		}
	}

	private static class LogOutputStream extends OutputStream {
		private static final Object LOCK = new Object();

		private String beforeDate;
		private String afterDate;

		public LogOutputStream(String beforeDate, String afterDate) {
			this.beforeDate = beforeDate;
			this.afterDate = afterDate;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			synchronized (LOCK) {
				String date = LocalDate.now().format(DateTimeUtils.DATE_FORMAT);
				String path = beforeDate + date + afterDate;
				File file = new File(path).getCanonicalFile();
				try (FileOutputStream out = new FileOutputStream(file, true)) {
					out.write(b, off, len);
				}
			}
		}

		@Override
		public void write(int b) throws IOException {
			write(new byte[] { (byte)b }, 0, 1);
		}
	}
}

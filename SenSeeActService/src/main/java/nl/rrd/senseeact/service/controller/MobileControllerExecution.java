package nl.rrd.senseeact.service.controller;

import jakarta.servlet.http.HttpServletRequest;
import nl.rrd.senseeact.client.MobileApp;
import nl.rrd.senseeact.client.MobileAppRepository;
import nl.rrd.senseeact.client.exception.ErrorCode;
import nl.rrd.senseeact.client.exception.HttpError;
import nl.rrd.senseeact.client.exception.HttpFieldError;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseCriteria;
import nl.rrd.senseeact.dao.DatabaseSort;
import nl.rrd.senseeact.service.Configuration;
import nl.rrd.senseeact.service.ProtocolVersion;
import nl.rrd.senseeact.service.exception.BadRequestException;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.model.MobileWakeRequest;
import nl.rrd.senseeact.service.model.MobileWakeRequestTable;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.datetime.DateTimeUtils;
import nl.rrd.utils.io.FileUtils;
import nl.rrd.utils.validation.Validation;
import nl.rrd.utils.validation.ValidationException;
import org.slf4j.Logger;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MobileControllerExecution {
	private static final int MIN_WAKE_INTERVAL = 60;

	private final Map<String, List<Object>> userLockQueue =
			new LinkedHashMap<>();

	public Object writeMobileLogForAppCode(HttpServletRequest request,
			String appCode, String device, LocalDate date, long position,
			boolean zip, User user) throws HttpException, Exception {
		MobileAppRepository appRepo = AppComponents.get(
				MobileAppRepository.class);
		MobileApp app;
		try {
			app = appRepo.forCode(appCode);
		} catch (IllegalArgumentException ex) {
			String msg = "Invalid app code: " + appCode;
			HttpError error = new HttpError(ErrorCode.INVALID_INPUT, msg);
			error.addFieldError(new HttpFieldError("app", msg));
			throw new BadRequestException(error);
		}
		doWriteMobileLog(request, app, device, date, position, zip, user);
		return null;
	}

	private void doWriteMobileLog(HttpServletRequest request, MobileApp app,
			String device, LocalDate date, long position, boolean zip,
			User user) throws HttpException, Exception {
		Object currLock = new Object();
		synchronized (userLockQueue) {
			List<Object> locks = userLockQueue.computeIfAbsent(user.getUserid(),
					key -> new ArrayList<>());
			locks.add(currLock);
			while (locks.get(0) != currLock) {
				userLockQueue.wait();
			}
		}
		try {
			doWriteMobileLogLock(request, app, device, date, position, zip,
					user);
		} finally {
			synchronized (userLockQueue) {
				List<Object> locks = userLockQueue.get(user.getUserid());
				locks.remove(currLock);
				if (locks.isEmpty())
					userLockQueue.remove(user.getUserid());
				userLockQueue.notifyAll();
			}
		}
	}

	private void doWriteMobileLogLock(HttpServletRequest request,
			MobileApp app, String device, LocalDate date, long position,
			boolean zip, User user) throws HttpException, Exception {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		logger.info(
				"Start write mobile log {}.{} for user {} (device: {}, date: {}, position: {})",
				app.getName(), zip ? "zip" : "log", user.getUserid(), device,
				date.format(DateTimeUtils.DATE_FORMAT), position);
		if (device != null && device.length() > 0) {
			try {
				Validation.validateStringLength(device, 0, 64);
				Validation.validateStringRegex(device, "[a-zA-Z0-9\\-]+");
			} catch (ValidationException ex) {
				String msg = "Invalid device ID: " + device;
				HttpError error = new HttpError(ErrorCode.INVALID_INPUT, msg);
				error.addFieldError(new HttpFieldError("device", msg));
				throw new BadRequestException(error);
			}
		}
		Configuration config = AppComponents.get(Configuration.class);
		String dataPath = config.get(Configuration.DATA_DIR);
		if (dataPath == null) {
			throw new Exception("Configuration key \"" +
					Configuration.DATA_DIR + "\" not found");
		}
		File logDir = new File(dataPath, "mobilelog" + File.separator +
				app.getName() + File.separator + user.getUserid());
		if (device != null && device.length() > 0)
			logDir = new File(logDir, device);
		FileUtils.mkdir(logDir);
		int totalRead = 0;
		try (InputStream input = request.getInputStream()) {
			try (OutputStream output = prepareLogFile(logDir, date, position,
					zip)) {
				byte[] bs = new byte[4096];
				int read;
				while ((read = input.read(bs)) > 0) {
					output.write(bs, 0, read);
					totalRead += read;
				}
			}
		}
		logger.info("Wrote mobile log for user {}: {} bytes", user.getUserid(),
				totalRead);
	}

	private OutputStream prepareLogFile(File logDir, LocalDate date,
			long position, boolean zip) throws IOException {
		DateTimeFormatter shortDateFormat = DateTimeFormatter.ofPattern(
				"yyyyMMdd");
		String prefix = date.format(shortDateFormat);
		String ext = zip ? "zip" : "log";
		Pattern regex = Pattern.compile(prefix + "_([0-9]+)" + "\\." + ext);
		File[] existing = logDir.listFiles(pathname -> {
			Matcher m = regex.matcher(pathname.getName());
			return m.matches();
		});
		List<File> existingList = Arrays.asList(existing);
		existingList.sort(Comparator.comparing(File::getName));
		File prevFile = null;
		long prevLen = 0;
		File logFile;
		if (existingList.isEmpty()) {
			logFile = new File(logDir, date.format(shortDateFormat) +
					"_000000." + ext);
		} else {
			prevFile = existingList.get(existingList.size() - 1);
			Matcher m = regex.matcher(prevFile.getName());
			if (!m.matches()) {
				throw new RuntimeException(
						"Regular expression match failed unexpectedly");
			}
			int index = Integer.parseInt(m.group(1));
			prevLen = prevFile.length();
			if (position >= prevLen) {
				logFile = prevFile;
			} else {
				logFile = new File(logDir, date.format(shortDateFormat) + "_" +
						String.format("%06d", index + 1) + "." + ext);
			}
		}
		if (prevFile != null && position < prevLen && zip)
			FileUtils.copyFile(prevFile, logFile, 0, position);
		long logLen = logFile.length();
		OutputStream out = new FileOutputStream(logFile, true);
		if (position > logLen)
			writeZeros(out, position - logLen);
		return out;
	}

	private void writeZeros(OutputStream out, long len) throws IOException {
		byte[] fill = new byte[4096];
		Arrays.fill(fill, (byte)0);
		long remain = len;
		while (remain > 0) {
			int toWrite = fill.length;
			if (remain < toWrite)
				toWrite = (int)remain;
			out.write(fill, 0, toWrite);
			remain -= toWrite;
		}
	}

	public Object registerMobileWakeRequest(ProtocolVersion version,
			Database authDb, User user, String subject, String deviceId,
			String fcmToken, int interval) throws HttpException, Exception {
		User subjectUser = User.findAccessibleUser(version, subject, authDb,
				user);
		if (interval < MIN_WAKE_INTERVAL) {
			String msg = String.format("Interval must be at least %s seconds",
					MIN_WAKE_INTERVAL);
			throw BadRequestException.withInvalidInput(new HttpFieldError(
					"interval", msg));
		}
		MobileWakeRequestTable table = new MobileWakeRequestTable();
		DatabaseCriteria criteria = new DatabaseCriteria.And(
				new DatabaseCriteria.Equal("user", subjectUser.getUserid()),
				new DatabaseCriteria.Equal("deviceId", deviceId)
		);
		DatabaseSort[] sort = new DatabaseSort[] {
				new DatabaseSort("id", true)
		};
		MobileWakeRequest request = authDb.selectOne(table, criteria, sort);
		boolean isNew = request == null;
		if (isNew)
			request = new MobileWakeRequest();
		request.setUser(subjectUser.getUserid());
		request.setDeviceId(deviceId);
		request.setFcmToken(fcmToken);
		request.setInterval(interval);
		if (isNew)
			authDb.insert(table.getName(), request);
		else
			authDb.update(table.getName(), request);
		return null;
	}

	public Object unregisterMobileWakeRequest(ProtocolVersion version,
			Database authDb, User user, String subject, String deviceId)
			throws HttpException, Exception {
		User subjectUser = User.findAccessibleUser(version, subject, authDb,
				user);
		DatabaseCriteria criteria = new DatabaseCriteria.And(
				new DatabaseCriteria.Equal("user", subjectUser.getUserid()),
				new DatabaseCriteria.Equal("deviceId", deviceId)
		);
		MobileWakeRequestTable table = new MobileWakeRequestTable();
		authDb.delete(table, criteria);
		return null;
	}
}

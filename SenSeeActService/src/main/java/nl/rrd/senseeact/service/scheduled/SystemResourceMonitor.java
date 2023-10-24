package nl.rrd.senseeact.service.scheduled;

import nl.rrd.utils.AppComponents;
import nl.rrd.utils.datetime.DateTimeUtils;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.client.model.SystemStat;
import nl.rrd.senseeact.client.model.SystemStatTable;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseConnection;
import nl.rrd.senseeact.dao.DatabaseCriteria;
import nl.rrd.senseeact.service.DatabaseLoader;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SystemResourceMonitor {
	private static final String LOGTAG =
			SystemResourceMonitor.class.getSimpleName();
	private static final Object LOCK = new Object();
	
	private Logger logger;

	private LocalDate lastCleanDate = null;
	
	@Scheduled(fixedDelay=900000)
	public void runTask() {
		logger = AppComponents.getLogger(LOGTAG);
		DatabaseLoader dbLoader = DatabaseLoader.getInstance();
		try {
			DatabaseConnection dbConn = dbLoader.openConnection();
			try {
				Database authDb = dbLoader.initAuthDatabase(dbConn);
				runTask(authDb);
			} finally {
				dbConn.close();
			}
		} catch (IOException ex) {
			logger.error("Can't connect to database: " + ex.getMessage(), ex);
		} catch (DatabaseException ex) {
			logger.error("Database error: " + ex.getMessage(), ex);
		}
	}
	
	private void runTask(Database authDb) throws DatabaseException {
		LocalDate today = LocalDate.now();
		synchronized (LOCK) {
			if (lastCleanDate == null || lastCleanDate.isBefore(today)) {
				new Thread(() -> cleanData(today)).start();
			}
		}
		Runtime runtime = Runtime.getRuntime();
		ZonedDateTime now = DateTimeUtils.nowMs();
		OperatingSystemMXBean opBean =
			ManagementFactory.getOperatingSystemMXBean();
		List<SystemStat> stats = new ArrayList<>();
		stats.add(new SystemStat(SystemStat.Name.FREE_MEMORY,
				runtime.freeMemory(), now));
		stats.add(new SystemStat(SystemStat.Name.MAX_MEMORY,
				runtime.maxMemory(), now));
		stats.add(new SystemStat(SystemStat.Name.TOTAL_MEMORY,
				runtime.totalMemory(), now));
		double systemLoad = opBean.getSystemLoadAverage();
		if (systemLoad >= 0) {
			stats.add(new SystemStat(SystemStat.Name.SYSTEM_CPU_LOAD_AVG_1MIN,
					(long)(100*systemLoad), now));
		} // else not available
		Iterable<FileStore> fileStores = FileSystems.getDefault()
				.getFileStores();
		for (FileStore fileStore : fileStores) {
			String descr = fileStore.toString();
			Pattern regex = Pattern.compile("\\(([^\\(\\)]*)\\)$");
			Matcher m = regex.matcher(descr);
			String partition = null;
			if (m.find())
				partition = m.group(1);
			if (partition == null || (!partition.matches("[A-Z]:") &&
					!partition.startsWith("/dev/"))) {
				continue;
			}
			try {
				long total = fileStore.getTotalSpace();
				if (total == 0)
					continue;
				stats.add(new SystemStat(SystemStat.Name.TOTAL_FILE_STORE_SPACE,
						total, now, descr));
				stats.add(new SystemStat(
						SystemStat.Name.UNALLOCATED_FILE_STORE_SPACE,
						fileStore.getUnallocatedSpace(), now, descr));
				stats.add(new SystemStat(
						SystemStat.Name.USABLE_FILE_STORE_SPACE,
						fileStore.getUsableSpace(), now, descr));
			} catch (IOException ex) {
				logger.error("Can't get space details from file store " +
						descr + ": " + ex.getMessage());
			}
		}
		authDb.insert(SystemStatTable.NAME, stats);
	}

	private void cleanData(LocalDate today) {
		DatabaseLoader dbLoader = DatabaseLoader.getInstance();
		try {
			DatabaseConnection dbConn = dbLoader.openConnection();
			try {
				Database authDb = dbLoader.initAuthDatabase(dbConn);
				doCleanData(authDb, today);
			} finally {
				dbConn.close();
			}
		} catch (IOException ex) {
			logger.error("Can't connect to database: " + ex.getMessage(), ex);
		} catch (DatabaseException ex) {
			logger.error("Database error: " + ex.getMessage(), ex);
		}
	}

	private void doCleanData(Database authDb, LocalDate today)
			throws DatabaseException {
		SystemStatTable table = new SystemStatTable();
		LocalDate startDate = today.minusDays(30);
		long startMs = startDate.atStartOfDay(ZoneId.systemDefault())
				.toInstant().toEpochMilli();
		DatabaseCriteria criteria = new DatabaseCriteria.LessThan(
				"utcTime", startMs);
		authDb.delete(table, criteria);
		logger.info("Cleaned system stats before " +
				startDate.format(DateTimeUtils.DATE_FORMAT));
		synchronized (LOCK) {
			if (lastCleanDate == null || lastCleanDate.isBefore(today))
				lastCleanDate = today;
		}
	}
}

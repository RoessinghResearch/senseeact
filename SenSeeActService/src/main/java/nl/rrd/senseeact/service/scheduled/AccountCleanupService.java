package nl.rrd.senseeact.service.scheduled;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.datetime.DateTimeUtils;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.utils.exception.TaskException;
import nl.rrd.utils.schedule.*;
import nl.rrd.senseeact.dao.Database;
import nl.rrd.senseeact.dao.DatabaseConnection;
import nl.rrd.senseeact.service.DatabaseLoader;
import nl.rrd.senseeact.service.controller.UserController;
import nl.rrd.senseeact.service.exception.HttpException;
import nl.rrd.senseeact.service.model.User;
import nl.rrd.senseeact.service.model.UserCache;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Service
public class AccountCleanupService {
	private String cleanupTaskId = null;

	@PostConstruct
	public void init() {
		TaskScheduler scheduler = AppComponents.get(TaskScheduler.class);
		cleanupTaskId = scheduler.generateTaskId();
		scheduler.scheduleTask(null, new CleanupTask(), cleanupTaskId);
	}

	@PreDestroy
	public void destroy() {
		if (cleanupTaskId != null) {
			TaskScheduler scheduler = AppComponents.get(TaskScheduler.class);
			scheduler.cancelTask(null, cleanupTaskId);
			cleanupTaskId = null;
		}
	}

	private void runService() {
		String logtag = getClass().getSimpleName();
		Logger logger = AppComponents.getLogger(logtag);
		logger.info("Start " + logtag);
		DatabaseLoader dbLoader = DatabaseLoader.getInstance();
		DatabaseConnection dbConn;
		try {
			dbConn = dbLoader.openConnection();
		} catch (IOException ex) {
			logger.error("Failed to connect to database: " + ex.getMessage());
			return;
		}
		try {
			Database authDb = dbLoader.initAuthDatabase(dbConn);
			UserCache userCache = UserCache.getInstance();
			List<User> users = userCache.getUsers(User::isHasTemporaryEmail,
					Comparator.comparing(User::getEmail));
			for (User user : users) {
				ZonedDateTime now = DateTimeUtils.nowMs();
				ZonedDateTime created = user.getCreated();
				if (now.isBefore(created.plusHours(24)))
					continue;
				try {
					UserController.deleteUser(authDb, user);
				} catch (HttpException | DatabaseException | IOException ex) {
					logger.error("Failed to delete temporary user " +
							user.getEmail() + ": " + ex.getMessage(), ex);
					return;
				}
				logger.info("Deleted temporary user " + user.getEmail());
			}
		} catch (DatabaseException ex) {
			logger.error("Database error: " + ex.getMessage());
		} finally {
			dbConn.close();
		}
	}

	private class CleanupTask extends AbstractScheduledTask {
		public CleanupTask() {
			LocalDate today = LocalDate.now().minusDays(1);
			Random random = new Random();
			int second = random.nextInt(86400);
			LocalTime time = LocalTime.ofSecondOfDay(second);
			TaskSchedule.TimeSchedule schedule = new TaskSchedule.TimeSchedule(
					today, time);
			schedule.setRepeatDate(new DateDuration(1, DateUnit.DAY));
			setSchedule(schedule);
		}

		@Override
		public String getName() {
			return AccountCleanupService.class.getSimpleName() + "." +
					getClass().getSimpleName();
		}

		@Override
		public void run(Object context, String taskId, ZonedDateTime now,
				ScheduleParams scheduleParams) throws TaskException {
			runService();
		}
	}
}

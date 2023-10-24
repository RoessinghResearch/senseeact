package nl.rrd.senseeact.service;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServiceUserRunState {
	private static final Object STATIC_LOCK = new Object();
	
	private Map<ServiceUserKey,ZonedDateTime> serviceRunTimes =
			new LinkedHashMap<>();
	
	private static ServiceUserRunState instance = null;
	
	private ServiceUserRunState() {
	}
	
	public static ServiceUserRunState getInstance() {
		synchronized (STATIC_LOCK) {
			if (instance == null)
				instance = new ServiceUserRunState();
			return instance;
		}
	}
	
	/**
	 * Sets the time when a service has run for the specified user.
	 * 
	 * @param serviceName the service name
	 * @param user the user
	 * @param time the time
	 */
	public void setServiceRunTime(String serviceName, String user,
			ZonedDateTime time) {
		synchronized (STATIC_LOCK) {
			serviceRunTimes.put(new ServiceUserKey(serviceName, user), time);
		}
	}
	
	/**
	 * Returns whether a service has run for the specified user at or after
	 * the specified time.
	 * 
	 * @param serviceName the service name
	 * @param user the user
	 * @param time the time
	 * @return true if the service has run, false otherwise
	 */
	public boolean isServiceRunSince(String serviceName, String user,
			ZonedDateTime time) {
		synchronized (STATIC_LOCK) {
			ZonedDateTime lastRun = serviceRunTimes.get(new ServiceUserKey(
					serviceName, user));
			return lastRun != null && !lastRun.isBefore(time);
		}
	}

	private static class ServiceUserKey {
		private String serviceName;
		private String user;
		
		public ServiceUserKey(String serviceName, String user) {
			this.serviceName = serviceName;
			this.user = user;
		}

		@Override
		public int hashCode() {
			int result = 1;
			result = 31 * result + serviceName.hashCode();
			result = 31 * result + (user == null ? 0 : user.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ServiceUserKey other = (ServiceUserKey)obj;
			if (!serviceName.equals(other.serviceName))
				return false;
			if ((user == null) != (other.user == null))
				return false;
			else if (user != null && !user.equals(other.user))
				return false;
			return true;
		}
	}
}

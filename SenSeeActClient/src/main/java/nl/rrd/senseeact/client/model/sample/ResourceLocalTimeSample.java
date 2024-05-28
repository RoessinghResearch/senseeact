package nl.rrd.senseeact.client.model.sample;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nl.rrd.utils.exception.ParseException;
import nl.rrd.senseeact.dao.DatabaseFieldException;
import nl.rrd.senseeact.dao.DatabaseObjectMapper;

import java.time.LocalDateTime;

/**
 * The base class for a resource sample table (without a user) that stores
 * samples with a local time. Resource sample classes should always extend
 * either this class or {@link ResourceUTCSample ResourceUTCSample}.
 *
 * <p>This base class only defines the sample time. Subclasses should define the
 * sample value. The sample time is a local date/time, so it does not define a
 * time zone or UTC time.</p>
 *
 * <p>Set methods may throw a {@link ParseException ParseException}. In that
 * case the parameter value will be treated as illegal and the {@link
 * DatabaseObjectMapper DatabaseObjectMapper} will throw a {@link
 * DatabaseFieldException DatabaseFieldException}.</p>
 *
 * @author Dennis Hofs (RRD)
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ResourceLocalTimeSample extends ResourceSample {

	/**
	 * Constructs a new empty sample. This is used for DataAccessObjects and
	 * JSON serialization. Users should not call this.
	 */
	public ResourceLocalTimeSample() {
	}

	/**
	 * Constructs a new sample at the specified time.
	 *
	 * @param time the time
	 */
	public ResourceLocalTimeSample(LocalDateTime time) {
		setLocalTime(time.format(Sample.LOCAL_TIME_FORMAT));
	}

	/**
	 * Changes the date/time. It should define the local time and
	 * location-based time zone (not an offset).
	 *
	 * @param time the date/time
	 */
	public void updateLocalDateTime(LocalDateTime time) {
		setLocalTime(time.format(Sample.LOCAL_TIME_FORMAT));
	}
}

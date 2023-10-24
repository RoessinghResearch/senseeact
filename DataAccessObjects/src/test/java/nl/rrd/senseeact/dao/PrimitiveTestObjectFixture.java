package nl.rrd.senseeact.dao;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.Random;

import nl.rrd.utils.datetime.DateTimeUtils;

public class PrimitiveTestObjectFixture {
	private Random random = new Random();

	public PrimitiveTestObject createRandomTestObject(String user) {
		PrimitiveTestObject obj = new PrimitiveTestObject();
		obj.setUser(user);
		
		obj.setByteBooleanField(random.nextBoolean());
		obj.setByteBooleanObj(random.nextBoolean());
		obj.setByteByteField((byte)(random.nextInt(256) - 128));
		obj.setByteByteObj((byte)(random.nextInt(256) - 128));
		obj.setByteShortField((short)(random.nextInt(256) - 128));
		obj.setByteShortObj((short)(random.nextInt(256) - 128));
		obj.setByteIntField(random.nextInt(256) - 128);
		obj.setByteIntObj(random.nextInt(256) - 128);
		obj.setByteLongField((random.nextInt(256) - 128));
		obj.setByteLongObj((long)(random.nextInt(256) - 128));

		obj.setShortBooleanField(random.nextBoolean());
		obj.setShortBooleanObj(random.nextBoolean());
		obj.setShortByteField((byte)(random.nextInt(256) - 128));
		obj.setShortByteObj((byte)(random.nextInt(256) - 128));
		obj.setShortShortField((short)(random.nextInt(32768) - 16536));
		obj.setShortShortObj((short)(random.nextInt(32768) - 16536));
		obj.setShortIntField(random.nextInt(32768) - 16536);
		obj.setShortIntObj(random.nextInt(32768) - 16536);
		obj.setShortLongField((random.nextInt(32768) - 16536));
		obj.setShortLongObj((long)(random.nextInt(32768) - 16536));

		obj.setIntBooleanField(random.nextBoolean());
		obj.setIntBooleanObj(random.nextBoolean());
		obj.setIntByteField((byte)(random.nextInt(256) - 128));
		obj.setIntByteObj((byte)(random.nextInt(256) - 128));
		obj.setIntShortField((short)(random.nextInt(32768) - 16536));
		obj.setIntShortObj((short)(random.nextInt(32768) - 16536));
		obj.setIntIntField(random.nextInt());
		obj.setIntIntObj(random.nextInt());
		obj.setIntLongField((random.nextInt()));
		obj.setIntLongObj((long)(random.nextInt()));

		obj.setLongBooleanField(random.nextBoolean());
		obj.setLongBooleanObj(random.nextBoolean());
		obj.setLongByteField((byte)(random.nextInt(256) - 128));
		obj.setLongByteObj((byte)(random.nextInt(256) - 128));
		obj.setLongShortField((short)(random.nextInt(32768) - 16536));
		obj.setLongShortObj((short)(random.nextInt(32768) - 16536));
		obj.setLongIntField(random.nextInt());
		obj.setLongIntObj(random.nextInt());
		obj.setLongLongField(random.nextLong());
		obj.setLongLongObj(random.nextLong());

		obj.setFloatFloatField(random.nextFloat());
		obj.setFloatFloatObj(random.nextFloat());
		obj.setFloatDoubleField(random.nextFloat());
		obj.setFloatDoubleObj((double)random.nextFloat());

		obj.setDoubleFloatField(random.nextFloat());
		obj.setDoubleFloatObj(random.nextFloat());
		obj.setDoubleDoubleField(random.nextDouble());
		obj.setDoubleDoubleObj(random.nextDouble());

		obj.setStringField(randomString(255));
		PrimitiveTestEnum[] enumOptions = PrimitiveTestEnum.values();
		obj.setEnumField(enumOptions[random.nextInt(enumOptions.length)]);

		obj.setTextField(randomString(500));

		obj.setDateField(LocalDate.now());

		LocalTime time = LocalTime.now();
		time = time.withNano(0);
		obj.setTimeField(time);

		LocalDateTime dateTime = DateTimeUtils.nowLocalMs();
		dateTime = dateTime.withNano(0);
		obj.setDateTimeField(dateTime);

		ZonedDateTime now = DateTimeUtils.nowMs().withFixedOffsetZone();
		obj.setIsoDateTimeField(now);
		obj.setIsoCalendarField(GregorianCalendar.from(now));
		obj.setIsoLongField(now.toInstant().toEpochMilli());
		obj.setIsoLongObj(now.toInstant().toEpochMilli());
		obj.setIsoDateField(Date.from(now.toInstant()));
		obj.setIsoInstantField(now.toInstant());
		return obj;
	}

	public PrimitiveTestObject createMinTestObject(String user) {
		PrimitiveTestObject obj = new PrimitiveTestObject();
		obj.setUser(user);
		
		obj.setByteBooleanField(false);
		obj.setByteBooleanObj(false);
		obj.setByteByteField((byte)-128);
		obj.setByteByteObj((byte)-128);
		obj.setByteShortField((short)-128);
		obj.setByteShortObj((short)-128);
		obj.setByteIntField(-128);
		obj.setByteIntObj(-128);
		obj.setByteLongField(-128);
		obj.setByteLongObj((long)-128);

		obj.setShortBooleanField(false);
		obj.setShortBooleanObj(false);
		obj.setShortByteField((byte)-128);
		obj.setShortByteObj((byte)-128);
		obj.setShortShortField((short)-16536);
		obj.setShortShortObj((short)-16536);
		obj.setShortIntField(-16536);
		obj.setShortIntObj(-16536);
		obj.setShortLongField(-16536);
		obj.setShortLongObj((long)-16536);

		obj.setIntBooleanField(false);
		obj.setIntBooleanObj(false);
		obj.setIntByteField((byte)-128);
		obj.setIntByteObj((byte)-128);
		obj.setIntShortField((short)-16536);
		obj.setIntShortObj((short)-16536);
		obj.setIntIntField(Integer.MIN_VALUE);
		obj.setIntIntObj(Integer.MIN_VALUE);
		obj.setIntLongField(Integer.MIN_VALUE);
		obj.setIntLongObj((long)Integer.MIN_VALUE);

		obj.setLongBooleanField(false);
		obj.setLongBooleanObj(false);
		obj.setLongByteField((byte)-128);
		obj.setLongByteObj((byte)-128);
		obj.setLongShortField((short)-16536);
		obj.setLongShortObj((short)-16536);
		obj.setLongIntField(Integer.MIN_VALUE);
		obj.setLongIntObj(Integer.MIN_VALUE);
		obj.setLongLongField(Long.MIN_VALUE);
		obj.setLongLongObj(Long.MIN_VALUE);

		obj.setFloatFloatField((float) Integer.MIN_VALUE);
		obj.setFloatFloatObj((float) Integer.MIN_VALUE);
		obj.setFloatDoubleField(Integer.MIN_VALUE);
		obj.setFloatDoubleObj((double) Integer.MIN_VALUE);

		obj.setDoubleFloatField((float) Integer.MIN_VALUE);
		obj.setDoubleFloatObj((float) Integer.MIN_VALUE);
		obj.setDoubleDoubleField(Integer.MIN_VALUE);
		obj.setDoubleDoubleObj((double) Integer.MIN_VALUE);

		obj.setStringField("");
		PrimitiveTestEnum[] enumOptions = PrimitiveTestEnum.values();
		obj.setEnumField(enumOptions[0]);

		obj.setTextField("");

		obj.setDateField(LocalDate.now());

		LocalTime time = LocalTime.now();
		time = time.withNano(0);
		obj.setTimeField(time);

		LocalDateTime dateTime = DateTimeUtils.nowLocalMs();
		dateTime = dateTime.withNano(0);
		obj.setDateTimeField(dateTime);

		ZonedDateTime now = DateTimeUtils.nowMs().withFixedOffsetZone();
		obj.setIsoDateTimeField(now);
		obj.setIsoCalendarField(GregorianCalendar.from(now));
		obj.setIsoLongField(now.toInstant().toEpochMilli());
		obj.setIsoLongObj(now.toInstant().toEpochMilli());
		obj.setIsoDateField(Date.from(now.toInstant()));
		obj.setIsoInstantField(now.toInstant());
		return obj;
	}

	public PrimitiveTestObject createMaxTestObject(String user) {
		PrimitiveTestObject obj = new PrimitiveTestObject();
		obj.setUser(user);
		
		obj.setByteBooleanField(true);
		obj.setByteBooleanObj(true);
		obj.setByteByteField((byte)127);
		obj.setByteByteObj((byte)127);
		obj.setByteShortField((short)127);
		obj.setByteShortObj((short)127);
		obj.setByteIntField(127);
		obj.setByteIntObj(127);
		obj.setByteLongField(127);
		obj.setByteLongObj((long)127);

		obj.setShortBooleanField(true);
		obj.setShortBooleanObj(true);
		obj.setShortByteField((byte)127);
		obj.setShortByteObj((byte)127);
		obj.setShortShortField((short)16535);
		obj.setShortShortObj((short)16535);
		obj.setShortIntField(16535);
		obj.setShortIntObj(16535);
		obj.setShortLongField(16535);
		obj.setShortLongObj((long)16535);

		obj.setIntBooleanField(true);
		obj.setIntBooleanObj(true);
		obj.setIntByteField((byte)127);
		obj.setIntByteObj((byte)127);
		obj.setIntShortField((short)16535);
		obj.setIntShortObj((short)16535);
		obj.setIntIntField(Integer.MAX_VALUE);
		obj.setIntIntObj(Integer.MAX_VALUE);
		obj.setIntLongField(Integer.MAX_VALUE);
		obj.setIntLongObj((long)Integer.MAX_VALUE);

		obj.setLongBooleanField(true);
		obj.setLongBooleanObj(true);
		obj.setLongByteField((byte)127);
		obj.setLongByteObj((byte)127);
		obj.setLongShortField((short)16535);
		obj.setLongShortObj((short)16535);
		obj.setLongIntField(Integer.MAX_VALUE);
		obj.setLongIntObj(Integer.MAX_VALUE);
		obj.setLongLongField(Long.MAX_VALUE);
		obj.setLongLongObj(Long.MAX_VALUE);

		obj.setFloatFloatField((float)Integer.MAX_VALUE);
		obj.setFloatFloatObj((float)Integer.MAX_VALUE);
		obj.setFloatDoubleField(Integer.MAX_VALUE);
		obj.setFloatDoubleObj((double)Integer.MAX_VALUE);

		obj.setDoubleFloatField((float)Integer.MAX_VALUE);
		obj.setDoubleFloatObj((float)Integer.MAX_VALUE);
		obj.setDoubleDoubleField(Integer.MAX_VALUE);
		obj.setDoubleDoubleObj((double)Integer.MAX_VALUE);

		obj.setStringField(randomString(255));
		PrimitiveTestEnum[] enumOptions = PrimitiveTestEnum.values();
		obj.setEnumField(enumOptions[enumOptions.length - 1]);

		obj.setTextField(randomString(500));

		obj.setDateField(LocalDate.now());

		LocalTime time = LocalTime.now();
		time = time.withNano(0);
		obj.setTimeField(time);

		LocalDateTime dateTime = DateTimeUtils.nowLocalMs();
		dateTime = dateTime.withNano(0);
		obj.setDateTimeField(dateTime);

		ZonedDateTime now = DateTimeUtils.nowMs().withFixedOffsetZone();
		obj.setIsoDateTimeField(now);
		obj.setIsoCalendarField(GregorianCalendar.from(now));
		obj.setIsoLongField(now.toInstant().toEpochMilli());
		obj.setIsoLongObj(now.toInstant().toEpochMilli());
		obj.setIsoDateField(Date.from(now.toInstant()));
		obj.setIsoInstantField(now.toInstant());
		return obj;
	}

	private String randomString(int len) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < len; i++) {
			result.append((char)('a' + random.nextInt(26)));
		}
		return result.toString();
	}
}

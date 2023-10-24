package nl.rrd.senseeact.dao;

import java.time.*;
import java.util.Calendar;
import java.util.Date;

public class PrimitiveTestObject extends AbstractDatabaseObject {
	@DatabaseField(value=DatabaseType.STRING)
	private String user = null;
	
	@DatabaseField(value= DatabaseType.BYTE)
	private boolean byteBooleanField = false;
	@DatabaseField(value= DatabaseType.BYTE)
	private Boolean byteBooleanObj = null;
	@DatabaseField(value= DatabaseType.BYTE)
	private byte byteByteField = 0;
	@DatabaseField(value= DatabaseType.BYTE)
	private Byte byteByteObj = null;
	@DatabaseField(value= DatabaseType.BYTE)
	private short byteShortField = 0;
	@DatabaseField(value= DatabaseType.BYTE)
	private Short byteShortObj = null;
	@DatabaseField(value= DatabaseType.BYTE)
	private int byteIntField = 0;
	@DatabaseField(value= DatabaseType.BYTE)
	private Integer byteIntObj = null;
	@DatabaseField(value= DatabaseType.BYTE)
	private long byteLongField = 0;
	@DatabaseField(value= DatabaseType.BYTE)
	private Long byteLongObj = null;

	@DatabaseField(value= DatabaseType.SHORT)
	private boolean shortBooleanField = false;
	@DatabaseField(value= DatabaseType.SHORT)
	private Boolean shortBooleanObj = null;
	@DatabaseField(value= DatabaseType.SHORT)
	private byte shortByteField = 0;
	@DatabaseField(value= DatabaseType.SHORT)
	private Byte shortByteObj = null;
	@DatabaseField(value= DatabaseType.SHORT)
	private short shortShortField = 0;
	@DatabaseField(value= DatabaseType.SHORT)
	private Short shortShortObj = null;
	@DatabaseField(value= DatabaseType.SHORT)
	private int shortIntField = 0;
	@DatabaseField(value= DatabaseType.SHORT)
	private Integer shortIntObj = null;
	@DatabaseField(value= DatabaseType.SHORT)
	private long shortLongField = 0;
	@DatabaseField(value= DatabaseType.SHORT)
	private Long shortLongObj = null;

	@DatabaseField(value= DatabaseType.INT)
	private boolean intBooleanField = false;
	@DatabaseField(value= DatabaseType.INT)
	private Boolean intBooleanObj = null;
	@DatabaseField(value= DatabaseType.INT)
	private byte intByteField = 0;
	@DatabaseField(value= DatabaseType.INT)
	private Byte intByteObj = null;
	@DatabaseField(value= DatabaseType.INT)
	private short intShortField = 0;
	@DatabaseField(value= DatabaseType.INT)
	private Short intShortObj = null;
	@DatabaseField(value= DatabaseType.INT)
	private int intIntField = 0;
	@DatabaseField(value= DatabaseType.INT)
	private Integer intIntObj = null;
	@DatabaseField(value= DatabaseType.INT)
	private long intLongField = 0;
	@DatabaseField(value= DatabaseType.INT)
	private Long intLongObj = null;

	@DatabaseField(value= DatabaseType.LONG)
	private boolean longBooleanField = false;
	@DatabaseField(value= DatabaseType.LONG)
	private Boolean longBooleanObj = null;
	@DatabaseField(value= DatabaseType.LONG)
	private byte longByteField = 0;
	@DatabaseField(value= DatabaseType.LONG)
	private Byte longByteObj = null;
	@DatabaseField(value= DatabaseType.LONG)
	private short longShortField = 0;
	@DatabaseField(value= DatabaseType.LONG)
	private Short longShortObj = null;
	@DatabaseField(value= DatabaseType.LONG)
	private int longIntField = 0;
	@DatabaseField(value= DatabaseType.LONG)
	private Integer longIntObj = null;
	@DatabaseField(value= DatabaseType.LONG)
	private long longLongField = 0;
	@DatabaseField(value= DatabaseType.LONG)
	private Long longLongObj = null;

	@DatabaseField(value = DatabaseType.FLOAT)
	private float floatFloatField = 0;
	@DatabaseField(value = DatabaseType.FLOAT)
	private Float floatFloatObj = null;
	@DatabaseField(value = DatabaseType.FLOAT)
	private double floatDoubleField = 0;
	@DatabaseField(value = DatabaseType.FLOAT)
	private Double floatDoubleObj = null;

	@DatabaseField(value = DatabaseType.DOUBLE)
	private float doubleFloatField = 0;
	@DatabaseField(value = DatabaseType.DOUBLE)
	private Float doubleFloatObj = null;
	@DatabaseField(value = DatabaseType.DOUBLE)
	private double doubleDoubleField = 0;
	@DatabaseField(value = DatabaseType.DOUBLE)
	private Double doubleDoubleObj = null;

	@DatabaseField(value = DatabaseType.STRING)
	private String stringField = null;
	@DatabaseField(value = DatabaseType.STRING)
	private PrimitiveTestEnum enumField = null;

	@DatabaseField(value = DatabaseType.TEXT)
	private String textField = null;

	@DatabaseField(value = DatabaseType.DATE)
	private LocalDate dateField = null;

	@DatabaseField(value = DatabaseType.TIME)
	private LocalTime timeField = null;

	@DatabaseField(value = DatabaseType.DATETIME)
	private LocalDateTime dateTimeField = null;

	@DatabaseField(value = DatabaseType.ISOTIME)
	private ZonedDateTime isoDateTimeField = null;
	@DatabaseField(value = DatabaseType.ISOTIME)
	private Calendar isoCalendarField = null;
	@DatabaseField(value = DatabaseType.ISOTIME)
	private long isoLongField = 0;
	@DatabaseField(value = DatabaseType.ISOTIME)
	private Long isoLongObj = null;
	@DatabaseField(value = DatabaseType.ISOTIME)
	private Date isoDateField = null;
	@DatabaseField(value = DatabaseType.ISOTIME)
	private Instant isoInstantField = null;

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public boolean isByteBooleanField() {
		return byteBooleanField;
	}

	public void setByteBooleanField(boolean byteBooleanField) {
		this.byteBooleanField = byteBooleanField;
	}

	public Boolean getByteBooleanObj() {
		return byteBooleanObj;
	}

	public void setByteBooleanObj(Boolean byteBooleanObj) {
		this.byteBooleanObj = byteBooleanObj;
	}

	public byte getByteByteField() {
		return byteByteField;
	}

	public void setByteByteField(byte byteByteField) {
		this.byteByteField = byteByteField;
	}

	public Byte getByteByteObj() {
		return byteByteObj;
	}

	public void setByteByteObj(Byte byteByteObj) {
		this.byteByteObj = byteByteObj;
	}

	public short getByteShortField() {
		return byteShortField;
	}

	public void setByteShortField(short byteShortField) {
		this.byteShortField = byteShortField;
	}

	public Short getByteShortObj() {
		return byteShortObj;
	}

	public void setByteShortObj(Short byteShortObj) {
		this.byteShortObj = byteShortObj;
	}

	public int getByteIntField() {
		return byteIntField;
	}

	public void setByteIntField(int byteIntField) {
		this.byteIntField = byteIntField;
	}

	public Integer getByteIntObj() {
		return byteIntObj;
	}

	public void setByteIntObj(Integer byteIntObj) {
		this.byteIntObj = byteIntObj;
	}

	public long getByteLongField() {
		return byteLongField;
	}

	public void setByteLongField(long byteLongField) {
		this.byteLongField = byteLongField;
	}

	public Long getByteLongObj() {
		return byteLongObj;
	}

	public void setByteLongObj(Long byteLongObj) {
		this.byteLongObj = byteLongObj;
	}

	public boolean isShortBooleanField() {
		return shortBooleanField;
	}

	public void setShortBooleanField(boolean shortBooleanField) {
		this.shortBooleanField = shortBooleanField;
	}

	public Boolean getShortBooleanObj() {
		return shortBooleanObj;
	}

	public void setShortBooleanObj(Boolean shortBooleanObj) {
		this.shortBooleanObj = shortBooleanObj;
	}

	public byte getShortByteField() {
		return shortByteField;
	}

	public void setShortByteField(byte shortByteField) {
		this.shortByteField = shortByteField;
	}

	public Byte getShortByteObj() {
		return shortByteObj;
	}

	public void setShortByteObj(Byte shortByteObj) {
		this.shortByteObj = shortByteObj;
	}

	public short getShortShortField() {
		return shortShortField;
	}

	public void setShortShortField(short shortShortField) {
		this.shortShortField = shortShortField;
	}

	public Short getShortShortObj() {
		return shortShortObj;
	}

	public void setShortShortObj(Short shortShortObj) {
		this.shortShortObj = shortShortObj;
	}

	public int getShortIntField() {
		return shortIntField;
	}

	public void setShortIntField(int shortIntField) {
		this.shortIntField = shortIntField;
	}

	public Integer getShortIntObj() {
		return shortIntObj;
	}

	public void setShortIntObj(Integer shortIntObj) {
		this.shortIntObj = shortIntObj;
	}

	public long getShortLongField() {
		return shortLongField;
	}

	public void setShortLongField(long shortLongField) {
		this.shortLongField = shortLongField;
	}

	public Long getShortLongObj() {
		return shortLongObj;
	}

	public void setShortLongObj(Long shortLongObj) {
		this.shortLongObj = shortLongObj;
	}

	public boolean isIntBooleanField() {
		return intBooleanField;
	}

	public void setIntBooleanField(boolean intBooleanField) {
		this.intBooleanField = intBooleanField;
	}

	public Boolean getIntBooleanObj() {
		return intBooleanObj;
	}

	public void setIntBooleanObj(Boolean intBooleanObj) {
		this.intBooleanObj = intBooleanObj;
	}

	public byte getIntByteField() {
		return intByteField;
	}

	public void setIntByteField(byte intByteField) {
		this.intByteField = intByteField;
	}

	public Byte getIntByteObj() {
		return intByteObj;
	}

	public void setIntByteObj(Byte intByteObj) {
		this.intByteObj = intByteObj;
	}

	public short getIntShortField() {
		return intShortField;
	}

	public void setIntShortField(short intShortField) {
		this.intShortField = intShortField;
	}

	public Short getIntShortObj() {
		return intShortObj;
	}

	public void setIntShortObj(Short intShortObj) {
		this.intShortObj = intShortObj;
	}

	public int getIntIntField() {
		return intIntField;
	}

	public void setIntIntField(int intIntField) {
		this.intIntField = intIntField;
	}

	public Integer getIntIntObj() {
		return intIntObj;
	}

	public void setIntIntObj(Integer intIntObj) {
		this.intIntObj = intIntObj;
	}

	public long getIntLongField() {
		return intLongField;
	}

	public void setIntLongField(long intLongField) {
		this.intLongField = intLongField;
	}

	public Long getIntLongObj() {
		return intLongObj;
	}

	public void setIntLongObj(Long intLongObj) {
		this.intLongObj = intLongObj;
	}

	public boolean isLongBooleanField() {
		return longBooleanField;
	}

	public void setLongBooleanField(boolean longBooleanField) {
		this.longBooleanField = longBooleanField;
	}

	public Boolean getLongBooleanObj() {
		return longBooleanObj;
	}

	public void setLongBooleanObj(Boolean longBooleanObj) {
		this.longBooleanObj = longBooleanObj;
	}

	public byte getLongByteField() {
		return longByteField;
	}

	public void setLongByteField(byte longByteField) {
		this.longByteField = longByteField;
	}

	public Byte getLongByteObj() {
		return longByteObj;
	}

	public void setLongByteObj(Byte longByteObj) {
		this.longByteObj = longByteObj;
	}

	public short getLongShortField() {
		return longShortField;
	}

	public void setLongShortField(short longShortField) {
		this.longShortField = longShortField;
	}

	public Short getLongShortObj() {
		return longShortObj;
	}

	public void setLongShortObj(Short longShortObj) {
		this.longShortObj = longShortObj;
	}

	public int getLongIntField() {
		return longIntField;
	}

	public void setLongIntField(int longIntField) {
		this.longIntField = longIntField;
	}

	public Integer getLongIntObj() {
		return longIntObj;
	}

	public void setLongIntObj(Integer longIntObj) {
		this.longIntObj = longIntObj;
	}

	public long getLongLongField() {
		return longLongField;
	}

	public void setLongLongField(long longLongField) {
		this.longLongField = longLongField;
	}

	public Long getLongLongObj() {
		return longLongObj;
	}

	public void setLongLongObj(Long longLongObj) {
		this.longLongObj = longLongObj;
	}

	public float getFloatFloatField() {
		return floatFloatField;
	}

	public void setFloatFloatField(float floatFloatField) {
		this.floatFloatField = floatFloatField;
	}

	public Float getFloatFloatObj() {
		return floatFloatObj;
	}

	public void setFloatFloatObj(Float floatFloatObj) {
		this.floatFloatObj = floatFloatObj;
	}

	public double getFloatDoubleField() {
		return floatDoubleField;
	}

	public void setFloatDoubleField(double floatDoubleField) {
		this.floatDoubleField = floatDoubleField;
	}

	public Double getFloatDoubleObj() {
		return floatDoubleObj;
	}

	public void setFloatDoubleObj(Double floatDoubleObj) {
		this.floatDoubleObj = floatDoubleObj;
	}

	public float getDoubleFloatField() {
		return doubleFloatField;
	}

	public void setDoubleFloatField(float doubleFloatField) {
		this.doubleFloatField = doubleFloatField;
	}

	public Float getDoubleFloatObj() {
		return doubleFloatObj;
	}

	public void setDoubleFloatObj(Float doubleFloatObj) {
		this.doubleFloatObj = doubleFloatObj;
	}

	public double getDoubleDoubleField() {
		return doubleDoubleField;
	}

	public void setDoubleDoubleField(double doubleDoubleField) {
		this.doubleDoubleField = doubleDoubleField;
	}

	public Double getDoubleDoubleObj() {
		return doubleDoubleObj;
	}

	public void setDoubleDoubleObj(Double doubleDoubleObj) {
		this.doubleDoubleObj = doubleDoubleObj;
	}

	public String getStringField() {
		return stringField;
	}

	public void setStringField(String stringField) {
		this.stringField = stringField;
	}

	public PrimitiveTestEnum getEnumField() {
		return enumField;
	}

	public void setEnumField(PrimitiveTestEnum enumField) {
		this.enumField = enumField;
	}

	public String getTextField() {
		return textField;
	}

	public void setTextField(String textField) {
		this.textField = textField;
	}

	public LocalDate getDateField() {
		return dateField;
	}

	public void setDateField(LocalDate dateField) {
		this.dateField = dateField;
	}

	public LocalTime getTimeField() {
		return timeField;
	}

	public void setTimeField(LocalTime timeField) {
		this.timeField = timeField;
	}

	public LocalDateTime getDateTimeField() {
		return dateTimeField;
	}

	public void setDateTimeField(LocalDateTime dateTimeField) {
		this.dateTimeField = dateTimeField;
	}

	public ZonedDateTime getIsoDateTimeField() {
		return isoDateTimeField;
	}

	public void setIsoDateTimeField(ZonedDateTime isoDateTimeField) {
		this.isoDateTimeField = isoDateTimeField;
	}

	public Calendar getIsoCalendarField() {
		return isoCalendarField;
	}

	public void setIsoCalendarField(Calendar isoCalendarField) {
		this.isoCalendarField = isoCalendarField;
	}

	public long getIsoLongField() {
		return isoLongField;
	}

	public void setIsoLongField(long isoLongField) {
		this.isoLongField = isoLongField;
	}

	public Long getIsoLongObj() {
		return isoLongObj;
	}

	public void setIsoLongObj(Long isoLongObj) {
		this.isoLongObj = isoLongObj;
	}

	public Date getIsoDateField() {
		return isoDateField;
	}

	public void setIsoDateField(Date isoDateField) {
		this.isoDateField = isoDateField;
	}

	public Instant getIsoInstantField() {
		return isoInstantField;
	}

	public void setIsoInstantField(Instant isoInstantField) {
		this.isoInstantField = isoInstantField;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		PrimitiveTestObject that = (PrimitiveTestObject) o;
		if (getId() != null ? !getId().equals(that.getId()) :
				that.getId() != null) {
			return false;
		}

		if (byteBooleanField != that.byteBooleanField)
			return false;
		if (byteByteField != that.byteByteField)
			return false;
		if (byteShortField != that.byteShortField)
			return false;
		if (byteIntField != that.byteIntField)
			return false;
		if (byteLongField != that.byteLongField)
			return false;
		if (byteBooleanObj != null ?
				!byteBooleanObj.equals(that.byteBooleanObj) :
				that.byteBooleanObj != null) {
			return false;
		}
		if (byteByteObj != null ? !byteByteObj.equals(that.byteByteObj) :
				that.byteByteObj != null) {
			return false;
		}
		if (byteShortObj != null ? !byteShortObj.equals(that.byteShortObj) :
				that.byteShortObj != null) {
			return false;
		}
		if (byteIntObj != null ? !byteIntObj.equals(that.byteIntObj) :
				that.byteIntObj != null) {
			return false;
		}
		if (byteLongObj != null ? !byteLongObj.equals(that.byteLongObj) :
				that.byteLongObj != null) {
			return false;
		}

		if (shortBooleanField != that.shortBooleanField)
			return false;
		if (shortByteField != that.shortByteField)
			return false;
		if (shortShortField != that.shortShortField)
			return false;
		if (shortIntField != that.shortIntField)
			return false;
		if (shortLongField != that.shortLongField)
			return false;
		if (shortBooleanObj != null ?
				!shortBooleanObj.equals(that.shortBooleanObj) :
				that.shortBooleanObj != null) {
			return false;
		}
		if (shortByteObj != null ? !shortByteObj.equals(that.shortByteObj) :
				that.shortByteObj != null) {
			return false;
		}
		if (shortShortObj != null ? !shortShortObj.equals(that.shortShortObj) :
				that.shortShortObj != null) {
			return false;
		}
		if (shortIntObj != null ? !shortIntObj.equals(that.shortIntObj) :
				that.shortIntObj != null) {
			return false;
		}
		if (shortLongObj != null ? !shortLongObj.equals(that.shortLongObj) :
				that.shortLongObj != null) {
			return false;
		}

		if (intBooleanField != that.intBooleanField)
			return false;
		if (intByteField != that.intByteField)
			return false;
		if (intShortField != that.intShortField)
			return false;
		if (intIntField != that.intIntField)
			return false;
		if (intLongField != that.intLongField)
			return false;
		if (intBooleanObj != null ? !intBooleanObj.equals(that.intBooleanObj) :
				that.intBooleanObj != null) {
			return false;
		}
		if (intByteObj != null ? !intByteObj.equals(that.intByteObj) :
				that.intByteObj != null) {
			return false;
		}
		if (intShortObj != null ? !intShortObj.equals(that.intShortObj) :
				that.intShortObj != null) {
			return false;
		}
		if (intIntObj != null ? !intIntObj.equals(that.intIntObj) :
				that.intIntObj != null) {
			return false;
		}
		if (intLongObj != null ? !intLongObj.equals(that.intLongObj) :
				that.intLongObj != null) {
			return false;
		}

		if (longBooleanField != that.longBooleanField)
			return false;
		if (longByteField != that.longByteField)
			return false;
		if (longShortField != that.longShortField)
			return false;
		if (longIntField != that.longIntField)
			return false;
		if (longLongField != that.longLongField)
			return false;
		if (longBooleanObj != null ?
				!longBooleanObj.equals(that.longBooleanObj) :
				that.longBooleanObj != null) {
			return false;
		}
		if (longByteObj != null ? !longByteObj.equals(that.longByteObj) :
				that.longByteObj != null) {
			return false;
		}
		if (longShortObj != null ? !longShortObj.equals(that.longShortObj) :
				that.longShortObj != null) {
			return false;
		}
		if (longIntObj != null ? !longIntObj.equals(that.longIntObj) :
				that.longIntObj != null) {
			return false;
		}
		if (longLongObj != null ? !longLongObj.equals(that.longLongObj) :
				that.longLongObj != null) {
			return false;
		}

		if (!equalFloat(floatFloatField, that.floatFloatField))
			return false;
		if (!equalDouble(floatDoubleField, that.floatDoubleField))
			return false;
		if (!equalFloat(floatFloatObj, that.floatFloatObj))
			return false;
		if (!equalDouble(floatDoubleObj, that.floatDoubleObj))
			return false;

		if (!equalFloat(doubleFloatField, that.doubleFloatField))
			return false;
		if (!equalDouble(doubleDoubleField, that.doubleDoubleField))
			return false;
		if (!equalFloat(doubleFloatObj, that.doubleFloatObj))
			return false;
		if (!equalDouble(doubleDoubleObj, that.doubleDoubleObj))
			return false;

		if (stringField != null ? !stringField.equals(that.stringField) :
				that.stringField != null) {
			return false;
		}
		if (enumField != that.enumField)
			return false;

		if (textField != null ? !textField.equals(that.textField) :
				that.textField != null) {
			return false;
		}

		if (dateField != null ? !dateField.equals(that.dateField) :
				that.dateField != null) {
			return false;
		}

		if (timeField != null ? !timeField.equals(that.timeField) :
				that.timeField != null) {
			return false;
		}

		if (dateTimeField != null ? !dateTimeField.equals(that.dateTimeField) :
				that.dateTimeField != null) {
			return false;
		}

		if (isoDateTimeField != null ?
				!isoDateTimeField.equals(that.isoDateTimeField) :
				that.isoDateTimeField != null) {
			return false;
		}
		if (isoCalendarField != null ?
				!isoCalendarField.equals(that.isoCalendarField) :
				that.isoCalendarField != null) {
			return false;
		}
		if (isoLongField != that.isoLongField)
			return false;
		if (isoLongObj != null ? !isoLongObj.equals(that.isoLongObj) :
				that.isoLongObj != null) {
			return false;
		}
		if (isoDateField != null ? !isoDateField.equals(that.isoDateField) :
				that.isoDateField != null) {
			return false;
		}
		if (isoInstantField != null ?
				!isoInstantField.equals(that.isoInstantField) :
				that.isoInstantField != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return getId() != null ? getId().hashCode() : 0;
	}

	private boolean equalFloat(Float expected, Float actual) {
		if (expected == null)
			return actual == null;
		if (actual == null)
			return false;
		return equalDouble((double)expected, (double)actual);
	}

	private boolean equalDouble(Double expected, Double actual) {
		if (expected == null)
			return actual == null;
		if (actual == null)
			return false;
		int log10 = expected == 0 ? 0 : (int)Math.log10(Math.abs(expected));
		double delta = Math.pow(10.0, log10 - 4);
		return Math.abs(expected - actual) < delta;
	}
}

package nl.rrd.senseeact.dao;

public class SimpleTestObject extends AbstractDatabaseObject {
	@DatabaseField(value=DatabaseType.STRING)
	private String user;
	@DatabaseField(value=DatabaseType.INT)
	private int order;
	@DatabaseField(value=DatabaseType.STRING)
	private String key;
	@DatabaseField(value=DatabaseType.STRING)
	private String value;
	@DatabaseField(value=DatabaseType.STRING)
	private String extra;

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		SimpleTestObject that = (SimpleTestObject) o;
		if (getId() != null ? !getId().equals(that.getId()) :
				that.getId() != null) {
			return false;
		}
		if (order != that.order)
			return false;
		if (key != null ? !key.equals(that.key) : that.key != null)
			return false;
		if (value != null ? !value.equals(that.value) : that.value != null)
			return false;
		if (extra != null ? !extra.equals(that.extra) : that.extra != null)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return getId() != null ? getId().hashCode() : 0;
	}
}

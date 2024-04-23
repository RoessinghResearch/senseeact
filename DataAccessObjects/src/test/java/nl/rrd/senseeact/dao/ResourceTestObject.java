package nl.rrd.senseeact.dao;

public class ResourceTestObject extends AbstractDatabaseObject {
	@DatabaseField(value=DatabaseType.INT)
	private int value = 0;

	public ResourceTestObject() {
	}

	public ResourceTestObject(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}
}

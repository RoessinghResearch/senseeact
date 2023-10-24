package nl.rrd.senseeact.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SimpleTestObjectFixture {
	private Random random = new Random();
	private List<SimpleTestObject> testObjects =
			new ArrayList<SimpleTestObject>();
	private List<SelectTestResult<?>> selectTests =
			new ArrayList<SelectTestResult<?>>();

	public SimpleTestObjectFixture(String user) {
		for (int i = 0; i < 12; i++) {
			testObjects.add(createTestObject(user, i + 1));
		}
		DatabaseCriteria criteria = new DatabaseCriteria.Equal("order", 4);
		DatabaseSort[] sort = null;
		List<SimpleTestObject> result = selectObjects(4);
		selectTests.add(new SelectTestResult<SimpleTestObject>(
				"equal order", criteria, 0, sort, result));

		criteria = new DatabaseCriteria.Equal("order", "0");
		sort = null;
		result = selectObjects();
		selectTests.add(new SelectTestResult<SimpleTestObject>(
				"equal order not exists", criteria, 0, sort, result));

		criteria = new DatabaseCriteria.Equal("value", "yyy");
		sort = new DatabaseSort[] { new DatabaseSort("order", true) };
		result = selectObjects(4, 5, 6);
		selectTests.add(new SelectTestResult<SimpleTestObject>(
				"equal value, order asc", criteria, 0, sort, result));

		criteria = new DatabaseCriteria.Equal("value", "yyy");
		sort = new DatabaseSort[] { new DatabaseSort("order", true) };
		result = selectObjects(4, 5);
		selectTests.add(new SelectTestResult<SimpleTestObject>(
				"equal value, limit", criteria, 2, sort, result));

		criteria = new DatabaseCriteria.Equal("value", "yyy");
		sort = new DatabaseSort[] { new DatabaseSort("order", false) };
		result = selectObjects(6, 5, 4);
		selectTests.add(new SelectTestResult<SimpleTestObject>(
				"equal value, order desc", criteria, 0, sort, result));

		criteria = new DatabaseCriteria.Equal("extra", (String)null);
		sort = new DatabaseSort[] { new DatabaseSort("order", true) };
		result = selectObjects(3, 6, 9, 12);
		selectTests.add(new SelectTestResult<SimpleTestObject>(
				"equal null", criteria, 0, sort, result));

		criteria = new DatabaseCriteria.NotEqual("order", 4);
		sort = new DatabaseSort[] {
				new DatabaseSort("value", true),
				new DatabaseSort("order", true)
		};
		result = selectObjects(10, 11, 12, 7, 8, 9, 5, 6, 1, 2, 3);
		selectTests.add(new SelectTestResult<SimpleTestObject>(
				"not equal order, value asc, order asc",
				criteria, 0, sort, result));

		criteria = new DatabaseCriteria.NotEqual("value", "yyy");
		sort = new DatabaseSort[] {
				new DatabaseSort("value", false),
				new DatabaseSort("key", false)
		};
		result = selectObjects(3, 2, 1, 9, 8, 7, 12, 11, 10);
		selectTests.add(new SelectTestResult<SimpleTestObject>(
				"not equal value, value desc, key desc",
				criteria, 0, sort, result));

		criteria = new DatabaseCriteria.NotEqual("extra", (String)null);
		sort = new DatabaseSort[] { new DatabaseSort("order", true) };
		result = selectObjects(1, 2, 4, 5, 7, 8, 10, 11);
		selectTests.add(new SelectTestResult<SimpleTestObject>(
				"not equal null", criteria, 0, sort, result));

		criteria = new DatabaseCriteria.LessThan("order", 4);
		sort = new DatabaseSort[] { new DatabaseSort("order", true) };
		result = selectObjects(1, 2, 3);
		selectTests.add(new SelectTestResult<SimpleTestObject>(
				"less than order", criteria, 0, sort, result));

		criteria = new DatabaseCriteria.LessThan("value", "yyy");
		sort = new DatabaseSort[] { new DatabaseSort("order", true) };
		result = selectObjects(7, 8, 9, 10, 11, 12);
		selectTests.add(new SelectTestResult<SimpleTestObject>(
				"less then value", criteria, 0, sort, result));

		criteria = new DatabaseCriteria.GreaterThan("order", 4);
		sort = new DatabaseSort[] { new DatabaseSort("order", true) };
		result = selectObjects(5, 6, 7, 8, 9, 10, 11, 12);
		selectTests.add(new SelectTestResult<SimpleTestObject>(
				"greater than order", criteria, 0, sort, result));

		criteria = new DatabaseCriteria.GreaterThan("value", "yyy");
		sort = new DatabaseSort[] { new DatabaseSort("order", true) };
		result = selectObjects(1, 2, 3);
		selectTests.add(new SelectTestResult<SimpleTestObject>(
				"greater than value", criteria, 0, sort, result));

		criteria = new DatabaseCriteria.LessEqual("order", 4);
		sort = new DatabaseSort[] { new DatabaseSort("order", true) };
		result = selectObjects(1, 2, 3, 4);
		selectTests.add(new SelectTestResult<SimpleTestObject>(
				"less equal order", criteria, 0, sort, result));

		criteria = new DatabaseCriteria.LessEqual("value", "yyy");
		sort = new DatabaseSort[] { new DatabaseSort("order", true) };
		result = selectObjects(4, 5, 6, 7, 8, 9, 10, 11, 12);
		selectTests.add(new SelectTestResult<SimpleTestObject>(
				"less equal value", criteria, 0, sort, result));

		criteria = new DatabaseCriteria.GreaterEqual("order", 4);
		sort = new DatabaseSort[] { new DatabaseSort("order", true) };
		result = selectObjects(4, 5, 6, 7, 8, 9, 10, 11, 12);
		selectTests.add(new SelectTestResult<SimpleTestObject>(
				"greater equal order", criteria, 0, sort, result));

		criteria = new DatabaseCriteria.GreaterEqual("value", "yyy");
		sort = new DatabaseSort[] { new DatabaseSort("order", true) };
		result = selectObjects(1, 2, 3, 4, 5, 6);
		selectTests.add(new SelectTestResult<SimpleTestObject>(
				"greater equal value", criteria, 0, sort, result));

		criteria = new DatabaseCriteria.And(
				new DatabaseCriteria.GreaterThan("order", 4),
				new DatabaseCriteria.LessThan("key", "h")
		);
		sort = new DatabaseSort[] { new DatabaseSort("order", true) };
		result = selectObjects(5, 6, 7);
		selectTests.add(new SelectTestResult<SimpleTestObject>(
				"and", criteria, 0, sort, result));

		criteria = new DatabaseCriteria.Or(
				new DatabaseCriteria.LessThan("order", 5),
				new DatabaseCriteria.Equal("value", "www")
		);
		sort = new DatabaseSort[] { new DatabaseSort("order", true) };
		result = selectObjects(1, 2, 3, 4, 10, 11, 12);
		selectTests.add(new SelectTestResult<SimpleTestObject>(
				"or", criteria, 0, sort, result));

		criteria = new DatabaseCriteria.And(
				new DatabaseCriteria.GreaterThan("order", 5),
				new DatabaseCriteria.Or(
						new DatabaseCriteria.Equal("value", "yyy"),
						new DatabaseCriteria.Equal("value", "www")
				)
		);
		sort = new DatabaseSort[] { new DatabaseSort("order", true) };
		result = selectObjects(6, 10, 11, 12);
		selectTests.add(new SelectTestResult<SimpleTestObject>(
				"and or", criteria, 0, sort, result));
	}

	public List<SimpleTestObject> getInserts() {
		return testObjects;
	}

	public List<SelectTestResult<?>> getSelectTests() {
		return selectTests;
	}

	private List<SimpleTestObject> selectObjects(int... order) {
		List<SimpleTestObject> result = new ArrayList<SimpleTestObject>();
		for (int o : order) {
			result.add(findObject(o));
		}
		return result;
	}

	private SimpleTestObject findObject(int order) {
		for (SimpleTestObject obj : testObjects) {
			if (obj.getOrder() == order)
				return obj;
		}
		return null;
	}

	private SimpleTestObject createTestObject(String user, int order) {
		SimpleTestObject obj = new SimpleTestObject();
		obj.setUser(user);
		obj.setOrder(order);
		obj.setKey((char) ('a' + (order - 1)) + createRandomString(5));
		obj.setValue(repeatChar((char) ('z' - ((order - 1) / 3)), 3));
		obj.setExtra(order % 3 == 0 ? null : "extra");
		return obj;
	}

	private String createRandomString(int len) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < len; i++) {
			builder.append((char)('a' + random.nextInt(26)));
		}
		return builder.toString();
	}

	private String repeatChar(char c, int len) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < len; i++) {
			builder.append(c);
		}
		return builder.toString();
	}
}

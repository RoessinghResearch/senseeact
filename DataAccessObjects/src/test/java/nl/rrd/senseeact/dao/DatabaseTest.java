package nl.rrd.senseeact.dao;

import nl.rrd.utils.exception.DatabaseException;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class DatabaseTest {
	private DatabaseConnection dbConn;
	private String dbName;

	public DatabaseTest(DatabaseConnection dbConn, String dbName) {
		this.dbConn = dbConn;
		this.dbName = dbName;
	}

	private Database initDatabase(boolean splitByUser) throws Exception {
		List<DatabaseTableDef<?>> tableDefs = new ArrayList<>();
		tableDefs.add(new PrimitiveTestTable(splitByUser));
		tableDefs.add(new PrimitiveUpdateTestTable(splitByUser));
		tableDefs.add(new SimpleTestTable(splitByUser));
		tableDefs.add(new ResourceTestTable());
		return dbConn.initDatabase(dbName, tableDefs, false);
	}

	public void testInsertSelect() throws Exception {
		Database db = initDatabase(false);

		PrimitiveTestObject inserted = new PrimitiveTestObject();
		db.insert(PrimitiveTestTable.NAME, inserted);
		Assert.assertEquals(1, db.count(PrimitiveTestTable.NAME,
				PrimitiveTestObject.class, null));

		PrimitiveTestObject selected = db.selectOne(
				new PrimitiveTestTable(false), null, null);
		Assert.assertEquals(inserted, selected);

		db.delete(PrimitiveTestTable.NAME, selected);
		Assert.assertEquals(0, db.count(PrimitiveTestTable.NAME,
				PrimitiveTestObject.class, null));

		PrimitiveTestObjectFixture fixture = new PrimitiveTestObjectFixture();
		inserted = fixture.createMinTestObject("testuser");
		db.insert(PrimitiveTestTable.NAME, inserted);
		Assert.assertEquals(1, db.count(PrimitiveTestTable.NAME,
				PrimitiveTestObject.class, null));

		selected = db.selectOne(new PrimitiveTestTable(false), null, null);
		db.delete(PrimitiveTestTable.NAME, selected);
		Assert.assertEquals(inserted, selected);

		inserted = fixture.createMaxTestObject("testuser");
		db.insert(PrimitiveTestTable.NAME, inserted);
		Assert.assertEquals(1, db.count(PrimitiveTestTable.NAME,
				PrimitiveTestObject.class, null));

		selected = db.selectOne(new PrimitiveTestTable(false), null, null);
		db.delete(PrimitiveTestTable.NAME, selected);
		Assert.assertEquals(inserted, selected);

		List<PrimitiveTestObject> testObjList = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			testObjList.add(fixture.createRandomTestObject("testuser"));
		}
		db.insert(PrimitiveTestTable.NAME, testObjList);
		Assert.assertEquals(testObjList.size(),
				db.count(PrimitiveTestTable.NAME, PrimitiveTestObject.class,
				null));
		List<PrimitiveTestObject> selectedList = db.select(
				new PrimitiveTestTable(false), null, 0, null);
		Assert.assertEquals(new HashSet<>(testObjList),
				new HashSet<>(selectedList));
	}

	public void testInsertSelectSplitByUser() throws Exception {
		Database db = initDatabase(true);

		PrimitiveTestObjectFixture fixture = new PrimitiveTestObjectFixture();
		List<PrimitiveTestObject> testObjListUser1 = new ArrayList<>();
		List<PrimitiveTestObject> testObjListUser2 = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			testObjListUser1.add(fixture.createRandomTestObject("testuser1"));
			testObjListUser2.add(fixture.createRandomTestObject("testuser2"));
		}
		List<PrimitiveTestObject> testObjListAll = new ArrayList<>(
				testObjListUser1);
		testObjListAll.addAll(testObjListUser2);
		DatabaseException exception = null;
		try {
			db.insert(PrimitiveTestTable.NAME, testObjListAll);
		} catch (DatabaseException ex) {
			exception = ex;
		}
		Assert.assertNotNull(exception);
		db.insert(PrimitiveTestTable.NAME, testObjListUser1);
		db.insert(PrimitiveTestTable.NAME, testObjListUser2);
		
		exception = null;
		try {
			db.count(PrimitiveTestTable.NAME, PrimitiveTestObject.class, null);
		} catch (DatabaseException ex) {
			exception = ex;
		}
		Assert.assertNotNull(exception);

		exception = null;
		try {
			db.select(new PrimitiveTestTable(true), null, 0, null);
		} catch (DatabaseException ex) {
			exception = ex;
		}
		Assert.assertNotNull(exception);
		
		DatabaseCriteria criteria = new DatabaseCriteria.Equal(
				"user", "testuser1");
		Assert.assertEquals(testObjListUser1.size(),
				db.count(PrimitiveTestTable.NAME, PrimitiveTestObject.class,
				criteria));
		List<PrimitiveTestObject> selectedList = db.select(
				new PrimitiveTestTable(true), criteria, 0, null);
		Assert.assertEquals(new HashSet<>(testObjListUser1),
				new HashSet<>(selectedList));
	}

	public void testInsertSelectResource() throws Exception {
		Database db = initDatabase(false);

		ResourceTestTable table = new ResourceTestTable();
		ResourceTestObject inserted = new ResourceTestObject(1);
		db.insert(table.getName(), inserted);
		Assert.assertEquals(1, db.count(table.getName(), table.getDataClass(),
				null));

		ResourceTestObject selected = db.selectOne(table, null, null);
		Assert.assertEquals(inserted, selected);

		db.delete(table.getName(), selected);
		Assert.assertEquals(0, db.count(table.getName(), table.getDataClass(),
				null));

		List<ResourceTestObject> testObjList = new ArrayList<>();
		Random random = new Random();
		for (int i = 0; i < 10; i++) {
			testObjList.add(new ResourceTestObject(random.nextInt(100)));
		}
		db.insert(table.getName(), testObjList);
		Assert.assertEquals(testObjList.size(), db.count(table.getName(),
				table.getDataClass(), null));
		List<ResourceTestObject> selectedList = db.select(table, null, 0, null);
		Assert.assertEquals(new HashSet<>(testObjList),
				new HashSet<>(selectedList));
	}

	public void testUpdateDelete() throws Exception {
		Database db = initDatabase(false);
		PrimitiveTestObjectFixture fixture = new PrimitiveTestObjectFixture();

		PrimitiveTestObject insertedUnchanged = fixture.createRandomTestObject(
				"testuser");
		db.insert(PrimitiveUpdateTestTable.NAME, insertedUnchanged);
		PrimitiveTestObject insertedChanged = fixture.createRandomTestObject(
				"testuser");
		db.insert(PrimitiveUpdateTestTable.NAME, insertedChanged);

		PrimitiveTestObject updated = fixture.createRandomTestObject(
				"testuser");
		updated.setId(insertedChanged.getId());
		db.update(PrimitiveUpdateTestTable.NAME, updated);

		DatabaseCriteria criteria = new DatabaseCriteria.Equal("id",
				insertedUnchanged.getId());
		List<PrimitiveTestObject> selected = db.select(
				new PrimitiveUpdateTestTable(false), criteria, 0, null);
		Assert.assertEquals(1, selected.size());
		Assert.assertEquals(insertedUnchanged, selected.get(0));

		criteria = new DatabaseCriteria.Equal("id", insertedChanged.getId());
		selected = db.select(new PrimitiveUpdateTestTable(false), criteria, 0,
				null);
		Assert.assertEquals(1, selected.size());
		Assert.assertEquals(updated, selected.get(0));

		db.delete(PrimitiveUpdateTestTable.NAME, insertedUnchanged);
		selected = db.select(new PrimitiveUpdateTestTable(false), null, 0,
				null);
		Assert.assertEquals(1, selected.size());
		Assert.assertEquals(updated, selected.get(0));

		insertedUnchanged.setId(null);
		db.insert(PrimitiveUpdateTestTable.NAME, insertedUnchanged);
		criteria = new DatabaseCriteria.Equal("id", insertedUnchanged.getId());
		int count = db.count(PrimitiveUpdateTestTable.NAME,
				PrimitiveTestObject.class, criteria);
		Assert.assertEquals(1, count);
		db.delete(PrimitiveUpdateTestTable.NAME, insertedUnchanged);
		selected = db.select(new PrimitiveUpdateTestTable(false), null, 0,
				null);
		Assert.assertEquals(1, selected.size());
		Assert.assertEquals(updated, selected.get(0));
	}

	public void testUpdateDeleteSplitByUser() throws Exception {
		Database db = initDatabase(true);
		PrimitiveTestObjectFixture fixture = new PrimitiveTestObjectFixture();

		PrimitiveTestObject insertedUnchanged = fixture.createRandomTestObject(
				"testuser1");
		db.insert(PrimitiveUpdateTestTable.NAME, insertedUnchanged);
		db.insert(PrimitiveUpdateTestTable.NAME,
				fixture.createRandomTestObject("testuser2"));
		PrimitiveTestObject insertedChanged = fixture.createRandomTestObject(
				"testuser1");
		db.insert(PrimitiveUpdateTestTable.NAME, insertedChanged);
		db.insert(PrimitiveUpdateTestTable.NAME,
				fixture.createRandomTestObject("testuser2"));

		PrimitiveTestObject updated = fixture.createRandomTestObject(
				"testuser1");
		updated.setId(insertedChanged.getId());
		db.update(PrimitiveUpdateTestTable.NAME, updated);

		DatabaseCriteria criteria = new DatabaseCriteria.Equal("id",
				insertedUnchanged.getId());
		DatabaseException exception = null;
		try {
			db.select(new PrimitiveUpdateTestTable(true), criteria, 0, null);
		} catch (DatabaseException ex) {
			exception = ex;
		}
		Assert.assertNotNull(exception);

		criteria = new DatabaseCriteria.And(
			new DatabaseCriteria.Equal("user", "testuser1"),
			new DatabaseCriteria.Equal("id", insertedUnchanged.getId())
		);
		List<PrimitiveTestObject> selected = db.select(
				new PrimitiveUpdateTestTable(true), criteria, 0, null);
		Assert.assertEquals(1, selected.size());
		Assert.assertEquals(insertedUnchanged, selected.get(0));

		criteria = new DatabaseCriteria.And(
			new DatabaseCriteria.Equal("user", "testuser1"),
			new DatabaseCriteria.Equal("id", insertedChanged.getId())
		);
		selected = db.select(new PrimitiveUpdateTestTable(true), criteria, 0,
				null);
		Assert.assertEquals(1, selected.size());
		Assert.assertEquals(updated, selected.get(0));

		db.delete(PrimitiveUpdateTestTable.NAME, insertedUnchanged);
		criteria = new DatabaseCriteria.Equal("user", "testuser1");
		selected = db.select(new PrimitiveUpdateTestTable(true), criteria, 0,
				null);
		Assert.assertEquals(1, selected.size());
		Assert.assertEquals(updated, selected.get(0));

		insertedUnchanged.setId(null);
		db.insert(PrimitiveUpdateTestTable.NAME, insertedUnchanged);
		criteria = new DatabaseCriteria.And(
			new DatabaseCriteria.Equal("user", "testuser1"),
			new DatabaseCriteria.Equal("id", insertedUnchanged.getId())
		);
		int count = db.count(PrimitiveUpdateTestTable.NAME,
				PrimitiveTestObject.class, criteria);
		Assert.assertEquals(1, count);
		db.delete(PrimitiveUpdateTestTable.NAME, insertedUnchanged);
		criteria = new DatabaseCriteria.Equal("user", "testuser1");
		selected = db.select(new PrimitiveUpdateTestTable(true), criteria, 0,
				null);
		Assert.assertEquals(1, selected.size());
		Assert.assertEquals(updated, selected.get(0));
	}

	public void testUpdateDeleteResource() throws Exception {
		Database db = initDatabase(false);

		ResourceTestTable table = new ResourceTestTable();
		ResourceTestObject insertedUnchanged = new ResourceTestObject(1);
		db.insert(table.getName(), insertedUnchanged);
		ResourceTestObject insertedChanged = new ResourceTestObject(2);
		db.insert(table.getName(), insertedChanged);

		ResourceTestObject updated = new ResourceTestObject(3);
		updated.setId(insertedChanged.getId());
		db.update(table.getName(), updated);

		DatabaseCriteria criteria = new DatabaseCriteria.Equal("id",
				insertedUnchanged.getId());
		List<ResourceTestObject> selected = db.select(table, criteria, 0, null);
		Assert.assertEquals(1, selected.size());
		Assert.assertEquals(insertedUnchanged, selected.get(0));

		criteria = new DatabaseCriteria.Equal("id", insertedChanged.getId());
		selected = db.select(table, criteria, 0, null);
		Assert.assertEquals(1, selected.size());
		Assert.assertEquals(updated, selected.get(0));

		db.delete(table.getName(), insertedUnchanged);
		selected = db.select(table, null, 0, null);
		Assert.assertEquals(1, selected.size());
		Assert.assertEquals(updated, selected.get(0));

		insertedUnchanged.setId(null);
		db.insert(table.getName(), insertedUnchanged);
		criteria = new DatabaseCriteria.Equal("id", insertedUnchanged.getId());
		int count = db.count(table.getName(), table.getDataClass(), criteria);
		Assert.assertEquals(1, count);
		db.delete(table.getName(), insertedUnchanged);
		selected = db.select(table, null, 0, null);
		Assert.assertEquals(1, selected.size());
		Assert.assertEquals(updated, selected.get(0));
	}

	public void testSelectQuery() throws Exception {
		Database db = initDatabase(false);

		SimpleTestObjectFixture fixture = new SimpleTestObjectFixture(
				"testuser");
		db.insert(SimpleTestTable.NAME, fixture.getInserts());

		List<SelectTestResult<?>> selectTests = fixture.getSelectTests();
		for (SelectTestResult<?> selectTest : selectTests) {
			try {
				List<SimpleTestObject> result = db.select(
						new SimpleTestTable(false), selectTest.getCriteria(),
						selectTest.getLimit(), selectTest.getSort());
				Assert.assertArrayEquals(selectTest.getResult().toArray(),
						result.toArray());
			} catch (Throwable t) {
				throw new Exception("Select test failed: " +
						selectTest.getLabel() + ": " + t.getMessage(), t);
			}
		}
	}

	public void testSelectQuerySplitByUser() throws Exception {
		Database db = initDatabase(true);

		SimpleTestObjectFixture fixtureUser1 = new SimpleTestObjectFixture(
				"testuser1");
		db.insert(SimpleTestTable.NAME, fixtureUser1.getInserts());
		SimpleTestObjectFixture fixtureUser2 = new SimpleTestObjectFixture(
				"testuser2");
		db.insert(SimpleTestTable.NAME, fixtureUser2.getInserts());

		List<SelectTestResult<?>> selectTests = fixtureUser1.getSelectTests();
		for (SelectTestResult<?> selectTest : selectTests) {
			try {
				DatabaseCriteria criteria = new DatabaseCriteria.And(
					new DatabaseCriteria.Equal("user", "testuser1"),
					selectTest.getCriteria()
				);
				List<SimpleTestObject> result = db.select(
						new SimpleTestTable(true), criteria,
						selectTest.getLimit(), selectTest.getSort());
				Assert.assertArrayEquals(selectTest.getResult().toArray(),
						result.toArray());
			} catch (Throwable t) {
				throw new Exception("Select test failed: " +
						selectTest.getLabel() + ": " + t.getMessage(), t);
			}
		}
	}
}

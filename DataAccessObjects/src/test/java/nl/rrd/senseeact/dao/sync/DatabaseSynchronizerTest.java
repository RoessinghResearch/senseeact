package nl.rrd.senseeact.dao.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.rrd.utils.AppComponents;
import nl.rrd.utils.exception.DatabaseException;
import nl.rrd.senseeact.dao.*;
import nl.rrd.senseeact.dao.DatabaseAction.Action;
import nl.rrd.senseeact.dao.sync.SyncTestFixture.Source;
import org.junit.Assert;
import org.slf4j.Logger;

import java.util.*;

public class DatabaseSynchronizerTest {
	private DatabaseConnection serverDbConn;
	private String serverDbName;
	private DatabaseConnection client1DbConn;
	private String client1DbName;
	private DatabaseConnection client2DbConn;
	private String client2DbName;

	private SyncTestFixture fixture;
	private List<DatabaseTableDef<SyncTestUserObject>> userTables =
			new ArrayList<>();

	public DatabaseSynchronizerTest(
			DatabaseConnection serverDbConn, String serverDbName,
			DatabaseConnection client1DbConn, String client1DbName,
			DatabaseConnection client2DbConn, String client2DbName) {
		this.serverDbConn = serverDbConn;
		this.serverDbName = serverDbName;
		this.client1DbConn = client1DbConn;
		this.client1DbName = client1DbName;
		this.client2DbConn = client2DbConn;
		this.client2DbName = client2DbName;
		this.fixture = new SyncTestFixture();
		userTables.add(new SyncTestUser1Table());
		userTables.add(new SyncTestUser2Table());
	}

	private Database initDatabase(DatabaseConnection dbConn, String dbName)
			throws Exception {
		List<DatabaseTableDef<?>> tableDefs = new ArrayList<>();
		tableDefs.add(new SyncTestUser1Table());
		tableDefs.add(new SyncTestUser2Table());
		tableDefs.add(new ResourceTestTable());
		return dbConn.initDatabase(dbName, tableDefs, false);
	}

	public void testSynchronizer() throws Exception {
		testInserts();
		testSyncActions();
		testMergedActions();
	}
	
	private void testInserts() throws Exception {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		serverDbConn.dropDatabase(serverDbName);
		serverDbConn.dropDatabase(client1DbName);
		serverDbConn.dropDatabase(client2DbName);
		Database serverDb = initDatabase(serverDbConn, serverDbName);
		Database client1Db = initDatabase(client1DbConn, client1DbName);
		Database client2Db = initDatabase(client2DbConn, client2DbName);

		// write first stage
		for (DatabaseTableDef<SyncTestUserObject> table : userTables) {
			List<SyncTestUserObject> objects = fixture.getUserObjects(
					SyncTestFixture.Source.SERVER, table.getName(), 0);
			serverDb.insert(table.getName(), objects);
			logger.info("Wrote {} user objects to server db, table {}",
					objects.size(), table.getName());
		}
		ResourceTestTable resTable = new ResourceTestTable();
		List<ResourceTestObject> resObjects = fixture.getResourceObjects(0);
		serverDb.insert(resTable.getName(), resObjects);
		logger.info("Wrote {} resource objects to server db, table {}",
				resObjects.size(), resTable.getName());
		for (DatabaseTableDef<SyncTestUserObject> table : userTables) {
			List<SyncTestUserObject> objects = fixture.getUserObjects(
					SyncTestFixture.Source.CLIENT1, table.getName(), 0);
			client1Db.insert(table.getName(), objects);
			logger.info("Wrote {} user objects to client1 db, table {}",
					objects.size(), table.getName());
		}
		for (DatabaseTableDef<SyncTestUserObject> table : userTables) {
			List<SyncTestUserObject> objects = fixture.getUserObjects(
					SyncTestFixture.Source.CLIENT2, table.getName(), 0);
			client2Db.insert(table.getName(), objects);
			logger.info("Wrote {} user objects to client2 db, table {}",
					objects.size(), table.getName());
		}

		// synchronize from client1 to server
		logger.info("Synchronize from client1 to server");
		DatabaseSynchronizer syncRead = new DatabaseSynchronizer(
				SyncTestFixture.USER1);
		DatabaseSynchronizer syncWrite = new DatabaseSynchronizer(
				SyncTestFixture.USER1);
		List<SyncProgress> progress = syncWrite.getSyncProgress(serverDb);
		Assert.assertEquals(0, progress.size());
		List<DatabaseAction> actions = syncRead.readSyncActions(client1Db,
				progress, 0, null, List.of("server"));
		syncWrite.writeSyncActions(serverDb, actions, "client1");
		// try to write the same actions again
		syncWrite.writeSyncActions(serverDb, actions, "client1");
		assertEqualUserSets(serverDb, new SourceStage[]{
				new SourceStage(SyncTestFixture.Source.SERVER, 0),
				new SourceStage(SyncTestFixture.Source.CLIENT1, 0)
		});
		progress = syncWrite.getSyncProgress(serverDb);
		assertEqualProgress(actions, progress);

		// synchronize from server to client1
		logger.info("Synchronize from server to client1");
		syncRead = new DatabaseSynchronizer(SyncTestFixture.USER1);
		syncWrite = new DatabaseSynchronizer(SyncTestFixture.USER1);
		syncWrite.setAllowWriteResourceTables(true);
		progress = syncWrite.getSyncProgress(client1Db);
		Assert.assertEquals(0, progress.size());
		actions = syncRead.readSyncActions(serverDb, progress, 0, null,
				List.of("client1"));
		syncWrite.writeSyncActions(client1Db, actions, "server");
		// try to write the same actions again
		syncWrite.writeSyncActions(client1Db, actions, "server");
		assertEqualUserSets(client1Db, new SourceStage[]{
				new SourceStage(SyncTestFixture.Source.SERVER, 0),
				new SourceStage(SyncTestFixture.Source.CLIENT1, 0)
		}, "testuser1");
		progress = syncWrite.getSyncProgress(client1Db);
		assertEqualProgress(actions, progress);

		// synchronize from client2 to server
		logger.info("Synchronize from client2 to server");
		syncRead = new DatabaseSynchronizer(SyncTestFixture.USER2);
		syncWrite = new DatabaseSynchronizer(SyncTestFixture.USER2);
		progress = syncWrite.getSyncProgress(serverDb);
		Assert.assertEquals(0, progress.size());
		actions = syncRead.readSyncActions(client2Db, progress, 0, null,
				List.of("server"));
		syncWrite.writeSyncActions(serverDb, actions, "client2");
		assertEqualUserSets(serverDb, new SourceStage[]{
				new SourceStage(SyncTestFixture.Source.SERVER, 0),
				new SourceStage(SyncTestFixture.Source.CLIENT1, 0),
				new SourceStage(SyncTestFixture.Source.CLIENT2, 0)
		});
		progress = syncWrite.getSyncProgress(serverDb);
		assertEqualProgress(actions, progress);

		// synchronize from server to client2
		logger.info("Synchronize from server to client2");
		syncRead = new DatabaseSynchronizer(SyncTestFixture.USER2);
		syncWrite = new DatabaseSynchronizer(SyncTestFixture.USER2);
		syncWrite.setAllowWriteResourceTables(true);
		progress = syncWrite.getSyncProgress(client2Db);
		Assert.assertEquals(0, progress.size());
		actions = syncRead.readSyncActions(serverDb, progress, 0, null,
				List.of("client2"));
		syncWrite.writeSyncActions(client2Db, actions, "server");
		// try to write the same actions again
		syncWrite.writeSyncActions(client2Db, actions, "server");
		assertEqualUserSets(client2Db, new SourceStage[]{
				new SourceStage(SyncTestFixture.Source.SERVER, 0),
				new SourceStage(SyncTestFixture.Source.CLIENT2, 0)
		}, "testuser2");
		progress = syncWrite.getSyncProgress(client2Db);
		assertEqualProgress(actions, progress);

		// write second stage
		for (DatabaseTableDef<SyncTestUserObject> table : userTables) {
			List<SyncTestUserObject> objects = fixture.getUserObjects(
					SyncTestFixture.Source.SERVER, table.getName(), 1);
			serverDb.insert(table.getName(), objects);
			logger.info("Wrote {} user objects to server db, table {}",
					objects.size(), table.getName());
		}
		resObjects = fixture.getResourceObjects(1);
		serverDb.insert(resTable.getName(), resObjects);
		logger.info("Wrote {} resource objects to server db, table {}",
				resObjects.size(), resTable.getName());
		for (DatabaseTableDef<SyncTestUserObject> table : userTables) {
			List<SyncTestUserObject> objects = fixture.getUserObjects(
					SyncTestFixture.Source.CLIENT1, table.getName(), 1);
			client1Db.insert(table.getName(), objects);
			logger.info("Wrote {} user objects to client1 db, table {}",
					objects.size(), table.getName());
		}
		for (DatabaseTableDef<SyncTestUserObject> table : userTables) {
			List<SyncTestUserObject> objects = fixture.getUserObjects(
					SyncTestFixture.Source.CLIENT2, table.getName(), 1);
			client2Db.insert(table.getName(), objects);
			logger.info("Wrote {} user objects to client2 db, table {}",
					objects.size(), table.getName());
		}

		// synchronize from client1 to server
		logger.info("Synchronize from client1 to server");
		syncRead = new DatabaseSynchronizer(SyncTestFixture.USER1);
		syncWrite = new DatabaseSynchronizer(SyncTestFixture.USER1);
		progress = syncWrite.getSyncProgress(serverDb);
		actions = syncRead.readSyncActions(client1Db, progress, 0, null,
				List.of("server"));
		Assert.assertEquals(6, actions.size());
		syncWrite.writeSyncActions(serverDb, actions, "client1");
		// try to write the same actions again
		syncWrite.writeSyncActions(serverDb, actions, "client1");
		assertEqualUserSets(serverDb, new SourceStage[]{
				new SourceStage(SyncTestFixture.Source.SERVER, 0),
				new SourceStage(SyncTestFixture.Source.SERVER, 1),
				new SourceStage(SyncTestFixture.Source.CLIENT1, 0),
				new SourceStage(SyncTestFixture.Source.CLIENT1, 1),
				new SourceStage(SyncTestFixture.Source.CLIENT2, 0)
		});
		progress = syncWrite.getSyncProgress(serverDb);
		assertEqualProgress(actions, progress);

		// synchronize from server to client1
		logger.info("Synchronize from server to client1");
		syncRead = new DatabaseSynchronizer(SyncTestFixture.USER1);
		syncWrite = new DatabaseSynchronizer(SyncTestFixture.USER1);
		syncWrite.setAllowWriteResourceTables(true);
		progress = syncWrite.getSyncProgress(client1Db);
		actions = syncRead.readSyncActions(serverDb, progress, 0, null,
				List.of("client1"));
		Assert.assertEquals(9, actions.size());
		syncWrite.writeSyncActions(client1Db, actions, "server");
		// try to write the same actions again
		syncWrite.writeSyncActions(client1Db, actions, "server");
		assertEqualUserSets(client1Db, new SourceStage[]{
				new SourceStage(SyncTestFixture.Source.SERVER, 0),
				new SourceStage(SyncTestFixture.Source.SERVER, 1),
				new SourceStage(SyncTestFixture.Source.CLIENT1, 0),
				new SourceStage(SyncTestFixture.Source.CLIENT1, 1)
		}, "testuser1");
		progress = syncWrite.getSyncProgress(client1Db);
		assertEqualProgress(actions, progress);
	}

	public void testSyncActions() throws Exception {
		serverDbConn.dropDatabase(serverDbName);
		Database db = initDatabase(serverDbConn, serverDbName);
		String user = SyncTestFixture.USER1;
		DatabaseSynchronizer dbSync = new DatabaseSynchronizer(user);
		List<DatabaseAction> actions = new ArrayList<>();
		SyncTestUser1Table table = new SyncTestUser1Table();
		SyncTestUserObject userObject = fixture.getUserObjects(
				Source.SERVER, table.getName(), 0, user).get(0);
		String id = UUID.randomUUID().toString().toLowerCase().replaceAll(
				"-", "");
		userObject.setId(id);
		DatabaseObjectMapper dbMapper = new DatabaseObjectMapper();
		Map<String,Object> map = dbMapper.objectToMap(userObject, false);
		ObjectMapper jsonMapper = new ObjectMapper();
		String json = jsonMapper.writeValueAsString(map);
		// create insert action
		long now = System.currentTimeMillis();
		DatabaseAction insertAction = new DatabaseAction();
		insertAction.setTable(table.getName());
		insertAction.setUser(user);
		insertAction.setAction(Action.INSERT);
		insertAction.setRecordId(id);
		insertAction.setJsonData(json);
		insertAction.setTime(now);
		insertAction.setOrder(0);
		actions.add(insertAction);
		dbSync.writeSyncActions(db, actions, "remote");
		// validate written object
		SyncTestUserObject dbObject = db.selectOne(table, null, null);
		Assert.assertEquals(userObject, dbObject);
		// clear progress and insert again: this simulates the situation when
		// an insertion succeeded but the progress was not updated (because of
		// a database failure or the process ended)
		db.delete(new SyncProgressTableDef(), null);
		dbSync.writeSyncActions(db, actions, "remote");
		// create updated insert action (simulates a merge of an insert and an
		// update)
		now = System.currentTimeMillis();
		userObject.setIntField(userObject.getIntField() + 1);
		map = dbMapper.objectToMap(userObject, false);
		json = jsonMapper.writeValueAsString(map);
		insertAction = new DatabaseAction();
		insertAction.setTable(table.getName());
		insertAction.setUser(user);
		insertAction.setAction(Action.INSERT);
		insertAction.setRecordId(id);
		insertAction.setJsonData(json);
		insertAction.setTime(now);
		insertAction.setOrder(0);
		actions.add(insertAction);
		dbSync.writeSyncActions(db, actions, "remote");
		// validate written object
		dbObject = db.selectOne(table, null, null);
		Assert.assertEquals(userObject, dbObject);
		// create update action
		Map<String,Object> updateData = new LinkedHashMap<>();
		userObject.setIntField(userObject.getIntField() + 1);
		updateData.put("intField", userObject.getIntField());
		json = jsonMapper.writeValueAsString(updateData);
		now = System.currentTimeMillis();
		DatabaseAction updateAction = new DatabaseAction();
		updateAction.setTable(table.getName());
		updateAction.setUser(user);
		updateAction.setAction(Action.UPDATE);
		updateAction.setRecordId(id);
		updateAction.setJsonData(json);
		updateAction.setTime(now);
		updateAction.setOrder(0);
		actions.clear();
		actions.add(updateAction);
		dbSync.writeSyncActions(db, actions, "remote");
		// validate written object
		dbObject = db.selectOne(table, null, null);
		Assert.assertEquals(userObject, dbObject);
		// clear progress and update again
		db.delete(new SyncProgressTableDef(), null);
		dbSync.writeSyncActions(db, actions, "remote");
		// create delete action
		now = System.currentTimeMillis();
		DatabaseAction deleteAction = new DatabaseAction();
		deleteAction.setTable(table.getName());
		deleteAction.setUser(user);
		deleteAction.setAction(Action.DELETE);
		deleteAction.setRecordId(id);
		deleteAction.setTime(now);
		deleteAction.setOrder(0);
		actions.clear();
		actions.add(deleteAction);
		dbSync.writeSyncActions(db, actions, "remote");
		// validate deletion
		dbObject = db.selectOne(table, null, null);
		Assert.assertNull(dbObject);
		// clear progress and update again
		db.delete(new SyncProgressTableDef(), null);
		dbSync.writeSyncActions(db, actions, "remote");
	}

	private void testMergedActions() throws Exception {
		Logger logger = AppComponents.getLogger(getClass().getSimpleName());
		serverDbConn.dropDatabase(serverDbName);
		serverDbConn.dropDatabase(client1DbName);
		Database serverDb = initDatabase(serverDbConn, serverDbName);
		Database client1Db = initDatabase(client1DbConn, client1DbName);

		// write first stage
		for (DatabaseTableDef<SyncTestUserObject> table : userTables) {
			List<SyncTestUserObject> objects = fixture.getUserObjects(
					Source.SERVER, table.getName(), 0);
			serverDb.insert(table.getName(), objects);
			logger.info("Wrote {} user objects to server db, table {}",
					objects.size(), table.getName());
		}

		// update records
		Map<String,List<SyncTestUserObject>> tableUpdateMap = new HashMap<>();
		for (DatabaseTableDef<SyncTestUserObject> table : userTables) {
			List<SyncTestUserObject> objects = fixture.getUserObjects(
					Source.SERVER, table.getName(), 0);
			tableUpdateMap.put(table.getName(), new ArrayList<>());
			for (SyncTestUserObject object : objects) {
				SyncTestUserObject update = new SyncTestUserObject();
				update.copyFrom(object);
				update.setIntField(update.getIntField() * 10);
				serverDb.update(table.getName(), update);
				if (update.getUser().equals(SyncTestFixture.USER1))
					tableUpdateMap.get(table.getName()).add(update);
			}
		}

		// synchronize from server to client1
		logger.info("Synchronize from server to client1");
		DatabaseSynchronizer syncRead = new DatabaseSynchronizer(
				SyncTestFixture.USER1);
		DatabaseSynchronizer syncWrite = new DatabaseSynchronizer(
				SyncTestFixture.USER1);
		List<SyncProgress> progress = syncWrite.getSyncProgress(client1Db);
		Assert.assertEquals(0, progress.size());
		List<DatabaseAction> actions = syncRead.readSyncActions(serverDb,
				progress, 0, null, List.of("server"));
		Assert.assertEquals(6, actions.size());
		syncWrite.writeSyncActions(client1Db, actions, "server");
		progress = syncWrite.getSyncProgress(client1Db);
		assertEqualProgress(actions, progress);
		for (DatabaseTableDef<SyncTestUserObject> table : userTables) {
			Set<SyncTestUserObject> expected = new HashSet<>();
			expected.addAll(tableUpdateMap.get(table.getName()));
			Set<SyncTestUserObject> actual = new HashSet<>(
					client1Db.select(table, null, 0, null));
			Assert.assertEquals(expected, actual);
		}

		// delete records
		for (DatabaseTableDef<SyncTestUserObject> table : userTables) {
			List<SyncTestUserObject> objects = fixture.getUserObjects(
					Source.SERVER, table.getName(), 0);
			serverDb.delete(table.getName(), objects.get(0));
		}
		progress = syncWrite.getSyncProgress(client1Db);
		Assert.assertEquals(2, progress.size());
		actions = syncRead.readSyncActions(serverDb, progress, 0, null,
				List.of("server"));
		// only 2 delete actions
		Assert.assertEquals(2, actions.size());
		// read all actions without progress
		actions = syncRead.readSyncActions(serverDb, null, 0, null,
				List.of("server"));
		// merged actions on 6 records
		Assert.assertEquals(6, actions.size());
		syncWrite.writeSyncActions(client1Db, actions, "server");
		progress = syncWrite.getSyncProgress(client1Db);
		assertEqualProgress(actions, progress);
		for (DatabaseTableDef<SyncTestUserObject> table : userTables) {
			List<SyncTestUserObject> actual = client1Db.select(table, null, 0,
					null);
			// 2 of 3 records remaining in each table
			Assert.assertEquals(2, actual.size());
		}
	}

	private void assertEqualUserSets(Database database,
			SourceStage[] sourceStages, String... users)
			throws DatabaseException {
		for (DatabaseTableDef<SyncTestUserObject> table : userTables) {
			Set<SyncTestUserObject> expected = new HashSet<>();
			for (SourceStage sourceStage : sourceStages) {
				expected.addAll(fixture.getUserObjects(sourceStage.source,
						table.getName(), sourceStage.stage, users));
			}
			Set<SyncTestUserObject> actual = new HashSet<>(
					database.select(table, null, 0, null));
			Assert.assertEquals(expected, actual);
		}
	}

	private void assertEqualProgress(List<DatabaseAction> actions,
			List<SyncProgress> progressList) {
		List<String> checkedTables = new ArrayList<>();
		for (int i = actions.size() - 1; i >= 0; i--) {
			DatabaseAction action = actions.get(i);
			if (checkedTables.contains(action.getTable()))
				continue;
			SyncProgress progress = findTableProgress(action.getTable(),
					progressList);
			Assert.assertNotNull(progress);
			Assert.assertEquals(action.getTime(), progress.getTime());
			Assert.assertEquals(action.getOrder(), progress.getOrder());
			checkedTables.add(action.getTable());
		}
	}

	private SyncProgress findTableProgress(String table,
			List<SyncProgress> progress) {
		if (progress == null)
			return null;
		for (SyncProgress sp : progress) {
			if (sp.getTable().equals(table))
				return sp;
		}
		return null;
	}

	private static class SourceStage {
		public SyncTestFixture.Source source;
		public int stage;

		public SourceStage(SyncTestFixture.Source source, int stage) {
			this.source = source;
			this.stage = stage;
		}
	}
}

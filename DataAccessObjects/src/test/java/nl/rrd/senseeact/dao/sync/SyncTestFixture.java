package nl.rrd.senseeact.dao.sync;

import nl.rrd.senseeact.dao.ResourceTestObject;

import java.util.*;

public class SyncTestFixture {
	public static final String USER1 = "testuser1";
	public static final String USER2 = "testuser2";

	private Random random = new Random();

	private Map<ObjectSetKey,List<SyncTestUserObject>> userSets =
			new LinkedHashMap<>();
	private Map<Integer,List<ResourceTestObject>> resourceSets =
			new LinkedHashMap<>();

	public SyncTestFixture() {
		for (int stage = 0; stage < 2; stage++) {
			String[] tables = new String[] { SyncTestUser1Table.NAME,
					SyncTestUser2Table.NAME };
			for (String table : tables) {
				List<SyncTestUserObject> objs = createRandomUserObjects(3,
						USER1, Source.SERVER);
				objs.addAll(createRandomUserObjects(3, USER2, Source.SERVER));
				userSets.put(new ObjectSetKey(Source.SERVER, table, stage),
						objs);
			}
			for (String table : tables) {
				userSets.put(new ObjectSetKey(Source.CLIENT1, table, stage),
						createRandomUserObjects(3, USER1, Source.CLIENT1));
			}
			for (String table : tables) {
				userSets.put(new ObjectSetKey(Source.CLIENT2, table, stage),
						createRandomUserObjects(3, USER2, Source.CLIENT2));
			}
			resourceSets.put(stage, createRandomResourceObjects(3));
		}
	}

	public List<SyncTestUserObject> getUserObjects(Source source,
			String table, int stage, String... users) {
		List<SyncTestUserObject> objs = userSets.get(
				new ObjectSetKey(source, table, stage));
		if (users == null || users.length == 0)
			return objs;
		List<SyncTestUserObject> filtered = new ArrayList<>();
		List<String> userList = Arrays.asList(users);
		for (SyncTestUserObject obj : objs) {
			if (userList.contains(obj.getUser()))
				filtered.add(obj);
		}
		return filtered;
	}

	public List<ResourceTestObject> getResourceObjects(int stage) {
		return resourceSets.get(stage);
	}

	private List<SyncTestUserObject> createRandomUserObjects(int n, String user,
			Source source) {
		List<SyncTestUserObject> result = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			result.add(createRandomUserObject(user, source));
		}
		return result;
	}

	private List<ResourceTestObject> createRandomResourceObjects(int n) {
		List<ResourceTestObject> result = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			result.add(new ResourceTestObject(createRandomInt()));
		}
		return result;
	}

	private SyncTestUserObject createRandomUserObject(String user,
			Source source) {
		SyncTestUserObject testObj = new SyncTestUserObject();
		testObj.setUser(user);
		testObj.setSource(source.toString().toLowerCase());
		testObj.setIntField(createRandomInt());
		return testObj;
	}

	private int createRandomInt() {
		return 1000 + random.nextInt(9000);
	}

	private static class ObjectSetKey {
		private Source source;
		private String table;
		private int stage;

		private ObjectSetKey(Source source, String table, int stage) {
			this.source = source;
			this.table = table;
			this.stage = stage;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			ObjectSetKey that = (ObjectSetKey) o;
			if (source != that.source)
				return false;
			if (!table.equals(that.table))
				return false;
			if (stage != that.stage)
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			int result = source.hashCode();
			result = 31 * result + table.hashCode();
			result = 31 * result + stage;
			return result;
		}
	}

	public enum Source {
		SERVER,
		CLIENT1,
		CLIENT2
	}
}

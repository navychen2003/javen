package org.javenstudio.common.entitydb.example;

import org.javenstudio.common.entitydb.wrapper.SimpleMemoryDB;

public class TestEntityQuery extends SimpleMemoryDB.TQuery<TestEntity> {

	public TestEntityQuery() {
		super(TestDatabase.getDatabase(), TestEntity.class); 
	}
	
	public void setName(String name) {
		if (name != null && name.length() > 0) 
			whereAnd(newEqualsClause(TestEntity.Table.FIELD_NAME, name)); 
	}
	
}

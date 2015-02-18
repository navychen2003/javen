package org.javenstudio.common.entitydb.example;

import org.javenstudio.common.entitydb.IEntityMap;
import org.javenstudio.common.entitydb.IIdentityGenerator;
import org.javenstudio.common.entitydb.ITable;
import org.javenstudio.common.entitydb.wrapper.SimpleMemoryDB;
import org.javenstudio.common.entitydb.type.LongIdentity;

public class TestEntity extends SimpleMemoryDB.TEntity {

	public static class Table extends SimpleMemoryDB.TTable<TestEntity> {
		public static final String TABLE_NAME = "test"; 
		public static final String FIELD_NAME = "name"; 
		public static final String FIELD_DESCRIPTION = "description"; 
		
		public Table() { 
			super(TABLE_NAME, TestEntity.class);
		}
		
		@Override
		public IIdentityGenerator<LongIdentity> createIdentityGenerator() { 
			return null; //new LongIdentity.Generator();
		}
		
		@Override
		public IEntityMap<LongIdentity, TestEntity> createEntityMap(ITable<LongIdentity, TestEntity> table) { 
			//return new TestDatabase.EntityHashMap<TestEntity>((SimpleMemoryDB.TMemoryTable<LongIdentity, TestEntity>)table); 
			return new TestDatabase.EntityTreeMap<TestEntity>((SimpleMemoryDB.TMemoryTable<LongIdentity, TestEntity>)table); 
		}
	}
	
	public String name = null; 
	public String description = null; 
	
	public TestEntity() { 
		super(); 
	}
	
	public TestEntity(long id) {
		super(id); 
	}
	
}

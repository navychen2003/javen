package org.javenstudio.common.entitydb;

public interface IDatabase {

	public String getDatabaseName(); 
	public IDatabaseManager getDatabaseManager(); 
	public IStreamStore getStreamStore(); 
	
	public <K extends IIdentity, T extends IEntity<K>> 
		ITable<K,T> createTable(String name, Class<T> entityClass, 
				String identityFieldName, IIdentityGenerator<K> idGenerator, 
				IMapCreator<K,T> mapCreator, boolean cacheEnabled); 
	
	public <K extends IIdentity, T extends IEntity<K>> ITable<K,T> getTable(String name); 
	public <K extends IIdentity, T extends IEntity<K>> ITable<K,T> getTable(Class<T> entityClass); 
	
	public <K extends IIdentity> K insert(IEntity<K> data); 
	public <K extends IIdentity> K update(IEntity<K> data); 
	public <K extends IIdentity> boolean delete(IEntity<K> data); 
	
	public void lock(); 
	public void unlock(); 
	
	public void beginTransaction(); 
	public void endTransaction(); 
	public void setTransactionSuccessful(); 
	public boolean inTransaction(); 
	
	public void close(); 
	
	public boolean isReadOnly(); 
	public boolean isOpen(); 
	
	public int getVersion(); 
	public void setVersion(int version); 
	
}

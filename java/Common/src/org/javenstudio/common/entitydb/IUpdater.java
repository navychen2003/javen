package org.javenstudio.common.entitydb;

public interface IUpdater {

	public void saveOrUpdate() throws EntityException; 
	public void insert() throws EntityException; 
	public void update() throws EntityException; 
	public void delete() throws EntityException; 
	
	public IIdentity insert(IEntity<? extends IIdentity> entity) throws EntityException;
	public IIdentity update(IEntity<? extends IIdentity> entity) throws EntityException;
	public IIdentity delete(IEntity<? extends IIdentity> entity) throws EntityException;
	
}

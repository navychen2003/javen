package org.javenstudio.common.entitydb.db;

import org.javenstudio.common.entitydb.IEntity;
import org.javenstudio.common.entitydb.IIdentity;
import org.javenstudio.common.entitydb.ITrigger;

public abstract class AbstractTrigger<K extends IIdentity, T extends IEntity<K>> implements ITrigger<K,T> {

}

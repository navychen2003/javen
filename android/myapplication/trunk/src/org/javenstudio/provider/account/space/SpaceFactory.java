package org.javenstudio.provider.account.space;

public abstract class SpaceFactory {

	public SpaceDataSets createSpaceDataSets(SpaceProvider p) { 
		return new SpaceDataSets(new SpaceCursorFactory());
	}
	
	public abstract SpaceBinder createSpaceBinder(SpaceProvider p);
	
}

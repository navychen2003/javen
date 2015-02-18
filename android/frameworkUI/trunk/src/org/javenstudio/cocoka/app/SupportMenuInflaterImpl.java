package org.javenstudio.cocoka.app;

final class SupportMenuInflaterImpl implements IMenuInflater {

	protected final SupportMenuInflater mInflater;
	
	public SupportMenuInflaterImpl(SupportMenuInflater inflater) { 
		mInflater = inflater;
	}

	@Override
	public void inflate(int menuRes, IMenu menu) {
		mInflater.inflate(menuRes, ((SupportMenuImpl)menu).mMenu);
	}
	
}

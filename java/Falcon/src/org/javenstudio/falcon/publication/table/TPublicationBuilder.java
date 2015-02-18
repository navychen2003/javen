package org.javenstudio.falcon.publication.table;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.publication.IPublication;
import org.javenstudio.falcon.publication.IPublication.Builder;

final class TPublicationBuilder implements Builder {

	private final TPublication mItem;
	
	public TPublicationBuilder(TPublication item) {
		if (item == null) throw new NullPointerException();
		mItem = item;
	}
	
	@Override
	public IPublication save() throws ErrorException {
		return mItem.getService().getCache().savePublication(mItem);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{publication=" + mItem + "}";
	}

	@Override
	public Builder setAttr(String name, int val) {
		mItem.setAttr(name, val);
		return this;
	}

	@Override
	public Builder setAttr(String name, long val) {
		mItem.setAttr(name, val);
		return this;
	}

	@Override
	public Builder setAttr(String name, float val) {
		mItem.setAttr(name, val);
		return this;
	}

	@Override
	public Builder setAttr(String name, boolean val) {
		mItem.setAttr(name, val);
		return this;
	}

	@Override
	public Builder setAttr(String name, byte[] val) {
		mItem.setAttr(name, val);
		return this;
	}

	@Override
	public Builder setAttr(String name, String val) {
		mItem.setAttr(name, val);
		return this;
	}

	@Override
	public Builder setHeader(String name, int val) {
		mItem.setHeader(name, val);
		return this;
	}

	@Override
	public Builder setHeader(String name, long val) {
		mItem.setHeader(name, val);
		return this;
	}

	@Override
	public Builder setHeader(String name, float val) {
		mItem.setHeader(name, val);
		return this;
	}

	@Override
	public Builder setHeader(String name, boolean val) {
		mItem.setHeader(name, val);
		return this;
	}

	@Override
	public Builder setHeader(String name, byte[] val) {
		mItem.setHeader(name, val);
		return this;
	}

	@Override
	public Builder setHeader(String name, String val) {
		mItem.setHeader(name, val);
		return this;
	}

	@Override
	public Builder setContent(String name, int val) {
		mItem.setContent(name, val);
		return this;
	}

	@Override
	public Builder setContent(String name, long val) {
		mItem.setContent(name, val);
		return this;
	}

	@Override
	public Builder setContent(String name, float val) {
		mItem.setContent(name, val);
		return this;
	}

	@Override
	public Builder setContent(String name, boolean val) {
		mItem.setContent(name, val);
		return this;
	}

	@Override
	public Builder setContent(String name, byte[] val) {
		mItem.setContent(name, val);
		return this;
	}

	@Override
	public Builder setContent(String name, String val) {
		mItem.setContent(name, val);
		return this;
	}

}

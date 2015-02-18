package org.javenstudio.falcon.datum.table;

import java.io.IOException;

import org.javenstudio.falcon.datum.IDatabase;
import org.javenstudio.falcon.datum.table.store.CompareFilter;
import org.javenstudio.falcon.datum.table.store.FilterList;
import org.javenstudio.falcon.datum.table.store.Get;
import org.javenstudio.falcon.datum.table.store.PageFilter;
import org.javenstudio.falcon.datum.table.store.Scan;
import org.javenstudio.falcon.datum.table.store.SingleColumnValueFilter;

public class TableQuery implements IDatabase.Query {

	private final TableRegion mTable;
	private FilterList mFilters = null;
	private PageFilter mPageFilter = null;
	private Scan mScan = null;
	private Get mGet = null;
	private Long mMinStamp = null;
	private Long mMaxStamp = null;
	private Integer mMaxVers = null;
	
	TableQuery(TableRegion table) { 
		if (table == null) throw new NullPointerException();
		mTable = table;
	}
	
	public TableRegion getTable() { return mTable; }
	
	public synchronized Scan getScan() throws IOException { 
		if (mScan == null) mScan = new Scan();
		if (mScan != null) {
			mScan.setFilter(getFilters());
			
			if (mMinStamp != null && mMaxStamp != null)
				mScan.setTimeRange(mMinStamp, mMaxStamp);
			
			if (mMaxVers != null)
				mScan.setMaxVersions(mMaxVers);
		}
		return mScan;
	}
	
	public synchronized Get getGet() throws IOException { 
		if (mGet != null) {
			mGet.setFilter(getFilters());
			
			if (mMinStamp != null && mMaxStamp != null)
				mGet.setTimeRange(mMinStamp, mMaxStamp);
			
			if (mMaxVers != null)
				mGet.setMaxVersions(mMaxVers);
		}
		return mGet;
	}
	
	public synchronized FilterList getFilters() { 
		if (mFilters == null) mFilters = new FilterList();
		return mFilters;
	}
	
	@Override
	public synchronized void setTimeRange(long minStamp, long maxStamp) {
		mMinStamp = new Long(minStamp);
		mMaxStamp = new Long(maxStamp);
	}
	
	@Override
	public synchronized void setTimeStamp(long timestamp) {
		mMinStamp = new Long(timestamp);
		mMaxStamp = new Long(timestamp+1);
	}
	
	@Override
	public synchronized void setMaxVersions(int maxVersions) {
		mMaxVers = new Integer(maxVersions);
	}
	
	@Override
	public synchronized void setRowSize(long size) { 
		if (size <= 0) size = Long.MAX_VALUE;
		if (mPageFilter == null) {
			mPageFilter = new PageFilter(size);
			getFilters().addFilter(mPageFilter);
		} else 
			mPageFilter.setPageSize(size);
	}
	
	@Override
	public synchronized void setRow(byte[] key) { 
		if (key == null) throw new NullPointerException();
		if (mGet != null) throw new IllegalArgumentException("Row key already set");
		mGet = new Get(key);
	}
	
	@Override
	public void setColumn(IDatabase.MatchOp op, IDatabase.Value... values) { 
		if (op == null || values == null) return;
		
		final CompareFilter.CompareOp cp;
		switch (op) { 
		case LESS:
			cp = CompareFilter.CompareOp.LESS;
			break;
		case LESS_OR_EQUAL:
			cp = CompareFilter.CompareOp.LESS_OR_EQUAL;
			break;
		case EQUAL:
			cp = CompareFilter.CompareOp.EQUAL;
			break;
		case NOT_EQUAL:
			cp = CompareFilter.CompareOp.NOT_EQUAL;
			break;
		case GREATER_OR_EQUAL:
			cp = CompareFilter.CompareOp.GREATER_OR_EQUAL;
			break;
		case GREATER:
			cp = CompareFilter.CompareOp.GREATER;
			break;
		default:
			cp = CompareFilter.CompareOp.NO_OP;
			break;
		}
		
		for (IDatabase.Value val : values) { 
			if (val == null) continue;
			getFilters().addFilter(new SingleColumnValueFilter(
					val.getFamily(), val.getQualifier(), cp, val.getValue()));
		}
	}
	
}

package org.javenstudio.jfm.filesystems.bdb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.swing.Icon;

import org.javenstudio.falcon.datum.bdb.BdbTableRow;
import org.javenstudio.jfm.filesystems.JFMFile;

public class JFMDBFile extends JFMFile {

	private final JFMDBTable mTable;
	private final BdbTableRow mRow;
	
	public JFMDBFile(JFMDBTable table, BdbTableRow data) {
		super(data);
		mTable = table;
		mRow = data;
	}

	public JFMDBTable getTable() { return mTable; }
	public BdbTableRow getRow() { return mRow; }
	
	@Override
	public JFMFile[] listFiles() throws IOException {
		return null;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return null;
	}

	@Override
	public String getName() {
		return mRow.getName();
	}

	@Override
	public String getFsName() {
		return mTable.getFsName();
	}

	@Override
	public String getFsSchemeName() {
		return mTable.getFsSchemeName();
	}

	@Override
	public String getParent() {
		return mTable.getName();
	}

	@Override
	public JFMFile getParentFile() {
		return mTable;
	}

	@Override
	public String getPath() {
		return getName();
	}

	@Override
	public void setPath(String path) {
	}

	@Override
	public boolean isAbsolute() {
		return true;
	}

	@Override
	public String getAbsolutePath() {
		return getName();
	}

	@Override
	public JFMFile getAbsoluteFile() {
		return this;
	}

	@Override
	public String getCanonicalPath() throws IOException {
		return getName();
	}

	@Override
	public JFMFile getCanonicalFile() throws IOException {
		return this;
	}

	@Override
	public boolean isDirectory() {
		return false;
	}

	@Override
	public boolean isFile() {
		return true;
	}

	@Override
	public boolean isHidden() {
		return false;
	}

	@Override
	public long lastModified() {
		return mRow.getTimestamp();
	}

	@Override
	public long length() {
		return mRow.getContentLength();
	}

	@Override
	public int compareTo(JFMFile pathname) {
		return getName().compareTo(pathname.getName());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || !(obj instanceof JFMDBFile)) return false;
		JFMDBFile other = (JFMDBFile)obj;
		return getName().equals(other.getName());
	}

	@Override
	public int hashCode() {
		return getRow().hashCode();
	}

	@Override
	public String getSystemDisplayName() {
		return getName();
	}

	@Override
	public boolean canRead() {
		return true;
	}

	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public JFMFile mkdir(String name, String path) throws IOException {
		return null;
	}

	@Override
	public JFMFile mkdir(JFMFile dir) throws IOException {
		return null;
	}

	@Override
	public boolean rename(String name) throws IOException {
		return false;
	}

	@Override
	public boolean renameTo(JFMFile newfile) throws IOException {
		return false;
	}

	@Override
	public boolean createNewFile(String name, String path) throws IOException {
		return false;
	}

	@Override
	public JFMFile createFile(String name, String path) throws IOException {
		return null;
	}

	@Override
	public boolean delete() throws IOException {
		return false;
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	@Override
	public URI toURI() {
		return null;
	}

}

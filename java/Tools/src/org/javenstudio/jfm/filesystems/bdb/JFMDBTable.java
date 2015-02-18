package org.javenstudio.jfm.filesystems.bdb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;

import javax.swing.Icon;

import org.javenstudio.falcon.datum.bdb.BdbTable;
import org.javenstudio.falcon.datum.bdb.BdbTableRow;
import org.javenstudio.jfm.filesystems.JFMFile;

public class JFMDBTable extends JFMFile {

	private final JFMDBFilesystem mFs;
	private final BdbTable mTable;
	private final String mName;
	
	public JFMDBTable(JFMDBFilesystem fs, BdbTable table) {
		super(table);
		mFs = fs;
		mTable = table;
		mName = table != null ? 
				"[" + table.getTableName() + "]" : 
				"<Empty>";
	}

	public JFMDBFilesystem getFs() { return mFs; }
	public BdbTable getTable() { return mTable; }
	
	@Override
	public JFMDBFile[] listFiles() throws IOException {
		BdbTable table = getTable();
		if (table != null) { 
			ArrayList<JFMDBFile> files = new ArrayList<JFMDBFile>();
			BdbTableRow[] rows = table.listRows();
			for (int i=0; rows != null && i < rows.length; i++) { 
				BdbTableRow row = rows[i];
				if (row != null)
					files.add(new JFMDBFile(this, row));
			}
			return files.toArray(new JFMDBFile[files.size()]);
		}
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
		return mName;
	}

	@Override
	public String getFsName() {
		return getFs().getName();
	}

	@Override
	public String getFsSchemeName() {
		return getFs().getSchemeAuthority(); 
	}

	@Override
	public String getParent() {
		return null;
	}

	@Override
	public JFMFile getParentFile() {
		return null;
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
		return false;
	}

	@Override
	public String getAbsolutePath() {
		return getName();
	}

	@Override
	public JFMFile getAbsoluteFile() {
		return null;
	}

	@Override
	public String getCanonicalPath() throws IOException {
		return getName();
	}

	@Override
	public JFMFile getCanonicalFile() throws IOException {
		return null;
	}

	@Override
	public boolean isDirectory() {
		return true;
	}

	@Override
	public boolean isFile() {
		return false;
	}

	@Override
	public boolean isHidden() {
		return false;
	}

	@Override
	public long lastModified() {
		return 0;
	}

	@Override
	public long length() {
		return 0;
	}

	@Override
	public int compareTo(JFMFile pathname) {
		return getName().compareTo(pathname.getName());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || !(obj instanceof JFMDBTable)) return false;
		JFMDBTable other = (JFMDBTable)obj;
		return getName().equals(other.getName());
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
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
		return this;
	}

	@Override
	public JFMFile mkdir(JFMFile dir) throws IOException {
		return this;
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
		if (path != null && path.length() > 0)
			return new JFMDBFileNew(this, path);
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

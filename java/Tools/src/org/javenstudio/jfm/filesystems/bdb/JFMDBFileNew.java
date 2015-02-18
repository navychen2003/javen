package org.javenstudio.jfm.filesystems.bdb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.swing.Icon;

import org.javenstudio.jfm.filesystems.JFMFile;

public class JFMDBFileNew extends JFMFile {

	private final JFMDBTable mTable;
	private final String mName;
	private BytesOutputStream mOutput = null;
	
	public JFMDBFileNew(JFMDBTable table, String name) {
		super(null);
		mTable = table;
		mName = name;
	}

	public JFMDBTable getTable() { return mTable; }
	
	@Override
	public JFMFile[] listFiles() throws IOException {
		return null;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public synchronized OutputStream getOutputStream() throws IOException {
		mOutput = new BytesOutputStream();
		return mOutput;
	}

	private class BytesOutputStream extends ByteArrayOutputStream { 
		public BytesOutputStream() {}
		
		@Override
		public void close() throws IOException { 
			super.close();
			
			byte[] content = toByteArray();
			getTable().getTable().putRow(getName(), content);
		}
	}
	
	@Override
	public String getName() {
		return mName;
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
		return 0; //mRow.getTimestamp();
	}

	@Override
	public long length() {
		return 0; //mRow.getContentLength();
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
		return false;
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

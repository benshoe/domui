package to.etc.dbcompare.db;

import java.io.*;
import java.util.*;

/**
 * Physical index on a table.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Dec 22, 2006
 */
public class Index implements Serializable {
	private Table				m_table;

	private List<IndexColumn>	m_columnList	= new ArrayList<IndexColumn>();

	private boolean				m_unique;

	private String				m_name;

	private String				m_tablespace;

	public String getTablespace() {
		return m_tablespace;
	}

	public void setTablespace(String tablespace) {
		m_tablespace = tablespace;
	}

	public void setTable(Table table) {
		m_table = table;
	}

	public void setColumnList(List<IndexColumn> columnList) {
		m_columnList = columnList;
	}

	public void setUnique(boolean unique) {
		m_unique = unique;
	}

	public void setName(String name) {
		m_name = name;
	}

	public Index(Table table, String name, boolean unique) {
		m_table = table;
		m_name = name;
		m_unique = unique;
	}

	public List<IndexColumn> getColumnList() {
		return m_columnList;
	}

	public String getName() {
		return m_name;
	}

	public Table getTable() {
		return m_table;
	}

	public boolean isUnique() {
		return m_unique;
	}

	public void addColumn(Column c, boolean desc) {
		m_columnList.add(new IndexColumn(c, desc));
	}
}

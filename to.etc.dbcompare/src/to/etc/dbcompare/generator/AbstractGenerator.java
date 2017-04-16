package to.etc.dbcompare.generator;

import static to.etc.dbcompare.AbstractSchemaComparator.*;

import java.util.*;

import to.etc.dbcompare.db.*;
import to.etc.dbcompare.db.Package;

/**
 * Abstract thingy which is able to generate the appropriate SQL for schema creation.
 * 
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Aug 6, 2007
 */
abstract public class AbstractGenerator {
	abstract public String getIdent();

	private Map<ColumnType, TypeMapping>	m_mapmap	= new HashMap<ColumnType, TypeMapping>();

	protected void registerMapping(ColumnType t, TypeMapping m) {
		m_mapmap.put(t, m);
	}

	protected void registerSimpleMapping(ColumnType t, String name) {
		m_mapmap.put(t, new SimpleMapping(name));
	}

	/**
	 * Returns T if the name passed can be used without identifier quoting.
	 * @param name
	 * @return
	 */
	public boolean isUnquotableName(String name) {
		if(name == null || name.length() == 0)
			return false;
		char c = name.charAt(0);
		if(!Character.isLetter(c) || !Character.isUpperCase(c))
			return false;

		for(int i = name.length(); --i > 0;) {
			c = name.charAt(i);
			if(c != '_' && !Character.isDigit(c) && !(Character.isLetter(c) && Character.isUpperCase(c)))
				return false;
		}
		return true;
	}

	/**
	 * Basic name renderer. Renders the name literally except when it
	 * contains lowercase or bad chars.
	 * @param a
	 * @param name
	 */
	public void renderName(Appendable a, String name) throws Exception {
		if(isUnquotableName(name)) {
			a.append(name);
			return;
		}
		a.append('"');
		a.append(name);
		a.append('"');
	}

	public void renderTableName(Appendable a, String name) throws Exception {
		a.append(name);
	}

	public void renderFieldName(Appendable a, String name) throws Exception {
		a.append(name);
	}

	public String quoted(String txt) {
		StringBuilder sb = new StringBuilder(txt.length() + 10);
		sb.append("'");
		for(int i = 0, e = txt.length(); i < e; i++) {
			char c = txt.charAt(i);
			if(c == '\'')
				sb.append(c);
			sb.append(c);
		}
		sb.append("'");
		return sb.toString();
	}

	public String getStatementDelimiter() {
		return ";";
	}

	final public String renderColumnType(Column c, boolean rest) throws Exception {
		StringBuilder sb = new StringBuilder();
		renderColumnType(sb, c, rest);
		return sb.toString();
	}

	static private final TypeMapping	BASE	= new TypeMapping() {
													public void renderType(Appendable sb, Column c) throws Exception {
														ColumnType ct = c.getType();
														if(c.getPlatformTypeName() != null)
															sb.append(c.getPlatformTypeName());
														else {
															sb.append(ct.getName());
														}
														if(ct.isPrecision() && c.getPrecision() >= 0) {
															sb.append("(");
															sb.append(Integer.toString(c.getPrecision()));
															if(ct.isScale() && c.getScale() >= 0) {
																sb.append(',');
																sb.append(Integer.toString(c.getScale()));
															}
															sb.append(')');
														}
													}
												};

	public TypeMapping getTypeMapping(Column c) {
		TypeMapping m = m_mapmap.get(c.getType());
		return m == null ? BASE : m;
	}

	public void renderColumnType(Appendable sb, Column c, boolean rest) throws Exception {
		TypeMapping m = getTypeMapping(c);
		m.renderType(sb, c);
		if(!c.isNullable() && rest)
			sb.append(" not null");
	}


	public void renderAddColumn(Appendable sb, Table dt, Column sc) throws Exception {
		sb.append("alter table ");
		renderTableName(sb, dt.getName());
		sb.append("\n\tadd ");
		sb.append(sc.getName());
		sb.append(' ');
		renderColumnType(sb, sc, true);
		sb.append(getStatementDelimiter() + "\n");
	}

	public void renderColumnDrop(List<String> l, Table dt, Column dc) throws Exception {
		//-- Create "drop"
		l.add("alter table " + dt.getName() + "\n\tdrop column " + dc.getName() + getStatementDelimiter() + "\n");
	}

	public void renderColumnComment(List<String> sl, Column sc) {
		if(sc.getComment() != null && sc.getComment().length() > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append("comment on column ");
			sb.append(sc.getTable().getName());
			sb.append('.');
			sb.append(sc.getName());
			sb.append(" is ");
			sb.append(quoted(sc.getComment()));
			sb.append(";\n");
			sl.add(sb.toString());
		}
	}

	public void renderTableComment(List<String> sl, Table sc) {
		if(sc.getComments() != null && sc.getComments().length() > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append("comment on table ");
			sb.append(sc.getName());
			sb.append(" is ");
			sb.append(quoted(sc.getComments()));
			sb.append(";\n");
			sl.add(sb.toString());
		}
	}


	public void columnChanged(List<String> l, Table dt, Column newc, Column oldc, int flag) throws Exception {
		StringBuilder sb = new StringBuilder();

		//-- What changes can I support?
		if(0 != (flag & (csPRECISION | csSCALE))) {
			//-- Same type BUT size has changed. Is nullability changed too?
			String nu = null;
			if(0 != (flag & csNULLABLE)) {
				if(!newc.isNullable()) {
					System.out.println("TODO: Need not-null modification for " + dt.getName() + "." + oldc.getName());
					nu = " not null";
				} else {
					nu = " null";
				}
			}
			flag &= ~csNULLABLE;

			//-- Create an inline comment detailing the change
			sb.setLength(0);
			sb.append("-- Change from old type= ");
			renderColumnType(sb, oldc, true);
			sb.append(";");
			l.add(sb.toString());

			sb.setLength(0);
			sb.append("alter table ");
			sb.append(dt.getName());
			sb.append("\n\tmodify ");
			sb.append(oldc.getName());
			sb.append(' ');
			renderColumnType(sb, newc, 0 != (flag & csNULLABLE));
			sb.append(getStatementDelimiter() + "\n");
			l.add(sb.toString());
		} else if(0 != (flag & csNULLABLE)) {
			//-- Nullability changed. If changed to "null" we accept,
			if(newc.isNullable()) {
				sb.setLength(0);
				sb.append("alter table ");
				sb.append(dt.getName());
				sb.append("\n\tmodify ");
				sb.append(oldc.getName());
				sb.append(" null;\n");
				l.add(sb.toString());
			} else {
				//-- Column has become not-null!! This can only be done when a default is known
				System.out.println("TODO: Need not-null modification for " + dt.getName() + "." + oldc.getName());

				//-- Naive update
				sb.setLength(0);
				sb.append("alter table ");
				sb.append(dt.getName());
				sb.append("\n\tmodify ");
				sb.append(oldc.getName());
				sb.append(" not null;\n");
				l.add(sb.toString());
			}
		}
		if(0 != (flag & (csPLATFORMTYPE | csSQLTYPE))) {
			System.out.println("TODO: Need column TYPE modification for " + dt.getName() + "." + oldc.getName() + " from " + renderColumnType(oldc, true) + " to " + renderColumnType(newc, true));
		}
		if(0 != (flag & csCOMMENT)) // Comment has changed?
			renderColumnComment(l, newc);
	}

	public void addTable(List<String> l, Table st) throws Exception {
		StringBuilder sb = new StringBuilder(512);
		sb.append("create table ");
		sb.append(st.getName());
		sb.append(" (\n");

		//-- Create the PK field 1st *if* the table has a single PK.
		boolean needcomma = false;
		PrimaryKey pk = st.getPrimaryKey();
		Column ignorec = null;
		if(pk != null && pk.getColumnList().size() == 1) {
			//-- Dump PK column,
			ignorec = pk.getColumnList().get(0);
			sb.append("\t");
			renderCreateColumn(sb, ignorec);
			sb.append(" primary key\n");
			needcomma = true;
		}

		//-- Render all other columns,
		for(Column c : st.getColumnList()) {
			if(c == ignorec)
				continue;
			if(needcomma)
				sb.append(',');
			sb.append("\t");
			renderCreateColumn(sb, c);
			sb.append("\n");
			needcomma = true;
		}

		//-- If a compound PK is present add that,
		if(pk != null && pk.getColumnList().size() > 1) {
			if(needcomma)
				sb.append(',');
			sb.append("\t");
			if(pk.getName() != null) {
				sb.append("constraint ");
				sb.append(pk.getName());
				sb.append(' ');
			}
			sb.append("primary key(");
			boolean fst = true;
			for(Column c : pk.getColumnList()) {
				if(!fst)
					sb.append(',');
				else
					fst = false;
				sb.append(c.getName());
			}
			sb.append(")");
			needcomma = true;
		}

		sb.append(");\n");
		l.add(sb.toString());

		//-- Table comments,
		renderTableComment(l, st);
		for(Column sc : st.getColumnList()) {
			renderColumnComment(l, sc);
		}
	}

	private void renderCreateColumn(StringBuilder sb, Column c) throws Exception {
		sb.append(c.getName());
		sb.append(" ");
		renderColumnType(sb, c, true);
	}

	public void renderDropTable(List<String> l, Table dt) {
		StringBuilder sb = new StringBuilder();
		sb.append("drop table ");
		sb.append(dt.getName());
		sb.append(" cascade constraints;\n");
		l.add(sb.toString());

	}

	public void renderCreatePK(List<String> l, PrimaryKey pk) throws Exception {
		StringBuilder a = new StringBuilder();
		a.append("alter table ");
		renderTableName(a, pk.getTable().getName());
		a.append("\n\tadd ");
		if(pk.getName() != null) {
			a.append("constraint ");
			a.append(pk.getName());
			a.append(' ');
		}
		a.append("primary key(");
		boolean comma = false;
		for(Column c : pk.getColumnList()) {
			if(comma)
				a.append(',');
			else
				comma = true;
			renderFieldName(a, c.getName());
		}
		a.append(")" + getStatementDelimiter());
		l.add(a.toString());
	}

	public void renderDropPK(List<String> l, PrimaryKey pk) throws Exception {
		StringBuilder a = new StringBuilder();
		a.append("alter table ");
		renderTableName(a, pk.getTable().getName());
		a.append("\n\tdrop ");
		if(pk.getName() != null) {
			a.append("constraint ");
			a.append(pk.getName());
			a.append(';');
			return;
		}
		a.append("primary key" + getStatementDelimiter());
		l.add(a.toString());
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Relation delta.										*/
	/*--------------------------------------------------------------*/

	public void renderDropRelation(List<String> l, Table dt, Relation sr) throws Exception {
		StringBuilder a = new StringBuilder(512);
		a.append("alter table ");
		renderTableName(a, dt.getName());
		a.append("\n\tdrop constraint ");
		renderName(a, sr.getName());
		a.append(";");
		l.add(a.toString());
	}

	public void renderAddRelation(List<String> l, Table dt, Relation dr) throws Exception {
		StringBuilder a = new StringBuilder(512);
		a.append("alter table ");
		renderTableName(a, dt.getName());
		a.append("\n\tadd constraint ");
		renderName(a, dr.getName());
		a.append(" foreign key(");
		int i = 0;
		for(FieldPair p : dr.getPairList()) {
			if(i++ > 0)
				a.append(',');
			renderName(a, p.getChildColumn().getName());
		}
		a.append(")\n\t\treferences ");
		renderTableName(a, dr.getParent().getName());
		a.append("(");
		i = 0;
		for(FieldPair p : dr.getPairList()) {
			if(i++ > 0)
				a.append(',');
			renderName(a, p.getParentColumn().getName());
		}
		a.append(");");
		l.add(a.toString());
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Views.												*/
	/*--------------------------------------------------------------*/

	public void renderCreateView(List<String> l, DbView v) throws Exception {
		StringBuilder a = new StringBuilder();
		a.append("create or replace view ");
		a.append(v.getName());
		a.append(" as\n");
		a.append(v.getSql());
		a.append(";\n/");
		l.add(a.toString());
	}

	public void renderDropView(List<String> l, DbView v) throws Exception {
		StringBuilder a = new StringBuilder();
		a.append("drop view ");
		a.append(v.getName());
		a.append(";");
		l.add(a.toString());
	}

	public void renderCreatePackageDefinition(List<String> l, Package p) throws Exception {
		StringBuilder a = new StringBuilder();
		a.append("create or replace package ");
		a.append(p.getName());
		a.append(" is\n");
		a.append(p.getDefinition());
		a.append("\n/");
		l.add(a.toString());
	}

	public void renderCreatePackageBody(List<String> l, Package p) throws Exception {
		StringBuilder a = new StringBuilder();
		a.append("create or replace package body ");
		a.append(p.getName());
		a.append(" is\n");
		a.append(p.getBody());
		a.append("\n/");
		l.add(a.toString());
	}

	public void renderDropPackage(List<String> l, Package p) throws Exception {
		StringBuilder a = new StringBuilder();
		a.append("drop package body ");
		a.append(p.getName());
		a.append(";");
		l.add(a.toString());
		a.setLength(0);
		a.append("drop package ");
		a.append(p.getName());
		a.append(";");
		l.add(a.toString());
	}

	public void renderAddTrigger(List<String> l, Trigger t) throws Exception {
		StringBuilder a = new StringBuilder();
		a.append("create or replace trigger ");
		a.append(t.getName());
		//    	a.append(" is\n");
		a.append(t.getCode());
		a.append("\n/");
		l.add(a.toString());
	}

	public void renderDropTrigger(List<String> l, Trigger t) throws Exception {
		StringBuilder a = new StringBuilder();
		a.append("drop trigger ");
		a.append(t.getName());
		a.append(";\n/");
		l.add(a.toString());
	}

	public void renderDropIndex(List<String> l, Index ix) throws Exception {
		StringBuilder a = new StringBuilder();
		a.append("drop index ");
		a.append(ix.getName());
		a.append(";\n/");
		l.add(a.toString());
	}

	public void renderCreateIndex(List<String> l, Index ix) throws Exception {
		StringBuilder a = new StringBuilder();
		a.append("create ");
		if(ix.isUnique())
			a.append("unique ");
		a.append("index ");
		renderName(a, ix.getName());
		a.append(" on ");
		renderTableName(a, ix.getTable().getName());
		a.append('(');
		int i = 0;
		for(IndexColumn c : ix.getColumnList()) {
			if(i++ != 0)
				a.append(',');
			renderName(a, c.getColumn().getName());
			if(c.isDescending())
				a.append(" desc");
		}
		a.append(");\n/");
		l.add(a.toString());
	}
}

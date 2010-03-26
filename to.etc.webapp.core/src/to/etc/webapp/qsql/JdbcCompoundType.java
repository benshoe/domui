package to.etc.webapp.qsql;

import java.sql.*;

import to.etc.util.*;

class JdbcCompoundType implements ITypeConverter, IJdbcTypeFactory {
	private JdbcClassMeta m_compoundMeta;

	public JdbcCompoundType(JdbcClassMeta compoundcm) {
		m_compoundMeta = compoundcm;
	}

	JdbcCompoundType() {}

	public int accept(JdbcPropertyMeta pm) {
		if(pm.isCompound())
			return 10;
		return -1;
	}

	/**
	 * Create a specific converter for this meta property.
	 * @see to.etc.webapp.qsql.IJdbcTypeFactory#createType(to.etc.webapp.qsql.JdbcPropertyMeta)
	 */
	@Override
	public ITypeConverter createType(JdbcPropertyMeta pm) throws Exception {
		JdbcClassMeta cm = JdbcMetaManager.getMeta(pm.getActualClass());
		if(!cm.isCompound())
			throw new IllegalStateException("Property " + pm + " has complex type " + pm.getActualClass() + ", but it is not marked as a compound type with @QJdbcCompound");
		return new JdbcCompoundType(cm);
	}

	@Override
	public int columnCount() {
		return m_compoundMeta.getColumnCount();
	}

	@Override
	public void assignParameter(PreparedStatement ps, int index, JdbcPropertyMeta pm, Object value) throws Exception {
		throw new IllegalStateException("Not implemented yet");
	}

	@Override
	public Object convertToInstance(ResultSet rs, int index) throws Exception {
		Object inst = m_compoundMeta.getDataClass().newInstance(); // Create empty instance;

		boolean nonnull = false;
		int rix = index;
		for(JdbcPropertyMeta pm : m_compoundMeta.getPropertyList()) {
			if(pm.isTransient())
				continue;
			ITypeConverter type = pm.getTypeConverter();
			Object pvalue;
			try {
				pvalue = type.convertToInstance(rs, rix);
			} catch(JdbcConversionException x) {
				throw x;
			} catch(Exception x) {
				throw JdbcConversionException.create(x, rs, pm, rix);
			}

			if(pvalue != null) {
				nonnull = true;
			} else {
				//-- If this is primitive convert if possible.
				if(pm.getActualClass().isPrimitive()) {
					String s = pm.getNullValue();
					if(s == null)
						s = "0";
					pvalue = RuntimeConversions.convertTo(s, pm.getActualClass());
				}
			}
			if(pvalue != null) {
				pm.getPi().getSetter().invoke(inst, pvalue);
			}
			rix += pm.getColumnNames().length;
		}
		return nonnull ? inst : null;
	}
}

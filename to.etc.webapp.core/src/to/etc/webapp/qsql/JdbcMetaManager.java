package to.etc.webapp.qsql;

import java.math.*;
import java.util.*;

/**
 * Singleton to manage all JDBC class metadata.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Aug 25, 2009
 */
public class JdbcMetaManager {
	static private final Map<Class< ? >, JdbcClassMeta> m_classMap = new HashMap<Class< ? >, JdbcClassMeta>();

	//	static private Map<Class< ? >, List<ITypeConverter>> m_converterMap = new HashMap<Class< ? >, List<ITypeConverter>>();

	static private List<IJdbcTypeFactory> m_factoryList = new ArrayList<IJdbcTypeFactory>();

	static private Set<Class< ? >> SIMPLE = new HashSet<Class< ? >>();

	static public JdbcClassMeta getMeta(Class< ? > jdbcClass) throws Exception {
		JdbcClassMeta	cm;
		synchronized(m_classMap) { // Atomically add or get in 1st lock
			cm = m_classMap.get(jdbcClass);
			if(cm == null) {
				cm = new JdbcClassMeta(jdbcClass);
				m_classMap.put(jdbcClass, cm);
			}
		}
		cm.initialize(); // Initialize in 2nd lock
		return cm;
	}

	static public synchronized void register(IJdbcTypeFactory f) {
		m_factoryList = new ArrayList<IJdbcTypeFactory>(m_factoryList);
		m_factoryList.add(f);
	}

	static private synchronized List<IJdbcTypeFactory> getFactoryList() {
		return m_factoryList;
	}

	static public ITypeConverter createConverter(JdbcPropertyMeta pm) throws Exception {
		IJdbcTypeFactory best = null;
		int bestscore = 0;
		for(IJdbcTypeFactory f : getFactoryList()) {
			int score = f.accept(pm);
			if(score > bestscore) {
				best = f;
				bestscore = score;
			}
		}
		if(best == null)
			throw new IllegalStateException("I cannot determine a JDBC converter type for " + pm + " of type=" + pm.getActualClass());
		return best.createType(pm);
	}

	//	static public synchronized void register(ITypeConverter c, Class< ? >... clses) {
	//		for(Class< ? > tc : clses) {
	//			List<ITypeConverter> cl = m_converterMap.get(tc);
	//			if(cl == null)
	//				cl = new ArrayList<ITypeConverter>();
	//			else
	//				cl = new ArrayList<ITypeConverter>(cl);
	//			cl.add(c);
	//			m_converterMap.put(tc, cl);
	//		}
	//	}
	//
	//	static private synchronized List<ITypeConverter> getConverterList(Class< ? > type) {
	//		List<ITypeConverter> cl = m_converterMap.get(type);
	//		return cl;
	//	}
	//
	//	static ITypeConverter findConverter(JdbcPropertyMeta pm) {
	//		List<ITypeConverter> cl = getConverterList(pm.getActualClass());
	//		if(cl == null)
	//			return null;
	//		ITypeConverter best = null;
	//		int bestscore = 0;
	//		for(ITypeConverter tc : cl) {
	//			int score = tc.accept(pm);
	//			if(score > bestscore) {
	//				bestscore = score;
	//				best = tc;
	//			}
	//		}
	//		return best;
	//	}
	//
	//	static ITypeConverter getConverter(JdbcPropertyMeta pm) {
	//		ITypeConverter tc = findConverter(pm);
	//		if(tc == null)
	//			throw new IllegalStateException("No converter for " + pm);
	//		return tc;
	//	}

	static public boolean isSimpleType(Class< ? > clz) {
		return SIMPLE.contains(clz);
	}

	static {
		SIMPLE = new HashSet<Class< ? >>();
		SIMPLE.add(Integer.class);
		SIMPLE.add(Integer.TYPE);
		SIMPLE.add(Long.class);
		SIMPLE.add(Long.TYPE);
		SIMPLE.add(Character.class);
		SIMPLE.add(Character.TYPE);
		SIMPLE.add(Short.class);
		SIMPLE.add(Short.TYPE);
		SIMPLE.add(Byte.class);
		SIMPLE.add(Byte.TYPE);
		SIMPLE.add(Double.class);
		SIMPLE.add(Double.TYPE);
		SIMPLE.add(Float.class);
		SIMPLE.add(Float.TYPE);
		SIMPLE.add(Boolean.class);
		SIMPLE.add(Boolean.TYPE);
		SIMPLE.add(BigDecimal.class);
		SIMPLE.add(String.class);
		SIMPLE.add(BigInteger.class);
		SIMPLE.add(Date.class);

		register(new StringType());
		register(new DoubleType());
		register(new IntegerType());
		register(new LongType());
		register(new TimestampType());
		register(new JdbcCompoundType());
		register(new TimestampType());
		//		register(new StringType(), String.class);
		//		register(new IntegerType(), Integer.class, int.class);
		//		register(new LongType(), Long.class, long.class);
		//		register(new TimestampType(), Date.class);
	}
}

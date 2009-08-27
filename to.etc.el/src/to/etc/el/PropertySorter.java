package to.etc.el;

import java.util.*;

/**
 * This is a comparator which compares objects using a property
 * expression.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on May 26, 2006
 */
public class PropertySorter implements Comparator<Object> {
	private PropertyExpression m_px;

	private boolean m_desc;

	public PropertySorter(PropertyExpression px, boolean desc) {
		m_px = px;
		m_desc = desc;
	}

	public int compare(Object o1, Object o2) {
		if(m_desc)
			return compareSub(o2, o1);
		else
			return compareSub(o1, o2);
	}

	private int compareSub(Object o1, Object o2) {
		//-- Calculate both values
		try {
			Object a = m_px.getValue(o1, null);
			Object b = m_px.getValue(o2, null);
			if(a == null && b == null)
				return 0;
			if(a == null)
				return -1;
			if(b == null)
				return 1;
			if(a instanceof Comparable && b instanceof Comparable) {
				return ((Comparable) a).compareTo(b);
			}

			throw new IllegalStateException("Do not know how to compare a " + a.getClass().getCanonicalName() + " and a " + b.getClass().getCanonicalName());
		} catch(Exception x) {
			throw new RuntimeException(x);
		}
	}
}

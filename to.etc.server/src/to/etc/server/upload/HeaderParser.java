package to.etc.server.upload;

import java.util.*;


/**
 * This decodes the headers. Each header is on a separate line (where a line ends in CRLF). An empty
 * line denotes the end of the header area. Each header has the format name: value CRLF
 * <p>If a header name occurs twice the map will contain a List of the values.
 */
public class HeaderParser {
	private String	m_str;

	private int		m_ix;

	private int		m_len;

	private String	m_property;

	private String	m_value;

	public HeaderParser() {
	}


	public final String getProperty() {
		return m_property;
	}

	public final void setProperty(String property) {
		m_property = property;
	}

	public final String getValue() {
		return m_value;
	}

	public final void setValue(String value) {
		m_value = value;
	}

	public void init(String in) {
		m_str = in;
		m_ix = 0;
		m_len = in.length();
	}

	/**
	 * Parses the next header from the area. Returns false if the line end has been reached.
	 * @return
	 */
	public boolean parseNext() {
		int nch = 0;
		char c = 0;
		char lc = 0;

		m_property = null;
		m_value = null;

		//-- 1. Skip ws in the name.
		int sp = m_ix;
		int ep = sp;
		while(m_ix < m_len) {
			c = m_str.charAt(m_ix++);
			if(c == 10 && lc == 13) // eoln?
			{
				if(nch == 0) {
					m_ix -= 2; // Back to crlf
					return false; // we're done!
				}
				ep = m_ix - 2; // Endpos minus crlf
				break;
			}
			if(c == ':') {
				ep = m_ix - 1;
				break;
			}
			lc = c;
		}
		while(sp < ep && Character.isWhitespace(m_str.charAt(sp)))
			sp++;
		while(ep > sp && Character.isWhitespace(m_str.charAt(ep - 1)))
			ep--;
		if(sp >= ep)
			return false;
		m_property = m_str.substring(sp, ep);
		if(lc == 13 && c == 10)
			return true; // Have a name but no value -> keep 

		//-- We have a ':': parse the value.
		sp = m_ix;
		ep = sp;
		lc = 0;
		while(m_ix < m_len) {
			c = m_str.charAt(m_ix++);
			if(c == 10 && lc == 13) // eoln?
			{
				ep = m_ix - 2; // Endpos minus crlf
				break;
			}
			lc = c;
		}
		while(sp < ep && Character.isWhitespace(m_str.charAt(sp)))
			sp++;
		while(ep > sp && Character.isWhitespace(m_str.charAt(ep - 1)))
			ep--;
		m_value = m_str.substring(sp, ep);
		return true;
	}

	public void parse(Map m, String hdr, boolean lcnames) {
		m.clear();
		init(hdr);
		while(parseNext()) {
			String n = getProperty();
			String v = getValue();
			if(v == null)
				continue; // Skip malformed headers
			if(lcnames)
				n = n.toLowerCase();
			Object o = m.get(n);
			if(o == null)
				m.put(n, v);
			else if(o instanceof List) {
				((List) o).add(v);
			} else {
				List l = new ArrayList(3);
				l.add(o);
				l.add(v);
				m.put(n, l);
			}
		}
	}

}

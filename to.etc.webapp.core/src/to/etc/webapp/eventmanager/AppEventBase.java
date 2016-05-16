/*
 * DomUI Java User Interface library
 * Copyright (c) 2010 by Frits Jalvingh, Itris B.V.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * See the "sponsors" file for a list of supporters.
 *
 * The latest version of DomUI and related code, support and documentation
 * can be found at http://www.domui.org/
 * The contact for the project is Frits Jalvingh <jal@etc.to>.
 */
package to.etc.webapp.eventmanager;

import java.util.*;

/**
 * Base class for all ViewPoint application events, as handled
 * by the event manager.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Sep 12, 2006
 */
public class AppEventBase {

	private long m_upid;

	private String m_server;

	private Date m_timestamp;

	public String getServer() {
		return m_server;
	}

	void setServer(final String server) {
		m_server = server;
	}

	public Date getTimestamp() {
		return m_timestamp;
	}

	void setTimestamp(final Date timestamp) {
		m_timestamp = timestamp;
	}

	public long getUpid() {
		return m_upid;
	}

	void setUpid(final long upid) {
		m_upid = upid;
	}

	@Override
	public String toString() {
		return "AppEvent[" + m_upid + "@" + m_server + "/" + getClass().getName() + "]";
	}
}

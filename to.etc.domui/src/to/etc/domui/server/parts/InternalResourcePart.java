package to.etc.domui.server.parts;

import java.io.*;
import java.util.*;

import javax.annotation.*;

import to.etc.domui.server.*;
import to.etc.domui.trouble.*;
import to.etc.domui.util.*;
import to.etc.domui.util.resources.*;
import to.etc.util.*;
import to.etc.webapp.nls.*;

/**
 * This part handler handles all internal resource requests; this are requests where the URL starts
 * with a dollar sign (and the URL is not some well-known name). The reason for this code is to allow
 * resources to come from the <i>classpath</i> and not just from within a webapp's files. This
 * code serves most of the default DomUI browser resources.
 * <p>Resources are located as follows:
 * <ul>
 *	<li>The dollar is stripped from the start of the base URL</li>
 *	<li>Try to find the resulting name in the webapp data files (below WebContent). If found there return this resource as a cached stream.</li>
 *	<li>Try to find the name as a Java classpath resource below /resources/, and return it as a cached stream.</li>
 * </ul>
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Nov 11, 2009
 */
public class InternalResourcePart implements IBufferedPartFactory {
	private static class ResKey {
		private Locale m_loc;

		private String m_rurl;

		public ResKey(Locale loc, String rurl) {
			m_loc = loc;
			m_rurl = rurl;
		}

		@Override
		public String toString() {
			return "[$resource " + m_rurl + "]";
		}

		public Locale getLoc() {
			return m_loc;
		}

		public String getRURL() {
			return m_rurl;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((m_loc == null) ? 0 : m_loc.hashCode());
			result = prime * result + ((m_rurl == null) ? 0 : m_rurl.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;
			if(obj == null)
				return false;
			if(getClass() != obj.getClass())
				return false;
			ResKey other = (ResKey) obj;
			if(m_loc == null) {
				if(other.m_loc != null)
					return false;
			} else if(!m_loc.equals(other.m_loc))
				return false;
			if(m_rurl == null) {
				if(other.m_rurl != null)
					return false;
			} else if(!m_rurl.equals(other.m_rurl))
				return false;
			return true;
		}
	}

	public Object decodeKey(String rurl, IExtendedParameterInfo param) throws Exception {
		//-- Is this an URL containing an nls'ed resource?
		Locale loc = null;
		int pos = rurl.lastIndexOf(".nls.");
		if(-1 != pos) {
			loc = NlsContext.getLocale();
			rurl = rurl.substring(0, pos) + rurl.substring(pos + 5);
		}
		if(rurl.endsWith(".class") /* || rurl.endsWith(".java") */)
			throw new ThingyNotFoundException(rurl);

		//-- Create the key.
		return new ResKey(loc, rurl);
	}

	/**
	 * Generate the local resource. This first checks to see if the resource is "externalized" into the webapp's
	 * files; if so we use the copy from there. Otherwise we expect the file to reside as a class resource rooted
	 * by the /resources path in the classpath.
	 * Resources are usually returned with an Expires: header allowing the browser to cache the resources for up to
	 * a week. However, to allow for easy debugging, you can disable all expiry header generation using a developer
	 * flag in $HOME/.developer.properties: domui.expires=false. In addition, resources generated from the webapp do
	 * not get an expires header when the server runs in DEBUG mode.
	 *
	 * @see to.etc.domui.server.parts.IBufferedPartFactory#generate(to.etc.domui.server.parts.PartResponse, to.etc.domui.server.DomApplication, java.lang.Object, to.etc.domui.util.resources.ResourceDependencyList)
	 */
	public void generate(@Nonnull PartResponse pr, @Nonnull DomApplication da, @Nonnull Object inkey, @Nonnull ResourceDependencyList rdl) throws Exception {
		ResKey k = (ResKey) inkey;

		//-- 1. Locate the resource
		IResourceRef ires;
		if(k.getLoc() != null)
			throw new IllegalStateException("Locale in resource not implemented.");
		String rurl = k.getRURL();
		ires = da.getApplicationResourceByName(rurl);
		if(da.inDevelopmentMode()) {
			rdl.add(ires);
		} else {
			// Resources are cached ONLY when in production mode.
			pr.setCacheTime(da.getDefaultExpiryTime());
		}

		pr.setMime(ServerTools.getExtMimeType(FileTool.getFileExtension(rurl)));
		InputStream is = ires.getInputStream();
		if(is == null)
			throw new ThingyNotFoundException(k.getRURL());
		try {
			FileTool.copyFile(pr.getOutputStream(), is);
		} finally {
			try {
				is.close();
			} catch(Exception x) {}
		}
		return;
	}
}

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
package to.etc.domui.dom.html;

import java.util.*;

import javax.annotation.*;

import to.etc.domui.component.layout.*;
import to.etc.domui.component.layout.title.*;
import to.etc.domui.component.misc.*;
import to.etc.domui.dom.errors.*;
import to.etc.domui.logic.*;
import to.etc.domui.util.*;
import to.etc.webapp.query.*;


/**
 * The base for all pages that can be accessed thru URL's. This is mostly a
 * dummy class which ensures that all pages/fragments properly extend from DIV,
 * ensuring that the Page logic can replace the "div" tag with a "body" tag for
 * root fragments.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Sep 1, 2008
 */
public class UrlPage extends Div {
	/** The title for the page in the head's TITLE tag. */
	private String m_pageTitle;

	public UrlPage() {
		setCssClass("ui-content");
	}

	/**
	 * Gets called when a page is reloaded (for ROOT pages only).
	 */
	public void onReload() throws Exception {}

	/**
	 * Called when the page gets destroyed (navigation or such).
	 * @throws Exception
	 */
	public void onDestroy() throws Exception {}

	/**
	 * Get the page name used for {@link AppPageTitleBar} and {@link BreadCrumb} related code. To set the head title use the
	 * "title" property.
	 * @return
	 */
	public String getPageTitle() {
		return m_pageTitle;
	}

	/**
	 * Set the page name used for {@link AppPageTitleBar} and {@link BreadCrumb} related code. To set the head title use the
	 * "title" property.
	 *
	 * @param pageTitle
	 */
	public void setPageTitle(String pageTitle) {
		m_pageTitle = pageTitle;
	}

	/**
	 * Adds javascript to close page window.
	 */
	public void closeWindow() {
		appendJavascript("window.close();");
	}

	/**
	 * In case that stretch layout is used, this needs to be called for UrlPage that does not have already set height on its body (usually case for normal UrlPage that is shown inside new browser popup window - it is not needed for regular subclasses of {@link Window}).
	 */
	protected void fixStretchBody() {
		//Since html and body are not by default 100% anymore we need to make it like this here in order to enable stretch to work.
		//We really need this layout support in domui!).
		appendCreateJS("$(document).ready(function() {document.body.parentNode.style.height = '100%'; document.body.style.height = '100%'; " + getCustomUpdatesCallJS() + "});");
	}

	/**
	 * This is the root implementation to get the "shared context" for database access. Override this to get
	 * a different "default".
	 * @see to.etc.domui.dom.html.NodeBase#getSharedContext()
	 */
	@Override
	@Nonnull
	public QDataContext getSharedContext() throws Exception {
		return getSharedContext(QContextManager.DEFAULT);
	}

	@Nonnull
	public QDataContext getSharedContext(@Nonnull String key) throws Exception {
		return QContextManager.getContext(key, getPage().getContextContainer(key));
	}

	@Override
	@Nonnull
	public QDataContextFactory getSharedContextFactory() {
		return getSharedContextFactory(QContextManager.DEFAULT);
	}

	@Nonnull
	public QDataContextFactory getSharedContextFactory(@Nonnull String key) {
		return QContextManager.getDataContextFactory(key, getPage().getContextContainer(key));
	}

	/**
	 * EXPERIMENTAL Returns the business logic context for the current form.
	 * @see to.etc.domui.dom.html.NodeBase#lc()
	 */
	@Override
	@Nonnull
	public LogiContext lc() throws Exception {
		LogiContext lc = (LogiContext) getPage().getConversation().getAttribute(LogiContext.class.getName());
		if(null == lc) {
			lc = new LogiContext(getSharedContext());
			getPage().getConversation().setAttribute(LogiContext.class.getName(), lc);
			registerLogicListeners(lc);

		}
		return lc;
	}

	protected void registerLogicListeners(@Nonnull final LogiContext lc) {
		lc.addActionMessageListener(new IMessageListener() {
			@Override
			public void actionMessages(@Nonnull List<UIMessage> msgl) {
				StringBuilder sb = new StringBuilder();

				MsgType maxt = MsgType.INFO;
				for(UIMessage m: msgl) {
					MsgType type = m.getType();
					if(type.getOrder() > maxt.getOrder())
						maxt = type;
					if(sb.length() > 0)
						sb.append("<br/>");
					sb.append(m.getMessage());
				}
				MsgBox.Type t;
				switch(maxt) {
					default:
						throw new IllegalStateException(maxt+": cannot map type");
					case ERROR: t = MsgBox.Type.ERROR; break;
					case INFO: t = MsgBox.Type.INFO; break;
					case WARNING: t = MsgBox.Type.WARNING; break;
				}

				MsgBox.message(UrlPage.this, t, sb.toString());
			}
		});

		//-- Add a request finished listener.
		getPage().addAfterRequestListener(new IExecute() {
			@Override
			public void execute() throws Exception {
				lc.endPhase();
			}
		});
	}


}

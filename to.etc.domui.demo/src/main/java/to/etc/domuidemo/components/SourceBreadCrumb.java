package to.etc.domuidemo.components;

import to.etc.domui.component.misc.ALink;
import to.etc.domui.component.misc.WindowParameters;
import to.etc.domui.dom.css.DisplayType;
import to.etc.domui.dom.html.ATag;
import to.etc.domui.dom.html.Div;
import to.etc.domui.dom.html.Img;
import to.etc.domui.dom.html.NodeContainer;
import to.etc.domui.dom.html.Span;
import to.etc.domui.dom.html.TextNode;
import to.etc.domui.dom.html.UrlPage;
import to.etc.domui.server.DomApplication;
import to.etc.domui.state.IPageParameters;
import to.etc.domui.state.IShelvedEntry;
import to.etc.domui.state.PageParameters;
import to.etc.domui.state.ShelvedDomUIPage;
import to.etc.domui.state.UIContext;
import to.etc.domui.state.WindowSession;
import to.etc.domuidemo.GitOptions;
import to.etc.domuidemo.sourceviewer.SourcePage;

import javax.annotation.Nonnull;
import java.util.List;

final public class SourceBreadCrumb extends Div {
	@Override
	public void createContent() throws Exception {
		setCssClass("d-sbc");
		WindowSession cm = UIContext.getRequestContext().getWindowSession();

		//-- Get the application's main page as the base;
		int ct = 0;
		List<IShelvedEntry> stack = cm.getShelvedPageStack();
		if(stack.size() == 0) {
			setDisplay(DisplayType.NONE);
			return;
		} else {
			IShelvedEntry se = stack.get(0);
			if(se instanceof ShelvedDomUIPage) {
				if(((ShelvedDomUIPage) se).getPage().getBody().getClass() != DomApplication.get().getRootPage()) {
					if(DomApplication.get().getRootPage() != null) {
						addPageLink(0, DomApplication.get().getRootPage(), null, "Demo Index", false);
						ct++;
					}
				}
			}
		}
		setDisplay(null);

		if(!hasSuper(getPage().getBody(), "WikiExplanationPage")) {
			//-- Add logo.
			Div right = new Div();
			add(right);
			right.setCssClass("d-sbc-logo");
			ATag at = new ATag();
			right.add(at);
			at.setHref("http://www.domui.org/");
			at.setTarget("_blank");

			Img img = new Img("img/logo-small.png");
			at.add(img);
			img.setImgBorder(0);
		}

		for(int i = 0; i < stack.size(); i++) {
			boolean last = i + 1 >= stack.size();
			ShelvedDomUIPage p = (ShelvedDomUIPage) stack.get(i);

			String ttl = p.getName();
			ttl = ttl.substring(ttl.lastIndexOf('.') + 1);
			addPageLink(ct, p.getPage().getBody().getClass(), p.getPage().getPageParameters(), ttl, last);
			ct++;
		}

		Div commits = new Div("d-sbc-commits");
		add(commits);

		if(GitOptions.hasProperties()) {
			commits.add(new Span("d-sbc-lbl", "commit"));
			commits.add(new Span("d-sbc-val", GitOptions.getCommit()));

			commits.add(new Span("d-sbc-lbl", " on "));
			commits.add(new Span("d-sbc-val", GitOptions.getCommitDate()));

			commits.add(new Span("d-sbc-lbl", " at "));
			commits.add(new Span("d-sbc-val", GitOptions.getCommitDate()));
		}
	}

	private static boolean hasSuper(@Nonnull Object instance, String what) {
		Class< ? > clz = instance.getClass();
		for(;;) {
			if(clz.getName().endsWith(what)) {
				return true;
			}
			clz = clz.getSuperclass();
			if(clz == null || clz == Object.class)
				return false;
		}
	}


	private void addPageLink(int ct, Class< ? extends UrlPage> class1, IPageParameters pageParameters, String ttl, boolean last) {
		//-- Create a LINK or a SPAN
		NodeContainer stgt;
		if(last) {
			stgt = new Span();
			stgt.setCssClass("d-sbc-c");
		} else {
			stgt = new ALink(class1, pageParameters);
			stgt.setCssClass("d-sbc-l");
		}
		if(ct > 0) {
			//-- Append the marker,
			Span sep = new Span();
			add(sep);
			sep.setCssClass("d-sbc-m");
			sep.add(new TextNode(" \u00bb "));
		}

		add(stgt);
		stgt.setText(ttl);

		ALink l = new ALink(SourcePage.class, new PageParameters("name", class1.getName().replace('.', '/') + ".java"));
		add(l);
		l.setNewWindowParameters(WindowParameters.createFixed(1024, 768, "src"));
		Img img = new Img("img/java.png");
		l.add(img);
		l.setTitle("Show the source file");

		add("\u00a0\u00a0");
	}
}

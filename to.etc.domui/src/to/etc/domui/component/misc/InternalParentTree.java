package to.etc.domui.component.misc;

import java.io.*;
import java.net.*;

import to.etc.domui.dom.html.*;
import to.etc.util.*;

/**
 * This popup floater shows all parent nodes from a given node up, and selects one.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Dec 28, 2010
 */
public class InternalParentTree extends Div {
	private NodeBase m_touched;

	public InternalParentTree(NodeBase touched) {
		m_touched = touched;
	}

	@Override
	public void createContent() throws Exception {
		setCssClass("ui-ipt");
		Div ttl = new Div();
		add(ttl);
		ttl.setCssClass("ui-ipt-ttl");
		ttl.add("Development: Parent Structure");
		Img img = new Img("THEME/close.png");
		img.setAlign(ImgAlign.RIGHT);
		ttl.add(img);
		img.setClicked(new IClicked<Img>() {
			@Override
			public void clicked(Img clickednode) throws Exception {
				//-- Remove this.
				InternalParentTree.this.remove();
			}
		});
		Div list = new Div();
		add(list);
		list.setCssClass("ui-ipt-list");

		//-- Run all parents.
		for(NodeBase nb = m_touched; nb != null; nb = nb.getParent()) {
			final NodeBase clicked = nb;
			Div item = new Div();
			list.add(item);
			item.setCssClass("ui-ipt-item");
			item.setClicked(new IClicked<Div>() {
				@Override
				public void clicked(Div clickednode) throws Exception {
					openSource(clicked);
				}
			});
			item.add(nb.getClass().getName());
		}

		appendCreateJS("$('#" + getActualID() + "').draggable({" + "ghosting: false, zIndex:" + 200 + ", handle: '#" + ttl.getActualID() + "'});");
	}


	protected void openSource(NodeBase clicked) {
		NodeBase body = getPage().getBody();
		remove();

		//-- Get name for the thingy,
		String name = clicked.getClass().getName()+".java";
		if(! openEclipseSource(name)) {
			MsgBox.message(body, MsgBox.Type.WARNING, "I was not able to send an OPEN FILE command to Eclipse.. You need to have the Eclipse plugin running. Please see " + URL + " for details");
		}
	}

	static private final String URL = "http://www.domui.org/wiki/bin/view/Documentation/EclipsePlugin";

	/**
	 * Try to reach Eclipse on localhost and make it open the source for the specified class.
	 * @param name
	 * @return
	 */
	static public boolean openEclipseSource(String name) {
		int port = DeveloperOptions.getInt("domui.eclipse", 5050); // Default Eclipse port is 5050.
		Socket s = null;
		OutputStream outputStream = null;
		//		boolean connected = false;
		try {
			s = new Socket("127.0.0.1", port);
			//			connected = true;
			outputStream = s.getOutputStream();
			String msg = name;
			outputStream.write(msg.getBytes("UTF-8"));
			outputStream.close();
			s.close();
			return true;
		} catch(Exception x) {
			System.out.println("DomUI: cannot connect to Eclipse on localhost:" + port + ". Is the DomUI plugin running in Eclipse? See "+URL);
			System.out.println("DomUI: the connect failed with " + x);
			return false;
		} finally {
			try {
				if(outputStream != null)
					outputStream.close();
			} catch(Exception x) {}
			try {
				if(s != null)
					s.close();
			} catch(Exception x) {}
		}
	}
}

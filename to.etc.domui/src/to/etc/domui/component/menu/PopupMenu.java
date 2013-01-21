package to.etc.domui.component.menu;

import java.util.*;

import javax.annotation.*;

import to.etc.domui.dom.html.*;

/**
 * Definition for a popup menu.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Feb 6, 2011
 */
public class PopupMenu {
	public static class Item {
		private String m_icon;

		private String m_title;

		private String m_hint;

		private boolean m_disabled;

		private IClicked<NodeBase> m_clicked;

		private IUIAction< ? > m_action;

		public Item(String icon, @Nonnull String title, String hint, boolean disabled, IClicked<NodeBase> clicked) {
			m_icon = icon;
			m_title = title;
			m_hint = hint;
			m_disabled = disabled;
			m_clicked = clicked;
		}

		public Item(IUIAction< ? > action) {
			m_action = action;
		}

		public String getIcon() {
			return m_icon;
		}

		public String getTitle() {
			return m_title;
		}

		public String getHint() {
			return m_hint;
		}

		public boolean isDisabled() {
			return m_disabled;
		}

		public IClicked<NodeBase> getClicked() {
			return m_clicked;
		}

		public IUIAction< ? > getAction() {
			return m_action;
		}
	}

	public final class Submenu extends Item {
		@Nonnull
		final private List<Item> m_itemList = new ArrayList<Item>();

		@Nullable
		final private Object m_target;

		public Submenu(String icon, @Nonnull String title, String hint, boolean disabled, Object target) {
			super(icon, title, hint, disabled, null);
			m_target = target;
		}

		public void addAction(@Nonnull IUIAction< ? > action) {
			m_itemList.add(new Item(action));
		}

		public void addItem(@Nonnull String caption, String icon, String hint, boolean disabled, IClicked<NodeBase> clk) {
			m_itemList.add(new Item(icon, caption, hint, disabled, clk));
		}

		public void addItem(@Nonnull String caption, String icon, IClicked<NodeBase> clk) {
			m_itemList.add(new Item(icon, caption, null, false, clk));
		}

		public void addMenu(@Nonnull String caption, String icon, String hint, boolean disabled, Object target) {
			m_itemList.add(new Submenu(icon, caption, hint, disabled, target));
		}

		@Nonnull
		public List<Item> getItemList() {
			return m_itemList;
		}

		@Nullable
		public Object getTarget() {
			return m_target;
		}
	}

	private List<Item> m_actionList = new ArrayList<Item>();

	public void addAction(@Nonnull IUIAction< ? > action) {
		m_actionList.add(new Item(action));
	}

	public void addItem(@Nonnull String caption, String icon, String hint, boolean disabled, IClicked<NodeBase> clk) {
		m_actionList.add(new Item(icon, caption, hint, disabled, clk));
	}

	public void addItem(@Nonnull String caption, String icon, IClicked<NodeBase> clk) {
		m_actionList.add(new Item(icon, caption, null, false, clk));
	}

	@Nonnull
	public Submenu addMenu(@Nonnull String caption, String icon, String hint, boolean disabled, Object target) {
		Submenu submenu = new Submenu(icon, caption, hint, disabled, target);
		m_actionList.add(submenu);
		return submenu;
	}

	/**
	 *
	 * @param ref
	 * @param target
	 */
	public <T> void show(NodeContainer ref, T target) {
		NodeContainer nc = ref.getPage().getPopIn();
		if(nc instanceof SimplePopupMenu) {
			SimplePopupMenu sp = (SimplePopupMenu) nc;
			if(sp.getSource() == this && target == sp.getTargetObject()) {
				sp.closeMenu();
				return;
			}
		}

		SimplePopupMenu sp = new SimplePopupMenu(ref, this, m_actionList, target);
		ref.getPage().setPopIn(sp);
		ref.getPage().getBody().add(0, sp);
	}
}

package to.etc.domui.component.input;

import java.util.*;

import to.etc.domui.component.meta.*;
import to.etc.domui.dom.html.*;

public class ComboFixed<T> extends SelectBasedControl<T> {
	static public final class Pair<T> {
		private T m_value;

		private String m_label;

		public Pair(T value, String label) {
			m_value = value;
			m_label = label;
		}

		public T getValue() {
			return m_value;
		}

		public String getLabel() {
			return m_label;
		}
	}

	private List<Pair<T>> m_choiceList = new ArrayList<Pair<T>>();

	public ComboFixed(List<Pair<T>> choiceList) {
		m_choiceList = choiceList;
	}

	public ComboFixed() {}

	@Override
	public void createContent() throws Exception {
		int ix = 0;
		T current = internalGetCurrentValue();
		if(!isMandatory()) {
			//-- Add 1st "empty" thingy representing the unchosen.
			SelectOption o = new SelectOption();
			if(getEmptyText() != null)
				o.setText(getEmptyText());
			add(o);
			if(current == null) {
				o.setSelected(true);
				internalSetSelectedIndex(0);
			}
			o.setSelected(current == null);
			ix++;
		}

		ClassMetaModel cmm = null;
		for(Pair<T> val : m_choiceList) {
			SelectOption o = new SelectOption();
			add(o);
			o.add(val.getLabel());
			boolean eq = false;
			if(current == null && val.getValue() == null && isMandatory()) // null is part of value domain, and in pair list?
				eq = true;
			else if(current != null && val.getValue() != null) {
				if(cmm == null) {
					cmm = MetaManager.findClassMeta(val.getValue().getClass());
				}
				eq = MetaManager.areObjectsEqual(val.getValue(), current, cmm);
			}
			o.setSelected(eq);
			if(eq)
				internalSetSelectedIndex(ix);
			ix++;
		}
	}

	@Override
	public int findListIndexForValue(T value) {
		int ix = 0;
		ClassMetaModel cmm = null;
		for(Pair<T> p : getData()) {
			if(value == null && p.getValue() == null && isMandatory()) // null is part of value domain, and in pair list?
				return ix;
			else if(value != null && p.getValue() != null) {
				if(cmm == null)
					cmm = MetaManager.findClassMeta(p.getValue().getClass());
				if(MetaManager.areObjectsEqual(p.getValue(), value, cmm))
					return ix;
			}
			ix++;
		}
		return -1;
	}

	/**
	 * Get the nth "T" value from the list-of-values.
	 * @see to.etc.domui.component.input.SelectBasedControl#findOptionValueByIndex(int)
	 */
	@Override
	protected T findOptionValueByIndex(int index) {
		if(index < 0 || index >= m_choiceList.size())
			return null; // Invalid index.
		return m_choiceList.get(index).getValue();
	}

	public void setData(List<Pair<T>> set) {
		m_choiceList = set;
		forceRebuild();
	}

	public List<Pair<T>> getData() {
		return m_choiceList;
	}
}
package to.etc.domui.component.input;

import to.etc.domui.component.layout.*;
import to.etc.domui.component.tbl.*;
import to.etc.domui.dom.css.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.util.*;

/**
 * Represents keyword search panel that is used from other components, like LookupInput.
 * Shows input field, marker that shows found results and waiting image that is hidden by default.
 * Waiting image is passive here, but it is used from browser script.
 *
 * @author <a href="mailto:vmijic@execom.eu">Vladimir Mijic</a>
 * Created on 21 Jan 2010
 */
class KeyWordSearchInput<T> extends Div {

	private int m_resultsCount = -1; //-1 states for not visible

	private TextStr m_keySearch = new TextStr();

	private Div m_pnlSearchCount;

	private IValueChanged<KeyWordSearchInput<T>> m_onLookupTyping;

	private IValueChanged<KeyWordSearchInput<T>> m_onShowTypingResults;

	public IRowRenderer<T> m_resultsHintPopupRowRenderer;

	private Img m_imgWaiting;

	private Div m_pnlSearchPopup;

	public KeyWordSearchInput() {
		super();
	}

	public KeyWordSearchInput(String m_inputCssClass) {
		super();
		m_keySearch.setCssClass(m_inputCssClass);
	}

	@Override
	public void createContent() throws Exception {
		super.createContent();
		//position must be set to relative to enable absoulute positioning of child elements (waiting image)
		setPosition(PositionType.RELATIVE);

		m_imgWaiting = new Img("THEME/wait16trans.gif");
		m_imgWaiting.setCssClass("ui-lui-waiting");
		m_imgWaiting.setDisplay(DisplayType.NONE);
		if(m_keySearch.getCssClass() == null) {
			m_keySearch.setCssClass("ui-lui-keyword");
		}
		m_keySearch.setMaxLength(40);
		m_keySearch.setSize(14);

		m_keySearch.setOnLookupTyping(new ILookupTypingListener<TextStr>() {

			@Override
			public void onLookupTyping(TextStr component, boolean done) throws Exception {
				if(done) {
					if(getOnShowResults() != null) {
						getOnShowResults().onValueChanged(KeyWordSearchInput.this);
					}
				} else {
					if(getOnLookupTyping() != null) {
						getOnLookupTyping().onValueChanged(KeyWordSearchInput.this);
					}
				}
			}
		});

		add(m_imgWaiting);
		add(m_keySearch);
		renderResultsCountPart();
	}

	public IValueChanged<KeyWordSearchInput<T>> getOnLookupTyping() {
		return m_onLookupTyping;
	}

	public void setOnLookupTyping(IValueChanged<KeyWordSearchInput<T>> onLookupTyping) {
		m_onLookupTyping = onLookupTyping;
	}

	public String getKeySearchValue() {
		return m_keySearch.getValue();
	}

	/**
	 * Set number of results label. Use -1 for hidding label.
	 * @param results
	 */
	public void setResultsCount(int results) {
		if(results != m_resultsCount) {
			m_resultsCount = results;
			if(isBuilt()) {
				renderResultsCountPart();
			}
		}
	}

	private void renderResultsCountPart() {
		if(m_resultsCount == -1 || m_resultsCount == 1) {
			if(m_pnlSearchCount != null) {
				removeChild(m_pnlSearchCount);
			}
			m_pnlSearchCount = null;
		} else {
			if(m_pnlSearchCount == null) {
				m_pnlSearchCount = new Div();
				add(m_pnlSearchCount);
			}
			if(m_resultsCount == 0) {
				m_pnlSearchCount.setCssClass("ui-lui-keyword-no-res");
				m_pnlSearchCount.setText(Msgs.BUNDLE.getString(Msgs.UI_KEYWORD_SEARCH_NO_MATCH));
			} else if(m_resultsCount == ITableModel.DEFAULT_MAX_SIZE) {
				//usually this means that query cutoff rest data, corner case when real m_resultsCount == MAX_RESULTS is not that important  
				m_pnlSearchCount.setCssClass("ui-lui-keyword-large");
				m_pnlSearchCount.setText(Msgs.BUNDLE.formatMessage(Msgs.UI_KEYWORD_SEARCH_LARGE_MATCH, "" + ITableModel.DEFAULT_MAX_SIZE));
			} else {
				if(m_resultsCount > ITableModel.DEFAULT_MAX_SIZE) {
					//in case that query does not cutoff rest of data (JDBC queries) report actual data size, but render as to large match
					m_pnlSearchCount.setCssClass("ui-lui-keyword-large");
				} else {
					m_pnlSearchCount.setCssClass("ui-lui-keyword-res");
				}
				m_pnlSearchCount.setText(Msgs.BUNDLE.formatMessage(Msgs.UI_KEYWORD_SEARCH_COUNT, "" + m_resultsCount));
			}
		}
	}

	public IValueChanged<KeyWordSearchInput<T>> getOnShowResults() {
		return m_onShowTypingResults;
	}

	public void setOnShowResults(IValueChanged<KeyWordSearchInput<T>> onShowResults) {
		m_onShowTypingResults = onShowResults;
	}

	@Override
	public void setFocus() {
		if(m_keySearch != null) {
			m_keySearch.setFocus();
		}
	}

	/**
	 * Get current window Z index and set its value to current control.
	 */
	private void fixZIndex() {
		//bug fix for IE when combining relative positioning, and overlapping control with absolute positioning.
		FloatingWindow parentFloatingWindow = getParent(FloatingWindow.class);
		int parentWindowZIndex = 0;
		if(parentFloatingWindow != null) {
			parentWindowZIndex = parentFloatingWindow.getZIndex();
		}
		if(parentWindowZIndex < 0) {
			parentWindowZIndex = 0;
		}
		setZIndex(parentWindowZIndex);
	}

	public void showResultsHintPopup(ITableModel<T> popupResults) throws Exception {
		assert (isBuilt());
		if(popupResults == null) {
			if(m_pnlSearchPopup != null) {
				removeChild(m_pnlSearchPopup);
				m_pnlSearchPopup = null;
			}
			fixZIndex(); //do not delete, fix for bug in IE
			m_keySearch.setZIndex(getZIndex()); //do not delete, fix for bug in domui
		} else {
			if(m_pnlSearchPopup == null) {
				m_pnlSearchPopup = new Div();
				m_pnlSearchPopup.setCssClass("ui-lui-keyword-popup");
				fixZIndex();
				//increase Z index both for current DIV and popup DIV.
				setZIndex(getZIndex() + 1); //FIXME: unfortunatelly this value never gets to browser. So that is why it is fixed in javascript on browser side.
				m_pnlSearchPopup.setZIndex(getZIndex());
				add(m_pnlSearchPopup);
			} else {
				m_pnlSearchPopup.removeAllChildren();
			}

			assert (m_resultsHintPopupRowRenderer != null);

			DataTable<T> tbl = new DataTable<T>(popupResults, m_resultsHintPopupRowRenderer);
			m_pnlSearchPopup.add(tbl);
			tbl.setWidth("100%");
			tbl.setOverflow(Overflow.HIDDEN);
			tbl.setPosition(PositionType.RELATIVE);
			m_pnlSearchPopup.setDisplay(DisplayType.NONE);
		}
	}

	public IRowRenderer<T> getResultsHintPopupRowRenderer() {
		return m_resultsHintPopupRowRenderer;
	}

	public void setResultsHintPopupRowRenderer(IRowRenderer<T> resultsHintPopupRowRenderer) {
		m_resultsHintPopupRowRenderer = resultsHintPopupRowRenderer;
	}
}

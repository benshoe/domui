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
package to.etc.domui.component.tbl;

import java.util.*;

import javax.annotation.*;

import to.etc.domui.component.meta.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.util.*;

/**
 * POC for a datatable based on the live dom code.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jun 1, 2008
 */
public class DataTable<T> extends TabularComponentBase<T> {
	private Table m_table = new Table();

	private IRowRenderer<T> m_rowRenderer;

	/** The size of the page */
	private int m_pageSize;

	/** If a result is visible this is the data table */
	private TBody m_dataBody;

	/** When the query has 0 results this is set to the div displaying that message. */
	private Div m_errorDiv;

	/** The items that are currently on-screen, to prevent a reload from the model when reused. */
	final private List<T> m_visibleItemList = new ArrayList<T>();

	public DataTable(@Nonnull ITableModel<T> m, @Nonnull IRowRenderer<T> r) {
		super(m);
		m_rowRenderer = r;
	}

	public DataTable(@Nonnull ITableModel<T> m) {
		super(m);
	}

	/**
	 * Return the backing table for this data browser. For component extension only - DO NOT MAKE PUBLIC.
	 * @return
	 */
	@Nullable
	protected Table getTable() {
		return m_table;
	}

	/**
	 * UNSTABLE INTERFACE - UNDER CONSIDERATION.
	 * @param dataBody
	 */
	protected void setDataBody(@Nullable TBody dataBody) {
		m_dataBody = dataBody;
	}

	@Nullable
	protected TBody getDataBody() {
		return m_dataBody;
	}

	@Override
	public void createContent() throws Exception {
		m_dataBody = null;
		m_errorDiv = null;
		setCssClass("ui-dt");

		//-- Ask the renderer for a sort order, if applicable
		m_rowRenderer.beforeQuery(this); // ORDER!! BEFORE CALCINDICES or any other call that materializes the result.

		calcIndices(); // Calculate rows to show.

		List<T> list = getPageItems(); // Data to show
		if(list.size() == 0) {
			setNoResults();
			return;
		}

		setResults();

		//-- Render the rows.
		ColumnContainer<T> cc = new ColumnContainer<T>(this);
		m_visibleItemList.clear();
		int ix = m_six;
		for(T o : list) {
			m_visibleItemList.add(o);
			TR tr = new TR();
			m_dataBody.add(tr);
			cc.setParent(tr);
			renderRow(tr, cc, ix, o);
			ix++;
		}
		appendCreateJS(JavascriptUtil.disableSelection(this)); // Needed to prevent ctrl+click in IE doing clipboard-select, because preventDefault does not work there of course.
	}

	private void setResults() throws Exception {
		if(m_errorDiv != null) {
			m_errorDiv.remove();
			m_errorDiv = null;
		}
		if(m_dataBody != null)
			return;

		m_table.removeAllChildren();
		add(m_table);

		//-- Render the header.
		THead hd = new THead();
		HeaderContainer<T> hc = new HeaderContainer<T>(this);
		TR tr = new TR();
		tr.setCssClass("ui-dt-hdr");
		hd.add(tr);
		hc.setParent(tr);
		renderHeader(hc);
		if(hc.hasContent()) {
			m_table.add(hd);
		} else {
			hc = null;
			hd = null;
			tr = null;
		}

		m_dataBody = new TBody();
		m_table.add(m_dataBody);
	}

	/**
	 * Removes any data table, and presents the "no results found" div.
	 */
	private void setNoResults() {
		m_visibleItemList.clear();
		if(m_errorDiv != null)
			return;

		if(m_table != null) {
			m_table.removeAllChildren();
			m_table.remove();
			m_dataBody = null;
		}

		m_errorDiv = new Div();
		m_errorDiv.setCssClass("ui-dt-nores");
		m_errorDiv.setText(Msgs.BUNDLE.getString(Msgs.UI_DATATABLE_EMPTY));
		add(m_errorDiv);
		return;
	}

	//	private void updateResults(int count) throws Exception {
	//		if(count == 0)
	//			setNoResults();
	//		else
	//			setResults();
	//	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Dumbass setters and getters.						*/
	/*--------------------------------------------------------------*/
	@Override
	public int getPageSize() {
		return m_pageSize;
	}

	public void setPageSize(int pageSize) {
		if(m_pageSize == pageSize)
			return;
		m_pageSize = pageSize;
		forceRebuild();
		firePageChanged();
	}


	/**
	 * Renders row content into specified row.
	 *
	 * @param cc
	 * @param index
	 * @param value
	 * @throws Exception
	 */
	protected void renderRow(@Nonnull TR tr, @Nonnull ColumnContainer<T> cc, int index, @Nullable T value) throws Exception {
		m_rowRenderer.renderRow(this, cc, index, value);
	}

	/**
	 * Renders row header into specified header container.
	 * It can be overriden if some specific content rendering is needed in sub class.
	 * @param hc specified header container
	 * @throws Exception
	 */
	protected void renderHeader(@Nonnull HeaderContainer<T> hc) throws Exception {
		m_rowRenderer.renderHeader(this, hc);
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	TableModelListener implementation					*/
	/*--------------------------------------------------------------*/
	/**
	 * Called when there are sweeping changes to the model. It forces a complete re-render of the table.
	 */
	@Override
	public void modelChanged(@Nullable ITableModel<T> model) {
		forceRebuild();
	}

	/**
	 * Row add. Determine if the row is within the paged-in indexes. If not we ignore the
	 * request. If it IS within the paged content we insert the new TR. Since this adds a
	 * new row to the visible set we check if the resulting rowset is not bigger than the
	 * page size; if it is we delete the last node. After all this the renderer will render
	 * the correct result.
	 * When called the actual insert has already taken place in the model.
	 *
	 * @see to.etc.domui.component.tbl.ITableModelListener#rowAdded(to.etc.domui.component.tbl.ITableModel, int, java.lang.Object)
	 */
	@Override
	public void rowAdded(@Nonnull ITableModel<T> model, int index, @Nullable T value) throws Exception {
		if(!isBuilt())
			return;
		calcIndices(); // Calculate visible nodes
		if(index < m_six || index >= m_eix) // Outside visible bounds
			return;

		//-- What relative row?
		setResults();
		int rrow = index - m_six; // This is the location within the child array
		ColumnContainer<T> cc = new ColumnContainer<T>(this);
		TR tr = new TR();
		cc.setParent(tr);
		renderRow(tr, cc, index, value);
		m_dataBody.add(rrow, tr);
		m_visibleItemList.add(rrow, value);

		//-- Is the size not > the page size?
		if(m_pageSize > 0 && m_dataBody.getChildCount() > m_pageSize) {
			//-- Delete the last row.
			m_dataBody.removeChild(m_dataBody.getChildCount() - 1); // Delete last element
		}
		while(m_visibleItemList.size() > m_pageSize)
			m_visibleItemList.remove(m_visibleItemList.size() - 1);
	}

	/**
	 * Delete the row specified. If it is not visible we do nothing. If it is visible we
	 * delete the row. This causes one less row to be shown, so we check if we have a pagesize
	 * set; if so we add a new row at the end IF it is available.
	 *
	 * @see to.etc.domui.component.tbl.ITableModelListener#rowDeleted(to.etc.domui.component.tbl.ITableModel, int, java.lang.Object)
	 */
	@Override
	public void rowDeleted(@Nonnull ITableModel<T> model, int index, @Nullable T value) throws Exception {
		if(!isBuilt())
			return;
		if(index < m_six || index >= m_eix) // Outside visible bounds
			return;
		int rrow = index - m_six; // This is the location within the child array
		m_dataBody.removeChild(rrow); // Discard this one;
		m_visibleItemList.remove(rrow);
		if(m_dataBody.getChildCount() == 0) {
			setNoResults();
			return;
		}

		//-- One row gone; must we add one at the end?
		int peix = m_six + m_pageSize - 1; // Index of last element on "page"
		if(m_pageSize > 0 && peix < m_eix) {
			ColumnContainer<T> cc = new ColumnContainer<T>(this);
			TR tr = new TR();
			cc.setParent(tr);
			renderRow(tr, cc, peix, getModelItem(peix));
			m_dataBody.add(m_pageSize - 1, tr);
			m_visibleItemList.add(m_pageSize - 1, value);
		}
	}

	/**
	 * Merely force a full redraw of the appropriate row.
	 *
	 * @see to.etc.domui.component.tbl.ITableModelListener#rowModified(to.etc.domui.component.tbl.ITableModel, int, java.lang.Object)
	 */
	@Override
	public void rowModified(@Nonnull ITableModel<T> model, int index, @Nullable T value) throws Exception {
		if(!isBuilt())
			return;
		if(index < m_six || index >= m_eix) // Outside visible bounds
			return;
		int rrow = index - m_six; // This is the location within the child array
		TR tr = (TR) m_dataBody.getChild(rrow); // The visible row there
		tr.removeAllChildren(); // Discard current contents.
		m_visibleItemList.set(rrow, value);

		ColumnContainer<T> cc = new ColumnContainer<T>(this);
		cc.setParent(tr);
		renderRow(tr, cc, index, value);
	}

	public void setTableWidth(@Nullable String w) {
		m_table.setTableWidth(w);
	}

	@Nonnull
	public IRowRenderer<T> getRowRenderer() {
		return m_rowRenderer;
	}

	public void setRowRenderer(@Nonnull IRowRenderer<T> rowRenderer) {
		if(DomUtil.isEqual(m_rowRenderer, rowRenderer))
			return;
		m_rowRenderer = rowRenderer;
		forceRebuild();
	}

	@Override
	protected void onForceRebuild() {
		m_visibleItemList.clear();
		super.onForceRebuild();
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	ISelectionListener.									*/
	/*--------------------------------------------------------------*/
	/**
	 * Called when a selection event fires. The underlying model has already been changed. It
	 * tries to see if the row is currently paged in, and if so asks the row renderer to update
	 * it's selection presentation.
	 *
	 * @see to.etc.domui.component.tbl.ISelectionListener#selectionChanged(java.lang.Object, boolean)
	 */
	public void selectionChanged(T row, boolean on) throws Exception {
		//-- Is this a visible row?
		for(int i = 0; i < m_visibleItemList.size(); i++) {
			if(MetaManager.areObjectsEqual(row, m_visibleItemList.get(i))) {
				updateSelectionChanged(row, i, on);
				return;
			}
		}
	}

	/**
	 * Updates the "selection" state of the specified local row#.
	 * @param instance
	 * @param i
	 * @param on
	 */
	private void updateSelectionChanged(T instance, int lrow, boolean on) throws Exception {
		TR tr = (TR) m_dataBody.getChild(lrow);
		THead head = m_table.getHead();
		if(null == head)
			throw new IllegalStateException("I've lost my head!?");
		TR headerrow = (TR) head.getChild(0);
		m_rowRenderer.renderSelectionChanged(this, headerrow, tr, instance, on);
	}
}

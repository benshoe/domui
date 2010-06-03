package to.etc.domui.component.lookup;

import java.util.*;

import javax.annotation.*;

import to.etc.domui.component.buttons.*;
import to.etc.domui.component.form.*;
import to.etc.domui.component.layout.*;
import to.etc.domui.component.meta.*;
import to.etc.domui.component.meta.impl.*;
import to.etc.domui.dom.css.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.server.*;
import to.etc.domui.state.*;
import to.etc.domui.util.*;
import to.etc.webapp.*;
import to.etc.webapp.query.*;

/**
 * Creates a search box to enter search criteria. This only presents the search part of the
 * form, constructed by metadata where needed, and the "search", "clear fields" and optional
 * "new" buttons. The actual searching must be done by the user of this component.
 * <p>The component will return a QCriteria query representing the search query constructed
 * by the user. This QCriteria object can, after retrieval, be used to add extra search
 * restrictions easily.</p>
 * <p>When used as-is, this form will use the class' metadata to discover any defined search
 * properties, and then populate the form with lookup controls which allow searches on those
 * properties. This is for "default" lookup screens. For more complex screens or lookup parts
 * that have controls interact with eachother you can manually define the contents of the
 * lookup form. By adding lookup items manually you <i>disable</i> the automatic discovery of
 * search options. This is proper because no form should <b>ever</b> depend on the content,
 * structure or order of metadata-defined lookup items!!! So if you want to manipulate the
 * lookup form's contents you have to define it's layout by hand.</p>
 * <p>Defining a form by hand is easy. To just add a property to search for to the form call
 * addProperty(String propname). This will create the default lookup input thing and label
 * for the property, as defined by metadata and factories. If you need more control you can
 * also call one of the addManualXXXX methods which allow full control over the controls
 * and search criteria used by the form.</p>
 * <p>Each search item added will usually return a LookupForm.Item. This is a handle to the
 * created lookup control and associated data and can be used to manipulate the control or
 * it's presentation at runtime.</p>
 * <p>The constructor for this control accepts an ellipsis list of property names to quickly
 * create a lookup using user-specified properties.</p>
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jul 14, 2008
 */
public class LookupForm<T> extends Div {
	/** The data class we're looking for */
	@Nonnull
	private Class<T> m_lookupClass;

	/** The metamodel for the class. */
	@Nonnull
	private ClassMetaModel m_metaModel;

	private String m_title;

	IClicked<LookupForm<T>> m_clicker;

	private IClicked<LookupForm<T>> m_onNew;

	private DefaultButton m_newBtn;

	private IClicked< ? extends LookupForm<T>> m_onClear;

	private IClicked<LookupForm<T>> m_onCancel;

	private DefaultButton m_cancelBtn;

	private DefaultButton m_collapseButton;

	private Table m_table;

	private TBody m_tbody;

	private Div m_content;

	private NodeContainer m_collapsed;

	private NodeContainer m_buttonRow;

	private ControlBuilder m_builder;

	/**
	 * Set to true in case that control have to be rendered as collapsed by default. It is used when lookup form have to popup with initial search results already shown.
	 */
	private boolean m_renderAsCollapsed;

	/**
	 * This is the definition for an Item to look up. A list of these
	 * will generate the actual lookup items on the screen, in the order
	 * specified by the item definition list.
	 *
	 * FIXME Should this actually be public??
	 *
	 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
	 * Created on Jul 31, 2009
	 */
	public static class Item implements SearchPropertyMetaModel {
		private String m_propertyName;

		private List<PropertyMetaModel> m_propertyPath;

		private ILookupControlInstance m_instance;

		private boolean m_ignoreCase = true;

		private int m_minLength;

		private String m_labelText;

		private String m_lookupHint;

		private String m_errorLocation;

		private int m_order;

		private String testId;

		public String getPropertyName() {
			return m_propertyName;
		}

		public void setPropertyName(String propertyName) {
			m_propertyName = propertyName;
		}

		public List<PropertyMetaModel> getPropertyPath() {
			return m_propertyPath;
		}

		public void setPropertyPath(List<PropertyMetaModel> propertyPath) {
			m_propertyPath = propertyPath;
		}

		public PropertyMetaModel getLastProperty() {
			if(m_propertyPath == null || m_propertyPath.size() == 0)
				return null;
			return m_propertyPath.get(m_propertyPath.size() - 1);
		}

		public boolean isIgnoreCase() {
			return m_ignoreCase;
		}

		public void setIgnoreCase(boolean ignoreCase) {
			m_ignoreCase = ignoreCase;
		}

		public int getMinLength() {
			return m_minLength;
		}

		public void setMinLength(int minLength) {
			m_minLength = minLength;
		}

		public String getLabelText() {
			return m_labelText;
		}

		public void setLabelText(String labelText) {
			m_labelText = labelText;
		}

		public String getLookupLabel() {
			return m_labelText;
		}

		public String getErrorLocation() {
			return m_errorLocation;
		}

		public void setErrorLocation(String errorLocation) {
			m_errorLocation = errorLocation;
		}

		ILookupControlInstance getInstance() {
			return m_instance;
		}

		void setInstance(ILookupControlInstance instance) {
			m_instance = instance;
		}

		/**
		 * Unused; only present to satisfy the interface.
		 * @see to.etc.domui.component.meta.SearchPropertyMetaModel#getOrder()
		 */
		public int getOrder() {
			return m_order;
		}

		void setOrder(int order) {
			m_order = order;
		}

		public String getLookupHint() {
			return m_lookupHint;
		}

		public void setLookupHint(String lookupHint) {
			m_lookupHint = lookupHint;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Item:");
			if(m_propertyName != null) {
				sb.append(" property: ");
				sb.append(m_propertyName);
			}
			if(m_labelText != null) {
				sb.append(" label: ");
				sb.append(m_labelText);
			}
			return sb.toString();
		}

		public String getTestId() {
			return testId;
		}

		public void setTestId(String testId) {
			this.testId = testId;
		}
	}

	/**
	 * Item that is used internally by LookupForm to mark table break when creating search field components.
	 *
	 * @author <a href="mailto:vmijic@execom.eu">Vladimir Mijic</a>
	 * Created on 13 Oct 2009
	 */
	static private class ItemBreak extends Item {
		public ItemBreak() {}
	}

	/** The primary list of defined lookup items. */
	private List<Item> m_itemList = new ArrayList<Item>(20);

	static public enum ButtonMode {
		/** Show this button only when the lookup form is expanded */
		NORMAL,

		/** Show this button only when the lookup form is collapsed */
		COLLAPSED,

		/** Always show this button. */
		BOTH
	}

	/**
	 * A button that needs to be present @ the button bar.
	 *
	 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
	 * Created on Nov 3, 2009
	 */
	private static class ButtonRowItem {
		private int m_order;

		private ButtonMode m_mode;

		private NodeBase m_thingy;

		public ButtonRowItem(int order, ButtonMode mode, NodeBase thingy) {
			m_order = order;
			m_mode = mode;
			m_thingy = thingy;
		}

		public ButtonMode getMode() {
			return m_mode;
		}

		public int getOrder() {
			return m_order;
		}

		public NodeBase getThingy() {
			return m_thingy;
		}
	}

	/** The list of buttons to show on the button row. */
	private List<ButtonRowItem> m_buttonItemList = Collections.EMPTY_LIST;

	public LookupForm(@Nonnull final Class<T> lookupClass, String... propertyList) {
		this(lookupClass, (ClassMetaModel) null, propertyList);
	}

	/**
	 * Create a LookupForm to find instances of the specified class.
	 * @param lookupClass
	 */
	public LookupForm(@Nonnull final Class<T> lookupClass, @Nullable final ClassMetaModel cmm, String... propertyList) {
		m_lookupClass = lookupClass;
		m_metaModel = cmm != null ? cmm : MetaManager.findClassMeta(lookupClass);
		m_builder = DomApplication.get().getControlBuilder();
		for(String prop : propertyList)
			addProperty(prop);
		defineDefaultButtons();
	}

	public ClassMetaModel getMetaModel() {
		return m_metaModel;
	}

	/**
	 * Returns the class whose instances we're looking up (a persistent class somehow).
	 * @return
	 */
	public Class<T> getLookupClass() {
		if(null == m_lookupClass)
			throw new NullPointerException("The LookupForm's 'lookupClass' cannot be null");
		return m_lookupClass;
	}

	/**
	 * QUESTIONABLE INTERFACE: This is actually typeless so should not be on a LookupForm&lt;T&gt; - by definition this class will never be a Class&lt;T&gt;...
	 * Change the class for which we are searching. This clear ALL definitions!
	 * @param lookupClass
	 */
	@Deprecated
	public void setLookupClass(@Nonnull final Class<T> lookupClass) {
		if(m_lookupClass == lookupClass)
			return;
		m_lookupClass = lookupClass;
		m_metaModel = MetaManager.findClassMeta(lookupClass);
		reset();
	}

	/**
	 * QUESTIONABLE INTERFACE: This is actually typeless so should not be on a LookupForm&lt;T&gt; - by definition this class will never be a Class&lt;T&gt;...
	 * Change the class and metamodel for which we are searching. This clear ALL definitions!
	 * @param lookupClass
	 */
	@Deprecated
	public void setLookupClass(@Nonnull final Class<T> lookupClass, @Nonnull ClassMetaModel cmm) {
		if(m_lookupClass == lookupClass)
			return;
		m_lookupClass = lookupClass;
		m_metaModel = cmm;
		reset();
	}

	/**
	 * Actually show the thingy.
	 * @see to.etc.domui.dom.html.NodeBase#createContent()
	 */
	@Override
	public void createContent() throws Exception {
		//-- If a page title is present render the search block in a CaptionedPanel, else present in it;s own div.
		Div sroot = new Div();
		if(getPageTitle() != null) {
			CaptionedPanel cp = new CaptionedPanel(getPageTitle(), sroot);
			add(cp);
			m_content = cp;
		} else {
			add(sroot);
			m_content = sroot;
		}
		NodeContainer searchContainer = sroot;
		if(containsItemBreaks(m_itemList)) {
			Table searchRootTable = new Table();
			sroot.add(searchRootTable);
			TBody searchRootTableBody = new TBody();
			searchRootTable.add(searchRootTableBody);
			TR searchRootRow = new TR();
			searchRootTableBody.add(searchRootRow);
			TD searchRootCell = new TD();
			searchRootCell.setValign(TableVAlign.TOP);
			searchRootRow.add(searchRootCell);
			searchContainer = searchRootCell;
		}

		//-- Walk all search fields
		m_table = new Table();
		searchContainer.add(m_table);
		m_tbody = new TBody();
		m_tbody.setTestID("tableBodyLookupForm");
		m_table.add(m_tbody);

		//-- Ok, we need the items we're going to show now.
		if(m_itemList.size() == 0) // If we don't have an item set yet....
			setItems(); // ..define it from metadata, and abort if there is nothing there

		//-- Start populating the lookup form with lookup items.
		for(Item it : m_itemList) {
			if(it instanceof ItemBreak) {
				TD anotherSearchRootCell = new TD();
				anotherSearchRootCell.setValign(TableVAlign.TOP);
				searchContainer.appendAfterMe(anotherSearchRootCell);
				searchContainer = anotherSearchRootCell;
				m_table = new Table();
				searchContainer.add(m_table);
				m_tbody = new TBody();
				m_tbody.setTestID("tableBodyLookupForm");
				m_table.add(m_tbody);
			} else {
				internalAddLookupItem(it);
			}
		}

		//-- The button bar.
		Div d = new Div();
		d.setTestID("buttonBar");
		sroot.add(d);
		m_buttonRow = d;

		//20091127 vmijic - since LookupForm can be reused each new rebuild should execute restore if previous state of form was collapsed.
		//20100118 vmijic - since LookupForm can be by default rendered as collapsed checks m_renderAsCollapsed are added.
		if(!m_renderAsCollapsed && m_collapsed != null) {
			restore();
		} else if(m_renderAsCollapsed && m_content.getDisplay() != DisplayType.NONE) {
			collapse();
			//Focus must be set, otherwise IE reports javascript problem since focus is requested on not displayed input tag.
			m_cancelBtn.setFocus();
		} else {
			createButtonRow(d, false);
		}

		//-- Add a RETURN PRESSED handler to allow pressing RETURN on search fields.
		setReturnPressed(new IReturnPressed() {
			public void returnPressed(final Div node) throws Exception {
				if(m_clicker != null)
					m_clicker.clicked(LookupForm.this);
			}
		});
	}

	protected void defineDefaultButtons() {
		DefaultButton b = new DefaultButton(Msgs.BUNDLE.getString(Msgs.LOOKUP_FORM_SEARCH));
		b.setIcon("THEME/btnFind.png");
		b.setTestID("searchButton");
		b.setClicked(new IClicked<NodeBase>() {
			public void clicked(final NodeBase bx) throws Exception {
				if(m_clicker != null)
					m_clicker.clicked(LookupForm.this);
			}
		});
		addButtonItem(b, 100, ButtonMode.NORMAL);

		b = new DefaultButton(Msgs.BUNDLE.getString(Msgs.LOOKUP_FORM_CLEAR));
		b.setIcon("THEME/btnClear.png");
		b.setTestID("clearButton");
		b.setClicked(new IClicked<NodeBase>() {
			public void clicked(final NodeBase xb) throws Exception {
				clearInput();
				if(getOnClear() != null)
					((IClicked<LookupForm<T>>) getOnClear()).clicked(LookupForm.this); // FIXME Another generics snafu, fix.
			}
		});
		addButtonItem(b, 200, ButtonMode.NORMAL);

		//-- Collapse button thingy
		m_collapseButton = new DefaultButton(Msgs.BUNDLE.getString(Msgs.LOOKUP_FORM_COLLAPSE), "THEME/btnHideLookup.png", new IClicked<DefaultButton>() {
			public void clicked(DefaultButton bx) throws Exception {
				collapse();
			}
		});
		m_collapseButton.setTestID("hideButton");
		addButtonItem(m_collapseButton, 500, ButtonMode.BOTH);
	}

	private boolean containsItemBreaks(List<Item> itemList) {
		for(Item item : itemList) {
			if(item instanceof ItemBreak) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This hides the search panel and adds a small div containing only the (optional) new and restore buttons.
	 */
	void collapse() {
		if(m_content.getDisplay() == DisplayType.NONE)
			return;
		//		appendJavascript("$('#" + m_content.getActualID() + "').slideUp();");
		m_content.slideUp();

		//		m_content.setDisplay(DisplayType.NONE);
		m_collapsed = new Div();
		m_collapsed.setCssClass("ui-lf-coll");
		add(m_collapsed);

		//-- Collapse button thingy
		m_collapseButton.setText(Msgs.BUNDLE.getString(Msgs.LOOKUP_FORM_RESTORE));
		m_collapseButton.setClicked(new IClicked<DefaultButton>() {
			public void clicked(DefaultButton bx) throws Exception {
				restore();
			}
		});
		createButtonRow(m_collapsed, true);
	}

	void restore() {
		if(m_collapsed == null)
			return;
		m_collapsed.remove();
		m_collapsed = null;
		createButtonRow(m_buttonRow, false);

		m_collapseButton.setText(Msgs.BUNDLE.getString(Msgs.LOOKUP_FORM_COLLAPSE));
		m_collapseButton.setClicked(new IClicked<DefaultButton>() {
			public void clicked(DefaultButton bx) throws Exception {
				collapse();
			}
		});

		m_content.setDisplay(DisplayType.BLOCK);
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Altering/defining the lookup items.					*/
	/*--------------------------------------------------------------*/
	/**
	 * This adds all properties that are defined as "search" properties in the metadata
	 * to the item list. The list is cleared before that!
	 */
	private void setItems() {
		m_itemList.clear();
		List<SearchPropertyMetaModelImpl> list = getMetaModel().getSearchProperties();
		if(list == null || list.size() == 0) {
			list = MetaManager.calculateSearchProperties(getMetaModel()); // 20100416 jal EXPERIMENTAL
			if(list == null || list.size() == 0)
				throw new IllegalStateException(getMetaModel() + " has no search properties defined in it's meta data.");
		}

		for(SearchPropertyMetaModel sp : list) { // The list is already in ascending order, so just add items;
			Item it = new Item();
			it.setIgnoreCase(sp.isIgnoreCase());
			it.setMinLength(sp.getMinLength());
			it.setPropertyName(sp.getPropertyName());
			it.setPropertyPath(sp.getPropertyPath());
			it.setLabelText(sp.getLookupLabel()); // If a lookup label is defined use it.
			it.setLookupHint(sp.getLookupHint()); // If a lookup hint is defined use it.
			addAndFinish(it);
		}
	}

	/**
	 * Add a property to look up to the list. The controls et al will be added using the factories.
	 * @param path		The property name (or path to some PARENT property) to search on, relative to the lookup class.
	 * @param minlen
	 * @param ignorecase
	 */
	public Item addProperty(String path, int minlen, boolean ignorecase) {
		return addProperty(path, null, minlen, Boolean.valueOf(ignorecase));
	}

	/**
	 * Add a property to look up to the list. The controls et al will be added using the factories.
	 * @param path		The property name (or path to some PARENT property) to search on, relative to the lookup class.
	 * @param minlen
	 */
	public Item addProperty(String path, int minlen) {
		return addProperty(path, null, minlen, null);
	}

	/**
	 * Add a property to look up to the list with user-specified label. The controls et al will be added using the factories.
	 * @param path	The property name (or path to some PARENT property) to search on, relative to the lookup class.
	 * @param label	The label text to use. Use the empty string to prevent a label from being generated. This still adds an empty cell for the label though.
	 */
	public Item addProperty(String path, String label) {
		return addProperty(path, label, 0, null);
	}

	/**
	 * Add a property to look up to the list. The controls et al will be added using the factories.
	 * @param path	The property name (or path to some PARENT property) to search on, relative to the lookup class.
	 */
	public Item addProperty(String path) {
		return addProperty(path, null, 0, null);
	}

	/**
	 * Add a property manually.
	 * @param path		The property name (or path to some PARENT property) to search on, relative to the lookup class.
	 * @param minlen
	 * @param ignorecase
	 */
	private Item addProperty(String path, String label, int minlen, Boolean ignorecase) {
		for(Item it : m_itemList) { // FIXME Useful?
			if(it.getPropertyName() != null && path.equals(it.getPropertyName())) // Already present there?
				throw new ProgrammerErrorException("The property " + path + " is already part of the search field list.");
		}

		//-- Define the item.
		Item it = new Item();
		it.setPropertyName(path);
		it.setLabelText(label);
		it.setIgnoreCase(ignorecase == null ? true : ignorecase.booleanValue());
		it.setMinLength(minlen);
		addAndFinish(it);
		return it;
	}

	public void addItemBreak() {
		ItemBreak itemBreak = new ItemBreak();
		m_itemList.add(itemBreak);
	}

	/**
	 * Add a manually-created lookup control instance to the item list.
	 * @return
	 */
	public Item addManual(ILookupControlInstance lci) {
		Item it = new Item();
		it.setInstance(lci);
		addAndFinish(it);
		return it;
	}

	/**
	 * Add a manually created control and link it to some property. The controls's configuration must be fully
	 * done by the caller; this will ask control factories to provide an ILookupControlInstance for the property
	 * and control passed in. The label for the lookup will come from property metadata.
	 *
	 * @param <X>
	 * @param property
	 * @param control
	 * @return
	 */
	public <X extends NodeBase & IInputNode< ? >> Item addManual(String property, X control) {
		Item it = new Item();
		it.setPropertyName(property);
		addAndFinish(it);

		//-- Add the generic thingy
		ILookupControlFactory lcf = m_builder.getLookupQueryFactory(it, control);
		ILookupControlInstance qt = lcf.createControl(it, control);
		if(qt == null || qt.getInputControls() == null || qt.getInputControls().length == 0)
			throw new IllegalStateException("Lookup factory " + lcf + " did not link thenlookup thingy for property " + it.getPropertyName());
		it.setInstance(qt);
		return it;
	}

	/**
	 * Add a manually-created lookup control instance with user-specified label to the item list.
	 * @return
	 */
	public Item addManualTextLabel(String labelText, ILookupControlInstance lci) {
		Item it = new Item();
		it.setInstance(lci);
		it.setLabelText(labelText);
		addAndFinish(it);
		return it;
	}

	/**
	 * Adds a manually-defined control, and use the specified property as the source for it's default label.
	 * @param property
	 * @param lci
	 * @return
	 */
	public Item addManualPropertyLabel(String property, ILookupControlInstance lci) {
		PropertyMetaModel pmm = getMetaModel().findProperty(property);
		if(null == pmm)
			throw new ProgrammerErrorException(property + ": undefined property for class=" + getLookupClass());
		return addManualTextLabel(pmm.getDefaultLabel(), lci);
	}

	/**
	 * Clear out the entire definition for this lookup form. After this it needs to be recreated completely.
	 */
	public void reset() {
		forceRebuild();
		m_itemList.clear();
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Internal.											*/
	/*--------------------------------------------------------------*/

	/**
	 * This adds the item to the item list, and tries to resolve all of the stuff needed to display
	 * the item. This means that the default label and the hint are calculated if missing, and that
	 * the lookup property is resolved if needed etc.
	 */
	private void addAndFinish(Item it) {
		m_itemList.add(it);

		//-- 1. If a property name is present but the path is unknown calculate the path
		if(it.getPropertyPath() == null && it.getPropertyName() != null && it.getPropertyName().length() > 0) {
			List<PropertyMetaModel> pl = MetaManager.parsePropertyPath(getMetaModel(), it.getPropertyName());
			if(pl.size() == 0)
				throw new ProgrammerErrorException("Unknown/unresolvable lookup property " + it.getPropertyName() + " on class=" + getLookupClass());
			it.setPropertyPath(pl);
		}

		//-- 2. Calculate/determine a label text if empty from metadata, else ignore
		PropertyMetaModel pmm = MetaUtils.findLastProperty(it); // Try to get metamodel
		if(it.getLabelText() == null) {
			if(pmm == null)
				it.setLabelText(it.getPropertyName()); // Last resort: default to property name if available
			else
				it.setLabelText(pmm.getDefaultLabel());
		}

		//-- 3. Calculate a default hint
		if(it.getLookupHint() == null) {
			if(pmm != null)
				it.setLookupHint(pmm.getDefaultHint());
		}

		//-- 4. Set an errorLocation
		if(it.getErrorLocation() == null) {
			it.setErrorLocation(it.getLabelText());
		}
	}


	/**
	 * Create the lookup item, depending on it's kind.
	 * @param it
	 */
	private void internalAddLookupItem(Item it) {
		if(it.getInstance() == null) {
			//-- Create everything using a control creation factory,
			ILookupControlInstance lci = createControlFor(it);
			if(lci == null)
				return;
			it.setInstance(lci);
		}
		if(it.getInstance() == null)
			throw new IllegalStateException("No idea how to create a lookup control for " + it);

		//-- Assign error locations to all input controls
		if(!DomUtil.isBlank(it.getErrorLocation()) ) {
			for(NodeBase ic : it.getInstance().getInputControls())
				ic.setErrorLocation(it.getErrorLocation());
		}

		//-- Assign test id. If single control is created, testId as it is will be applied,
		//   if multiple component control is created, testId with suffix number will be applied.
		if(!DomUtil.isBlank(it.getTestId())) {
			if(it.getInstance().getInputControls().length == 1) {
				it.getInstance().getInputControls()[0].setTestID(it.getTestId());
			} else if(it.getInstance().getInputControls().length > 1) {
				int controlCounter = 1;
				for(NodeBase ic : it.getInstance().getInputControls()) {
					ic.setTestID(it.getTestId() + "_" + controlCounter);
					controlCounter++;
				}
			}
		}

		addItemToTable(it); // Create visuals.
	}

	/**
	 * Add the visual representation of the item: add a row with a cell containing a label
	 * and another cell containing the lookup controls. This tries all the myriad ways of
	 * getting the label for the control.
	 *
	 * @param it	The fully completed item definition to add.
	 */
	private void addItemToTable(Item it) {
		ILookupControlInstance qt = it.getInstance();

		//-- Create control && label cells,
		TR tr = new TR();
		m_tbody.add(tr);
		TD lcell = new TD(); // Label cell
		tr.add(lcell);
		lcell.setCssClass("ui-f-lbl");

		TD ccell = new TD(); // Control cell
		tr.add(ccell);
		ccell.setCssClass("ui-f-in");

		//-- Now add the controls and shtuff..
		NodeBase labelcontrol = qt.getLabelControl();
		for(NodeBase b : qt.getInputControls()) { // Add all nodes && try to find label control if unknown.
			ccell.add(b);
			if(labelcontrol == null && b instanceof IInputNode< ? >)
				labelcontrol = b;
		}
		if(labelcontrol == null)
			labelcontrol = qt.getInputControls()[0];

		//-- Finally: add the label
		if(it.getLabelText() != null && it.getLabelText().length() > 0) {
			Label l = new Label(labelcontrol, it.getLabelText());
			//			if(l.getForNode() == null)
			//				l.setForNode(labelcontrol);
			lcell.add(l);
		}
	}

	/**
	 * Create the optimal control using metadata for a property. This can only be called for an item
	 * containing a property with metadata.
	 *
	 * @param container
	 * @param name
	 * @param pmm
	 * @return
	 */
	private ILookupControlInstance createControlFor(Item it) {
		PropertyMetaModel pmm = it.getLastProperty();
		if(pmm == null)
			throw new IllegalStateException("property cannot be null when creating using factory.");
		IRequestContext rq = PageContext.getRequestContext();
		boolean viewable = true;
		boolean editable = true;

		viewable = MetaManager.isAccessAllowed(pmm.getViewRoles(), rq);
		editable = MetaManager.isAccessAllowed(pmm.getEditRoles(), rq);
		if(!viewable) {
			//-- Check edit stuff:
			if(pmm.getEditRoles() == null) // No edit roles at all -> exit
				return null;
			if(!editable)
				return null;
		}

		ILookupControlFactory lcf = m_builder.getLookupControlFactory(it);
		ILookupControlInstance qt = lcf.createControl(it, null);
		if(qt == null || qt.getInputControls() == null || qt.getInputControls().length == 0)
			throw new IllegalStateException("Lookup factory " + lcf + " did not create a lookup thingy for property " + it.getPropertyName());
		return qt;
	}

	/**
	 * This checks all of the search fields for data. For every field that contains search
	 * data we check if the data is suitable for searching (not too short for instance); if
	 * it is we report errors. If the data is suitable <b>and</b> at least one field is filled
	 * we create a Criteria containing the search criteria.
	 *
	 * If anything goes wrong (one of the above mentioned errors occurs) ths returns null.
	 * If none of the input fields have data this will return a Criteria object, but the
	 * restrictions count in it will be zero. This can be used to query but will return all
	 * records.
	 *
	 * <h2>Internal working</h2>
	 * <p>Internally this just walks the list of thingies added when the components were added
	 * to the form. Each thingy refers to the input components used to register the search on a
	 * property, and knows how to convert that thingy to a criteria fragment.
	 * </p>
	 *
	 * @return
	 */
	public QCriteria<T> getEnteredCriteria() throws Exception {
		QCriteria<T> root = QCriteria.create(m_lookupClass);
		boolean success = true;
		for(Item it : m_itemList) {
			ILookupControlInstance li = it.getInstance();
			if(li != null) { // FIXME Is it reasonable to allow null here?? Should we not abort?
				if(!li.appendCriteria(root))
					success = false;
			}
		}
		if(!success) // Some input failed to validate their input criteria?
			return null; // Then exit null -> should only display errors.
		return root;
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Silly and small methods.							*/
	/*--------------------------------------------------------------*/
	/**
	 * Tells all input items to clear their content, clearing all user choices from the form. After
	 * this call, the form should return an empty QCriteria without any restrictions.
	 */
	public void clearInput() {
		for(Item it : m_itemList) {
			if(it.getInstance() != null)
				it.getInstance().clearInput();
		}
	}

	/**
	 * Sets the onNew handler. When set this will render a "new" button in the form's button bar.
	 * @return
	 */
	public IClicked<LookupForm<T>> getOnNew() {
		return m_onNew;
	}

	/**
	 * Returns the onNew handler. When set this will render a "new" button in the form's button bar.
	 * @param onNew
	 */
	public void setOnNew(final IClicked<LookupForm<T>> onNew) {
		if(m_onNew != onNew) {
			m_onNew = onNew;
			if(m_onNew != null && m_newBtn == null) {
				m_newBtn = new DefaultButton(Msgs.BUNDLE.getString(Msgs.LOOKUP_FORM_NEW));
				m_newBtn.setIcon("THEME/btnNew.png");
				m_newBtn.setTestID("newButton");
				m_newBtn.setClicked(new IClicked<NodeBase>() {
					public void clicked(final NodeBase xb) throws Exception {
						if(getOnNew() != null) {
							getOnNew().clicked(LookupForm.this);
						}
					}
				});
				addButtonItem(m_newBtn, 300, ButtonMode.BOTH);
			} else if(m_onNew == null && m_newBtn != null) {
				for(ButtonRowItem bri : m_buttonItemList) {
					if(bri.getThingy() == m_newBtn) {
						m_buttonItemList.remove(bri);
						break;
					}
				}
				m_newBtn = null;
			}
			forceRebuild();
		}
	}

	/**
	 * Returns the search block's part title, if present. Returns null if the title is not set.
	 */
	public String getPageTitle() {
		return m_title;
	}

	/**
	 * Sets a part title for this search block. When unset the search block does not have a title, when set
	 * the search block will be shown inside a CaptionedPanel.
	 * @param title
	 */
	public void setPageTitle(final String title) {
		m_title = title;
	}

	/**
	 * Set the handler to call when the "Search" button is clicked.
	 * @see to.etc.domui.dom.html.NodeBase#setClicked(to.etc.domui.dom.html.IClicked)
	 */
	@Override
	public void setClicked(final IClicked< ? > clicked) {
		m_clicker = (IClicked<LookupForm<T>>) clicked;
	}

	public IClicked< ? extends LookupForm<T>> getOnClear() {
		return m_onClear;
	}

	/**
	 * Listener to call when the "clear" button is pressed.
	 * @param onClear
	 */
	public void setOnClear(IClicked< ? extends LookupForm<T>> onClear) {
		m_onClear = onClear;
	}

	/**
	 * When set, this causes a "cancel" button to be added to the form. When that button is pressed this handler gets called.
	 * @param onCancel
	 */
	public void setOnCancel(IClicked<LookupForm<T>> onCancel) {
		if(m_onCancel != onCancel) {
			m_onCancel = onCancel;
			if(m_onCancel != null && m_cancelBtn == null) {
				m_cancelBtn = new DefaultButton(Msgs.BUNDLE.getString(Msgs.LOOKUP_FORM_CANCEL));
				m_cancelBtn.setIcon("THEME/btnCancel.png");
				m_cancelBtn.setTestID("cancelButton");
				m_cancelBtn.setClicked(new IClicked<NodeBase>() {
					public void clicked(final NodeBase xb) throws Exception {

						if(getOnCancel() != null) {
							getOnCancel().clicked(LookupForm.this);
						}
					}
				});
				addButtonItem(m_cancelBtn, 400, ButtonMode.BOTH);
			} else if(m_onCancel == null && m_cancelBtn != null) {
				for(ButtonRowItem bri : m_buttonItemList) {
					if(bri.getThingy() == m_cancelBtn) {
						m_buttonItemList.remove(bri);
						break;
					}
				}
				m_cancelBtn = null;
			}
			forceRebuild();
		}
	}

	public IClicked<LookupForm<T>> getOnCancel() {
		return m_onCancel;
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Button row code.									*/
	/*--------------------------------------------------------------*/

	public void addButtonItem(NodeBase b) {
		addButtonItem(b, m_buttonItemList.size(), ButtonMode.BOTH);
	}

	/**
	 * Add a button (or other item) to show on the button row. The item will
	 * be visible always.
	 * @param b
	 * @param order
	 */
	public void addButtonItem(NodeBase b, int order) {
		addButtonItem(b, order, ButtonMode.BOTH);
	}

	/**
	 * Add a button (or other item) to show on the button row.
	 *
	 * @param b
	 * @param order
	 * @param both
	 */
	public void addButtonItem(NodeBase b, int order, ButtonMode both) {
		if(m_buttonItemList == Collections.EMPTY_LIST)
			m_buttonItemList = new ArrayList<ButtonRowItem>(10);
		m_buttonItemList.add(new ButtonRowItem(order, both, b));
	}

	/**
	 * Add all buttons, both default and custom to buttom row.
	 * @param c
	 * @param iscollapsed
	 */
	private void createButtonRow(NodeContainer c, boolean iscollapsed) {
		Collections.sort(m_buttonItemList, new Comparator<ButtonRowItem>() { // Sort in ascending order,
				public int compare(ButtonRowItem o1, ButtonRowItem o2) {
					return o1.getOrder() - o2.getOrder();
				}
			});

		for(ButtonRowItem bi : m_buttonItemList) {
			if((iscollapsed && (bi.getMode() == ButtonMode.BOTH || bi.getMode() == ButtonMode.COLLAPSED)) || (!iscollapsed && (bi.getMode() == ButtonMode.BOTH || bi.getMode() == ButtonMode.NORMAL))) {
				c.add(bi.getThingy());
			}
		}
	}

	public boolean isRenderAsCollapsed() {
		return m_renderAsCollapsed;
	}

	public void setRenderAsCollapsed(boolean renderAsCollapsed) {
		m_renderAsCollapsed = renderAsCollapsed;
	}
}

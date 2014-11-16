package to.etc.domui.component2.form4;

import to.etc.domui.component.meta.*;
import to.etc.domui.component2.controlfactory.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.server.*;
import to.etc.webapp.annotations.*;

import javax.annotation.*;

/**
 * Yet another attempt at a generic form builder, using the Builder pattern. The builder
 * starts in vertical mode - call horizontal() to move horizontally.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jun 17, 2014
 */
final public class FormBuilder {
	/**
	 * Handle adding nodes generated by the form builder to the page.
	 *
	 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
	 * Created on Jun 13, 2012
	 */
	interface IAppender {
		void add(@Nonnull NodeBase formNode);
	}

	@Nonnull
	final private IAppender m_appender;

	private boolean m_horizontal;

	private boolean m_currentDirection;

	private String m_nextLabel;

	private Label m_nextLabelControl;

	private PropertyMetaModel< ? > m_propertyMetaModel;

	private Object m_instance;

	private Boolean m_mandatory;

	private boolean m_append;

	private Boolean m_readOnly;

	public FormBuilder(@Nonnull IAppender appender) {
		m_appender = appender;
	}

	public FormBuilder(@Nonnull final NodeContainer nb) {
		this(new IAppender() {
			@Override
			public void add(NodeBase formNode) {
				nb.add(formNode);
			}
		});
	}

	@Nonnull
	public FormBuilder horizontal() {
		m_horizontal = true;
		return this;
	}

	@Nonnull
	public FormBuilder vertical() {
		m_horizontal = false;
		return this;
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Label control.										*/
	/*--------------------------------------------------------------*/
	/**
	 *
	 * @param label
	 * @return
	 */
	@Nonnull
	public FormBuilder label(@Nonnull String label) {
		if(null != m_nextLabelControl)
			throw new IllegalStateException("You already set a Label instance");
		m_nextLabel = label;
		return this;
	}

	@Nonnull
	public FormBuilder label(@Nonnull Label label) {
		if(null != m_nextLabel)
			throw new IllegalStateException("You already set a String label instance");
		m_nextLabelControl = label;
		return this;
	}

	@Nonnull
	public FormBuilder unlabeled() {
		label("");
		return this;
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Readonly, mandatory, disabled.						*/
	/*--------------------------------------------------------------*/
	/**
	 *
	 * @return
	 */
	@Nonnull
	public FormBuilder readOnly() {
		m_readOnly = Boolean.TRUE;
		return this;
	}

	@Nonnull
	public FormBuilder readOnly(boolean ro) {
		m_readOnly = Boolean.valueOf(ro);
		return this;
	}

	@Nonnull
	public FormBuilder mandatory() {
		m_mandatory = true;
		return this;
	}

	/*--------------------------------------------------------------*/
	/*	CODING: defining (manually created) controls.				*/
	/*--------------------------------------------------------------*/
	/**
	 *
	 * @param control
	 * @throws Exception
	 */
	public void control(@Nonnull IControl< ? > control) throws Exception {
		if(control.isMandatory()) {
			m_mandatory = true;
		} else if(m_mandatory) {
			control.setMandatory(true);
		}
		addControl((NodeBase) control);
		resetBuilder();
	}

	@Nonnull
	public IControl< ? > control() throws Exception {
		return control((Class< ? extends IControl< ? >>) null);
	}

	@Nonnull
	public <T, C extends IControl<T>> C control(@Nullable Class<C> controlClass) throws Exception {
		ControlCreatorRegistry builder = DomApplication.get().getControlCreatorRegistry();
		PropertyMetaModel<T> pmm = (PropertyMetaModel<T>) m_propertyMetaModel;
		if(null == pmm)
			throw new IllegalStateException("You must have called 'property(...)' before");
		C control = builder.createControl(pmm, controlClass);
		bindControlData(control, pmm);
		addControl((NodeBase) control);
		resetBuilder();
		return control;
	}

	public void item(@Nonnull NodeBase item) throws Exception {
		addControl(item);
		resetBuilder();
	}

	public <T, C extends IControl<T>> void bindControlData(@Nonnull C control, @Nonnull PropertyMetaModel<T> pmm) throws Exception {


	}


	@Nonnull
	public <T> FormBuilder property(@Nonnull T instance, @GProperty String property) {
		if(null != m_propertyMetaModel)
			throw new IllegalStateException("You need to end the builder pattern with a call to 'control()'");
		m_propertyMetaModel = MetaManager.findPropertyMeta(instance.getClass(), property);
		m_instance = instance;
		return this;
	}

	private void resetBuilder() {
		m_readOnly = null;
		m_instance = null;
		m_propertyMetaModel = null;
		m_append = false;
		m_mandatory = null;
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Form building code.									*/
	/*--------------------------------------------------------------*/

	private Table m_table;

	private TBody m_body;

	private TR m_labelRow;

	private TR m_controlRow;

	private void addControl(@Nonnull NodeBase control) throws Exception {
		resetDirection();
		if(m_horizontal)
			addHorizontal(control);
		else
			addVertical(control);

		if(control instanceof IControl) {
			IControl< ? > ctl = (IControl< ? >) control;
			PropertyMetaModel< ? > pmm = m_propertyMetaModel;
			if(null != pmm) {
				Object instance = m_instance;
				if(null != instance) {
					ctl.bind().to(instance, pmm);
				}
			}

			Boolean ro = m_readOnly;
			if(null != ro) {
				if(ro.booleanValue()) {
					ctl.setReadOnly(true);
				}
			}
		}
	}


	private void resetDirection() {
		if(m_horizontal == m_currentDirection)
			return;
		clearTable();
		m_currentDirection = m_horizontal;
	}

	public FormBuilder nl() {
		clearTable();
		return this;
	}

	private void clearTable() {
		m_table = null;
		m_body = null;
		m_labelRow = null;
		m_controlRow = null;
	}

	@Nonnull
	private TBody body() {
		if(m_body == null) {
			Table tbl = m_table = new Table();
			m_appender.add(tbl);
			tbl.setCssClass(m_horizontal ? "ui-f4 ui-f4-h" : "ui-f4 ui-f4-v");
			tbl.setCellPadding("0");
			tbl.setCellSpacing("0");
			TBody b = m_body = new TBody();
			tbl.add(b);
			return b;
		}
		return m_body;
	}

	private void addVertical(NodeBase control) {
		TBody b = body();
		Label lbl = determineLabel();
		if(m_append) {
			TD cell = b.cell();
			if(lbl != null)
				cell.add(lbl);
			cell.add(control);
		} else {
			TD labelcell = b.addRowAndCell();
			labelcell.setCssClass("ui-f4-lbl ui-f4-lbl-v");
			if(null != lbl)
				labelcell.add(lbl);
			TD controlcell = b.addCell();
			controlcell.setCssClass("ui-f4-ctl ui-f4-ctl-v");
			controlcell.add(control);
		}
		if(null != lbl)
			lbl.setForNode(control);
	}

	@Nonnull
	private TR controlRow() {
		TR row = m_controlRow;
		if(null == row) {
			labelRow();
			row = m_controlRow = body().addRow();
		}
		return row;
	}

	@Nonnull
	private TR labelRow() {
		TR row = m_labelRow;
		if(null == row) {
			row = m_labelRow = body().addRow();
		}
		return row;
	}

	private void addHorizontal(NodeBase control) {
		TBody b = body();
		Label lbl = determineLabel();
		if(m_append) {
//			if(lbl != null)
//				labelCell().add(lbl);

			TR row = controlRow();
			TD cell;
			if(row.getChildCount() == 0) {
				cell = row.addCell();
				cell.setCssClass("ui-f4-ctl ui-f4-ctl-h");
			} else {
				cell = (TD) row.getChild(row.getChildCount() - 1);
			}
			cell.add(control);
		} else {
			TD labelcell = labelRow().addCell();
			labelcell.setCssClass("ui-f4-lbl ui-f4-lbl-h");
			if(null != lbl)
				labelcell.add(lbl);
			TD controlcell = controlRow().addCell();
			controlcell.setCssClass("ui-f4-ctl ui-f4-ctl-h");
			controlcell.add(control);
		}
		if(null != lbl)
			lbl.setForNode(control);
	}

	/**
	 *
	 * @return
	 */
	@Nullable
	private Label determineLabel() {
		Label res = null;
		String txt = m_nextLabel;
		if(null != txt) {
			m_nextLabel = null;
			if(txt.length() != 0)					// Not "unlabeled"?
				res = new Label(txt);
		} else {
			res = m_nextLabelControl;
			if(res != null) {
				m_nextLabelControl = null;
			} else {
				//-- Property known?
				PropertyMetaModel< ? > pmm = m_propertyMetaModel;
				if(null != pmm) {
					txt = pmm.getDefaultLabel();
					if(txt != null && txt.length() > 0)
						res = new Label(txt);
				}
			}
		}
		return res;
	}

}

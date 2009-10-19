package to.etc.domui.component.misc;

import to.etc.domui.dom.html.*;

public class PercentageCompleteRuler extends Div {
	private int m_percentage;

	private int m_pixelwidth;

	public PercentageCompleteRuler() {
		setWidth("100px");
		m_pixelwidth = 100;
	}

	public void setWidth(int pixels) {
		m_pixelwidth = pixels;
		setWidth(pixels + "px");
	}

	@Override
	public void createContent() throws Exception {
		setCssClass("ui-pct-rlr");
		updateValues();
	}

	public int getPercentage() {
		return m_percentage;
	}

	public void setPercentage(int percentage) {
		if(percentage > 100)
			percentage = 100;
		else if(percentage < 0)
			percentage = 0;
		if(m_percentage != percentage) {
			m_percentage = percentage;
			updateValues();
		}
	}

	private void updateValues() {
		setText(Integer.valueOf(m_percentage) + "%");

		//-- Set background position.
		int pxl = (m_percentage * m_pixelwidth / 100);
		setBackgroundPosition((-400 + pxl) + "px 0px");
	}
}

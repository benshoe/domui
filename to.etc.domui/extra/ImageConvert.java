package to.etc.domui.util.images.converters;

public class ImageConvert implements IImageConversionSpecifier {
	private String m_targetMime;

	public ImageConvert(String targetMime) {
		m_targetMime = targetMime;
	}

	public String getTargetMime() {
		return m_targetMime;
	}

	public String getConversionKey() {
		return m_targetMime.replace('/', '$');
	}
}
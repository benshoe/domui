package to.etc.util;

import java.io.*;

/**
 * Writes all data sent to it as a base64 encoded stream to the wrapped stream.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jan 24, 2010
 */
public class Base64OutputStream extends OutputStream {
	private OutputStream	m_os;

	private boolean				m_close;

	/** Holding buffer for an incomplete 3-byte set. */
	private byte[]			m_buf	= new byte[3];

	/** The #bytes currently in the holding buffer. */
	private int				m_holdix;

	static final private byte[]	BASE64MAP	= StringTool.getBase64Map();

	public Base64OutputStream(OutputStream os, boolean close) {
		m_os = os;
		m_close = close;
	}

	@Override
	public void close() throws IOException {
		switch(m_holdix){
			default:
				throw new IllegalStateException(m_holdix + " !?!?");
			case 0:
				//-- No bytes left. Begone.
				break;

			case 1:
				//-- One byte left. Need 2 chars and 2 chars padding.
				m_os.write(BASE64MAP[(m_buf[0] >>> 2) & 0x3f]); // First char
				m_os.write(BASE64MAP[(m_buf[0] << 4) & 0x3f]); // 2nd char, with only 2 top bits
				m_os.write('=');
				m_os.write('=');
				break;

			case 2:
				//-- 2 bytes left.
				m_os.write(BASE64MAP[(m_buf[0] >>> 2) & 0x3f]); // First char
				m_os.write(BASE64MAP[(m_buf[1] >>> 4) & 017 | (m_buf[0] << 4) & 077]);
				m_os.write(BASE64MAP[(m_buf[1] << 2) & 077]);
				m_os.write('=');
				break;
		}
		if(m_close)
			m_os.close();
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if(len <= 0)
			return;

		//-- If we have an incomplete run in the holding area try to complete it,
		if(m_holdix > 0) {
			while(m_holdix < 3) {
				if(len <= 0) // Source data exhausted- quit.
					return;
				m_buf[m_holdix++] = b[off++];
				len--;
			}

			//-- We have a full holding buffer now- encode
			encodePart(m_buf, 0, 3);
			m_holdix = 0;
		}
		if(len <= 0)
			return;

		//-- Encode all that we can (all complete 3-byte parts)
		int tbp = (len / 3) * 3; // Actual #of bytes that /can/ be moved now
		if(tbp > 0) {
			encodePart(b, off, tbp);
			len -= tbp; // This is left afterwards,
			off += tbp;
		}

		while(len-- > 0)
			m_buf[m_holdix++] = b[off++];
	}

	/**
	 * Encode full 3-byte pairs. The buffer is always a full set of 3-byte items.
	 * @param buf
	 * @param i
	 * @param j
	 */
	private void encodePart(byte[] data, int off, int len) throws IOException {
		int eidx = off + len;

		for(int sidx = off; sidx < eidx; sidx += 3) {
			m_os.write(BASE64MAP[(data[sidx] >>> 2) & 0x3f]);
			m_os.write(BASE64MAP[(data[sidx + 1] >>> 4) & 017 | (data[sidx] << 4) & 077]);
			m_os.write(BASE64MAP[(data[sidx + 2] >>> 6) & 003 | (data[sidx + 1] << 2) & 077]);
			m_os.write(BASE64MAP[data[sidx + 2] & 077]);
		}
	}

	@Override
	public void write(int b) throws IOException {
		m_buf[m_holdix++] = (byte) b;
		if(m_holdix < 3)
			return;

		//-- Encode a new 3-byte pair to values
		encodePart(m_buf, 0, 3);
		m_holdix = 0;
	}
}

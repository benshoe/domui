package to.etc.el.node;

import java.io.*;

/**
 * Handles indented writing for dumps and the like.
 * 
 * @author jal
 * Created on May 3, 2004
 */
public class IndentWriter extends Writer {
	/** The writer to dump to. */
	private Writer m_pw;

	private boolean m_dontclose;

	/**
	 * Constructor
	 */
	private IndentWriter() {}

	public IndentWriter(Writer w) {
		m_pw = w;
	}

	public IndentWriter(Writer w, boolean dontclose) {
		m_pw = w;
		m_dontclose = dontclose;
	}

	@Override
	public void close() throws IOException {
		if(m_dontclose)
			m_pw.flush();
		else
			m_pw.close();
	}

	@Override
	public void flush() throws IOException {
		m_pw.flush();
	}


	private void write(char c) throws IOException {
		if(m_pw != null)
			m_pw.write(c);
	}

	private final static int cIndFactor = 4;

	/** Current code indent level */
	protected int m_ind_level = 0;

	/** The current x-position in the file. */
	protected int m_ind_x = 0;

	public void setInd(int i) {
		m_ind_level = i;
	}

	@Override
	public void write(char[] buf, int off, int len) throws IOException {
		//** Ok. Generate the required string & indent after NL. Writes the code
		//** to the code stream.
		int eoff = off + len;
		for(int ix = off; ix < eoff; ix++) {
			char c = buf[ix]; // Get nth character,
			if(c == '\n') // Newline?
			{
				write('\n'); // Add to output,
				m_ind_x = 0; // Current X star-of-line
			} else {
				//** Printchar. Are we before the index level?
				while(m_ind_x < m_ind_level) {
					m_ind_x++;
					write(' ');
				}
				write(c);
				m_ind_x++;
			}
		}
	}

	public void inc() {
		m_ind_level += cIndFactor;
	}

	public void dec() {
		if(m_ind_level >= cIndFactor)
			m_ind_level -= cIndFactor;
	}

	public void forceNewline() throws IOException {
		if(m_ind_x > 0)
			print("\n");
	}

	public void print(String s) throws IOException {
		char[] c = s.toCharArray();
		write(c, 0, c.length);
	}

	public void println(String s) throws IOException {
		print(s);
		print("\n");
	}

	static public IndentWriter getStdoutWriter() {
		OutputStreamWriter osw = new OutputStreamWriter(System.out);
		return new IndentWriter(osw, true);
	}

}

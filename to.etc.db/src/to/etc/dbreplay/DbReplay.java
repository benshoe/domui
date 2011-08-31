package to.etc.dbreplay;

import java.io.*;
import java.text.*;
import java.util.*;

import javax.annotation.*;

import to.etc.dbpool.*;

/**
 * This utility will replay a database logfile, for performance evaluation purposes.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Aug 31, 2011
 */
public class DbReplay {
	public static void main(String[] args) {
		try {
			new DbReplay().run(args);
		} catch(Exception x) {
			x.printStackTrace();
		}
	}

	private File m_inputFile;

	/** The buffered input file containing statements. */
	private BufferedInputStream m_bis;

	private long m_firstTime;

	private long m_lastTime;

	private long m_startTime;

	private long m_endTime;

	private File m_poolFile;

	private String m_poolId;

	private ConnectionPool m_pool;

	/** The #of separate executor threads to start. */
	private int m_executors = 20;

	/** The #of executors that are actually running/ready. */
	private int m_runningExecutors;

	private void run(String[] args) throws Exception {
		if(!decodeOptions(args))
			return;

		try {
			initialize();

			//-- Input distributor loop.
			m_startTime = System.currentTimeMillis();
			for(;;) {
				ReplayRecord rr = ReplayRecord.readRecord(this);
				if(null == rr)
					break;
				if(m_recordNumber == 0) {
					m_firstTime = rr.getStatementTime();
				}
				m_lastTime = rr.getStatementTime();

				m_recordNumber++;
			}
			m_endTime = System.currentTimeMillis();
			System.out.println("Normal EOF after " + m_recordNumber + " records and " + m_fileOffset + " file bytes");
			Date st = new Date(m_firstTime);
			Date et = new Date(m_lastTime);
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			System.out.println("  - input time from " + df.format(st) + " till " + df.format(et) + ", " + DbPoolUtil.strMillis(m_lastTime - m_firstTime));
			System.out.println("  - real time spent: " + DbPoolUtil.strMillis(m_endTime - m_startTime));
		} catch(Exception x) {
			System.err.println("Error: " + x);
			System.err.println("   -at record " + m_recordNumber + ", file offset " + m_fileOffset);
			x.printStackTrace();
		} finally {
			releaseAll();
		}
	}

	private boolean decodeOptions(String[] args) throws Exception {
		int argc = 0;
		while(argc < args.length) {
			String s = args[argc++];
			if(s.startsWith("-")) {
				if("-pf".equals(s) || "-poolfile".equals(s)) {
					if(argc >= args.length)
						throw new IllegalArgumentException("Missing file name after -poolfile");
					m_poolFile = new File(args[argc++]);
					if(!m_poolFile.exists() || !m_poolFile.isFile())
						throw new IllegalArgumentException(m_poolFile+": file not found");
				} else {
					usage("Unknown option: " + s);
					return false;
				}
			} else {
				if(m_inputFile == null) {
					m_inputFile = new File(s);
					if(!m_inputFile.exists() || !m_inputFile.isFile())
						throw new Exception(m_inputFile + ": file does not exist or is not a file.");
				} else if(m_poolId == null) {
					m_poolId = s;
				} else {
					usage("Unexpected extra argument on command line");
					return false;
				}
			}
		}

		if(m_inputFile == null) {
			usage("Missing input file name");
			return false;
		}
		if(m_poolId == null) {
			usage("Missing pool ID");
			return false;
		}

		return true;
	}

	private void usage(String msg) {
		System.out.println("Error: " + msg);
		System.out.println("Usage: DbReplay [options] filename poolID");
		System.out.println("Options are:\n" //
			+ "-poolfile|-pf [filename]: The name of the pool.properties defining the database connection.\n" //
		);
	}

	private void releaseAll() {
		try {
			terminateAll();
		} catch(Exception x) {
			x.printStackTrace();
		}
	}

	synchronized public ConnectionPool getPool() {
		return m_pool;
	}

	private void initialize() throws Exception {
		m_bis = new BufferedInputStream(new FileInputStream(m_inputFile), 65536);
		if(m_poolFile == null)
			m_pool = PoolManager.getInstance().definePool(m_poolId);
		else
			m_pool = PoolManager.getInstance().definePool(m_poolFile, m_poolId);

		startExecutors();
		waitForReady();
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Accessing the data stream.							*/
	/*--------------------------------------------------------------*/

	private long m_fileOffset;

	private int m_recordNumber;

	/**
	 *
	 * @param is
	 * @return
	 * @throws Exception
	 */
	@Nullable
	public String readString() throws Exception {
		int len = readInt();
		if(len < 0)
			return null;
		byte[] data = new byte[len];
		int szrd = m_bis.read(data);
		if(szrd != len)
			throw new IOException("Unexpected EOF: got " + szrd + " bytes but needed " + len);
		m_fileOffset += len;
		return new String(data, "utf-8");
	}


	public long readLong() throws Exception {
		long a = (readInt() & 0xffffffffl);
		long b = (readInt() & 0xffffffffl);
		return (a << 32) | b;
	}

	public int readInt() throws Exception {
		int v = m_bis.read();
		if(v == -1)
			throw new EOFException();
		int r = v << 24;

		v = m_bis.read();
		if(v == -1)
			throw new EOFException();
		r |= v << 16;

		v = m_bis.read();
		if(v == -1)
			throw new EOFException();
		r |= v << 8;

		v = m_bis.read();
		if(v == -1)
			throw new EOFException();
		r |= v;

		m_fileOffset += 4;
		return r;
	}

	public int readByte() throws Exception {
		int b = m_bis.read();
		if(-1 == b)
			throw new EOFException();
		return b;
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Execution framework.								*/
	/*--------------------------------------------------------------*/
	/** List of all registered executors. */
	private List<ReplayExecutor> m_executorList = new ArrayList<ReplayExecutor>(100);

	private void startExecutors() {
		System.out.println("init: starting " + m_executors + " executor threads");
		for(int i = 0; i < m_executors; i++) {
			ReplayExecutor rx = new ReplayExecutor(this, i);
			synchronized(this) {
				m_executorList.add(rx);
			}
			rx.setDaemon(true);
			rx.setName("x#" + i);
			rx.start();
		}
	}

	synchronized private List<ReplayExecutor> getExecutorList() {
		return new ArrayList<ReplayExecutor>(m_executorList);
	}

	public void executorReady(ReplayExecutor replayExecutor) {
		synchronized(this) {
			m_runningExecutors++;
			notifyAll();
		}
	}

	public void executorStopped(ReplayExecutor rx) {
		synchronized(this) {
			m_runningExecutors--;
			notifyAll();
		}
	}


	/**
	 * Force all executors into termination asap.
	 */
	public void terminateAll() throws Exception {
		//-- Force all executors to terminate.
		for(ReplayExecutor rx : getExecutorList()) {
			rx.terminate();
		}

		//-- Wait for all of them, max 30 secs.
		long ets = System.currentTimeMillis() + 30 * 1000; // Allow max. 30 seconds startup time.
		for(;;) {
			long ts = System.currentTimeMillis();
			if(ts >= ets) {
				//-- Failed to start!!! Abort.
				synchronized(this) {
					System.out.println("term: Timeout waiting for executors to terminate - " + m_runningExecutors + " of " + m_executors + " keep running");
				}
				return;
			}

			synchronized(this) {
				if(m_runningExecutors <= 0)
					break;
				System.out.println("term: waiting for " + m_runningExecutors + " of " + m_executors + " executors to terminate.");
				wait(5000);
			}
		}
		System.out.println("term: all executors stopped");
	}


	private void waitForReady() throws Exception {
		long ets = System.currentTimeMillis() + 30 * 1000; // Allow max. 30 seconds startup time.
		long lmt = 0;
		for(;;) {
			long ts = System.currentTimeMillis();
			if(ts >= ets) {
				//-- Failed to start!!! Abort.
				throw new RuntimeException("Timeout waiting for executors to start - aborting");
			}

			synchronized(this) {
				int ntodo = m_executors - m_runningExecutors;
				if(ntodo <= 0)
					break;
				if(ts >= lmt) {
					System.out.println("init: waiting for " + ntodo + " of " + m_executors + " executors to become ready.");
					lmt = ts + 5 * 1000;
				}
				wait(5000);
			}
		}
		System.out.println("init: ready for execution");
	}

}

package yoyo.common.thread;

public class JThread implements Runnable, IThread {

	private volatile boolean m_bRun; 	// 线程的运行状态
	private Thread m_Thread; 			// 线程
	private int m_dwPriority; 			// 线程的优先级
	private String m_szName; 			// 线程名称

	public JThread() {
		m_bRun = false;
		m_szName = this.getClass().getName();
		m_dwPriority = Thread.NORM_PRIORITY;
	}

	public JThread(String name, int dwPriority) {
		m_bRun = false;
		m_szName = name;
		m_dwPriority = dwPriority;
	}

	public JThread(int dwPriority) {
		m_bRun = false;
		m_szName = this.getClass().getName();
		m_dwPriority = dwPriority;
	}

	@Override
	public boolean IsRunning() {
		return m_bRun;
	}

	@Override
	public boolean StartThread() {
		if (IsRunning())
			return false;
		m_Thread = new Thread(this);
		m_Thread.setName(m_szName);
		m_Thread.setPriority(m_dwPriority);
		m_Thread.start();
		m_bRun = true;
		return m_bRun;
	}

	@Override
	public boolean ChoseThread() {
		if (IsRunning()) {
			m_bRun = false;
			if (m_Thread != null) {
				m_Thread.interrupt();
			}
			return true;
		}
		return false;
	}

	@Override
	public void sleep(long timeout) {
		try {
			Thread.sleep(timeout);
		} catch (InterruptedException e) {
			if(m_bRun){ e.printStackTrace(); }
		}
	}

	@Override
	public void run() {
		boolean bSuccess = OnEventThreadStart();
		while (bSuccess && IsRunning()) {
			bSuccess = OnEventThreadRun();
		}
		ChoseThread();
		OnEventThreadClose();
		m_Thread = null;
	}

	@Override
	public String toString() {
		return super.toString();
	}

	protected boolean OnEventThreadStart() {
		return false;
	}

	protected boolean OnEventThreadRun() {
		return false;
	}

	protected boolean OnEventThreadClose() {
		return false;
	}
}

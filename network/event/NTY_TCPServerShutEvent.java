package yoyo.network.event;

public class NTY_TCPServerShutEvent {
	private int	m_dwClientAddr;
	public int getClientAddr() {
		return m_dwClientAddr;
	}
	private long m_lActiveTime;
	public long getActiveTime() {
		return m_lActiveTime;
	}
	private int m_dwSocketID;
	public int getSocketID() {
		return m_dwSocketID;
	}

	public NTY_TCPServerShutEvent(int dwClientAddr,long lActiveTime, int dwSocketID) {
		m_dwClientAddr	= dwClientAddr;
		m_lActiveTime	= lActiveTime;
		m_dwSocketID	= dwSocketID;
	}
}

package yoyo.network.event;

public class NTY_TCPServerAcceptEvent {
	
	private int m_dwClientAddr;
	public int getClientAddr() {
		return m_dwClientAddr;
	}
	private int m_dwSocketID;
	public int getSocketID() {
		return m_dwSocketID;
	}
	
	public NTY_TCPServerAcceptEvent(int dwClientAddr, int dwSocketID) {
		m_dwClientAddr		= dwClientAddr;
		m_dwSocketID		= dwSocketID;
	}
}

package yoyo.network.event;

public class NTY_TCPClientShutEvent {
	private int m_dwServiceID;
	public int getServiceID(){
		return m_dwServiceID;
	}
	private byte m_cbShutReason;
	public byte getShutReason(){
		return m_cbShutReason;
	}
	
	public NTY_TCPClientShutEvent(int serviceID ,byte cbShutReason){
		m_dwServiceID 	= serviceID;
		m_cbShutReason 	= cbShutReason;
	}
}

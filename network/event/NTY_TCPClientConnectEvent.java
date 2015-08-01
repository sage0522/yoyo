package yoyo.network.event;

public class NTY_TCPClientConnectEvent {

	private int m_dwServiceID;
	public int getServiceID(){
		return m_dwServiceID;
	}
	private int m_dwErrorCode;
	public int getErrorCode(){
		return m_dwErrorCode;
	}
	
	public NTY_TCPClientConnectEvent(int serviceID ,int errorCode){
		m_dwServiceID = serviceID;
		m_dwErrorCode = errorCode;
	}
}

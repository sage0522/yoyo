package yoyo.database.event;

public class NTY_DBEvent {
	
	private int m_dwRequestID;
	public int getRequestID(){
		return m_dwRequestID;
	}
	
	private int m_dwSocketID;
	public int getSocketID(){
		return m_dwSocketID;
	}
	
	private Object m_pData;
	public Object getData(){
		return m_pData;
	}
	
	public NTY_DBEvent(int requestID ,int socketID ,Object pData){
		m_dwRequestID 	= requestID;
		m_dwSocketID	= socketID;
		m_pData			= pData;
	}
}

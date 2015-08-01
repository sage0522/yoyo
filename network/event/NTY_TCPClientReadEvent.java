package yoyo.network.event;

import java.nio.ByteBuffer;

public class NTY_TCPClientReadEvent {
	private int m_dwSerciceID;
	public int getServiceID(){
		return m_dwSerciceID;
	}
	
	private short m_wMainCmdID;
	public short getMainCmdID(){
		return m_wMainCmdID;
	}
	
	private short m_wSubCmdID;
	public short getSubCmdID(){
		return m_wSubCmdID;
	}
	
	private ByteBuffer m_pBuffer;
	public ByteBuffer getBuffer(){
		return m_pBuffer;
	}

	private short m_wDataSize;
	public short getDataSize() {
		return m_wDataSize;
	}
	public NTY_TCPClientReadEvent(int dwServiceID,short wMainCmdID ,short wSubCmdID ,
									ByteBuffer pBuffer,short wDataSize) {
		m_dwSerciceID = dwServiceID;
		m_wMainCmdID 	= wMainCmdID;
		m_wSubCmdID 	= wSubCmdID;
		m_pBuffer	 	= pBuffer;
		m_wDataSize = wDataSize;
	}
}

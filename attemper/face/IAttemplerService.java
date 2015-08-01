package yoyo.attemper.face;

import java.nio.ByteBuffer;

import yoyo.common.face.IServiceModule;

public interface IAttemplerService extends IServiceModule {
	
	/**
	 * 
	 * @param dwTimerID
	 * @param dwLeftRepeat
	 * @param bindParameter
	 * @return
	 */
	boolean OnEventTimer(int dwTimerID, int dwLeftRepeat, Object bindParameter);
	
	/**
	 * 
	 * @param dwRequestID
	 * @param dwSocketID
	 * @param pData
	 * @return
	 */
	boolean OnEventDataBase(int dwRequestID ,int dwSocketID ,Object pData);
	
	/**
	 * 
	 * @param dwSocketID
	 * @param dwClientAddr
	 * @return
	 */
	boolean OnServerAcceptEvent(int dwSocketID,int dwClientAddr);
	/**
	 * 
	 * @param dwSocketID
	 * @param wMainCmdID
	 * @param wSubCmdID
	 * @param pBuffer
	 * @param wDataSize
	 * @return
	 */
	boolean OnServerReadEvent(int dwSocketID,short wMainCmdID,short wSubCmdID,ByteBuffer pBuffer,short wDataSize);
	/**
	 * 
	 * @param dwSocketID
	 * @param dwClientAddr
	 * @param lActiveTime
	 * @return
	 */
	boolean OnServerShutEvent(int dwSocketID,int dwClientAddr,long lActiveTime);
	
	/**
	 * 
	 * @param serviceID
	 * @param errorCode
	 * @return
	 */
	boolean OnClientConnectEvent(int serviceID ,int errorCode);
	/**
	 * 
	 * @param dwServiceID
	 * @param wMainCmdID
	 * @param wSubCmdID
	 * @param pBuffer
	 * @param wDataSize
	 * @return
	 */
	boolean OnClientReadEvent(int dwServiceID,short wMainCmdID ,short wSubCmdID ,ByteBuffer pBuffer,short wDataSize);
	/**
	 * 
	 * @param serviceID
	 * @param cbShutReason
	 * @return
	 */
	boolean OnClientShutEvent(int serviceID ,byte cbShutReason);
}

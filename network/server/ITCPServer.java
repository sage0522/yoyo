package yoyo.network.server;

import java.nio.ByteBuffer;

import yoyo.common.face.IServiceModule;
import yoyo.queue.face.IAddQueueSink;

public interface ITCPServer extends IServiceModule {
	
	/**
	 * 
	 * @param dwPort
	 * @param dwMaxSocketItem
	 * @param bDetect
	 */
	void InitServerParameter(int dwPort ,int dwMaxSocketItem ,boolean bDetect);
	
	/**
	 * 
	 * @param pITimerAddQueueSink
	 * @return
	 */
	boolean SetAddQueueSink(IAddQueueSink pIAddQueueSink);
	/**
	 * 
	 * @param wMainCmdID
	 * @param wSubCmdID
	 * @param dwSocketID
	 * @return
	 */
	boolean SendData(short wMainCmdID,short wSubCmdID,int dwSocketID);
	/**
	 * 
	 * @param wMainCmdID
	 * @param wSubCmdID
	 * @param pData
	 * @param wDataSize
	 * @param dwSocketID
	 * @return
	 */
	boolean SendData(short wMainCmdID,short wSubCmdID,ByteBuffer pData,short wDataSize,int dwSocketID);
	/**
	 * 
	 * @param pData
	 * @param wDataSize
	 * @param wMainCmdID
	 * @param wSubCmdID
	 * @return
	 */
	boolean SendDataBatch(short wMainCmdID,short wSubCmdID,ByteBuffer pData,short wDataSize);
	/**
	 * 
	 * @param dwSocketID
	 * @return
	 */
	boolean CloseSocket(int dwSocketID);
	/**
	 * 
	 * @param dwSocketID
	 * @return
	 */
	boolean ShutDownSocket(int dwSocketID);
	/**
	 * 
	 * @param dwSocketID
	 * @param bAllowBatch
	 * @return
	 */
	boolean AllowBatchSend(int dwSocketID ,boolean bAllowBatch);
}

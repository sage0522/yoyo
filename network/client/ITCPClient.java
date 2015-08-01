package yoyo.network.client;

import java.nio.ByteBuffer;

import yoyo.common.face.IServiceModule;
import yoyo.queue.face.IAddQueueSink;

public interface ITCPClient extends IServiceModule {
	/**
	 * @param dwServiceID
	 * @return
	 */
	public boolean SetServiceID(int dwServiceID);
	/**
	 * 
	 * @param pIClientSocketAddQueueSink
	 * @return
	 */
	public boolean SetAddQueueSink(IAddQueueSink pIClientSocketAddQueueSink);
	/**
	 * 
	 * @return
	 */
	public boolean CloseSocket(byte cbShutReason,String msg);
	/**
	 * 
	 * @param dwServerIP
	 * @param dwPort
	 * @return
	 */
	public boolean Connect(int dwServerIP, int dwPort);
	/**
	 * 
	 * @param szServerIP
	 * @param dwPort
	 * @return
	 */
	public boolean Connect(String szServerIP,int dwPort);
	/**
	 * 
	 * @param wMainCmdID
	 * @param wSubCmd
	 * @return
	 */
	public boolean SendData(short wMainCmdID,short wSubCmd);
	/**
	 * 
	 * @param wMainCmdID
	 * @param wSubCmd
	 * @param pData
	 * @param wDataSize
	 * @return
	 */
	public boolean SendData(short wMainCmdID,short wSubCmd,ByteBuffer pBuffer,short wDataSize);
}

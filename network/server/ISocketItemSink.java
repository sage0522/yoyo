package yoyo.network.server;

import java.nio.ByteBuffer;

public interface ISocketItemSink {

	/**
	 * 
	 * @param pSocketItem
	 * @return
	 */
	public boolean SocketAcceptEvent(final JSocketItem pSocketItem);
	
	/**
	 * 
	 * @param Command
	 * @param pData
	 * @param wDataSize
	 * @param pSocketItem
	 * @return
	 */
	public boolean SocketReadEvent(short wMainCmdID ,short wSubCmdID ,ByteBuffer pBuffer,short wDataSize,final JSocketItem pSocketItem);
	
	/**
	 * 
	 * @param pSocketItem
	 * @return
	 */
	public boolean SocketCloseEvent(final JSocketItem pSocketItem);
}

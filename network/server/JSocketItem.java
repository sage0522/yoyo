package yoyo.network.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ConcurrentLinkedQueue;

import yoyo.Globals;
import yoyo.common.logger.JLogger;
import yoyo.common.utils.JUtils;
import yoyo.network.event.KernelCMD;
import yoyo.network.event.TCP_Head;

class JSocketItem implements CompletionHandler<AsynchronousSocketChannel, ISocketItemSink>{
	// 链接属性
	private int							m_dwClientAddr;				// 连接地址
	private int							m_dwClientPort;				// 连接端口
	private long						m_lConnectTime;				// 连接时间
	
	// 核心变量
	protected short						m_wRoundID;					// 循环计数(状态变化一次,该值增加一次,以保证该结构与上一次使用不同)
	protected AsynchronousSocketChannel	m_ClientSocketChannel;		// Socket异步链接通道
	
	// 内部变量
	private short						m_wIndex;					// 连接索引
	private ISocketItemSink				m_pISocketItemSink;			// 回调接口
	
	// 状态变量
	private boolean						m_bShutDown;				// 关闭标识
	private boolean						m_bAllowBatch;				// 是否群发
	private short						m_wRecvBufferSize;			// 接收缓冲数据大小
	private ByteBuffer					m_ReadBuffer;				// 数据读取缓冲区
	

	// 计数变量
	private long						m_lSendTickTime;			// 发送时间
	private long						m_lRecvTickTime;			// 接受时间
	private int							m_dwSendPacketCount;		// 发送计数
	private int							m_dwRecvPacketCount;		// 接受计数	

	private SendCompletionHandle		m_SendCompletionHandle;		//发送完成端口
	private ReadCompletionhandle		m_ReadCompletionHandle;		//读取完成端口
	
	/**
	 * 
	 */
	public JSocketItem(short wIndex)
	{
		// 设置回调接口
		ResetSocketItemData(wIndex);
	}
	public boolean ResetSocketItemData(short wIndex)
	{
		// 设置索引
		m_wIndex = wIndex;
		// 初始化数据
		m_wRoundID = JUtils._max((short) 1, m_wRoundID++);
		m_dwClientAddr = 0;
		m_dwClientPort = 0;
		m_lConnectTime = 0;

		m_bShutDown = false;
		m_bAllowBatch = false;

		m_wRecvBufferSize = 0;
		if (m_ReadBuffer == null) {
			m_ReadBuffer = ByteBuffer.allocate(Globals.SOCKET_TCP_BUFFER);
		}
		m_ReadBuffer.clear();
		m_lSendTickTime = 0;
		m_lRecvTickTime = 0;
		m_dwSendPacketCount = 0;
		m_dwRecvPacketCount = 0;
		m_pISocketItemSink = null;
		return true;
	}
	/**
	 * 
	 * @param wRoundID
	 * @return
	 */
	public boolean CloseSocket(int wRoundID)
	{
		if (wRoundID != m_wRoundID) return false;
		//先关闭连接
		m_ReadCompletionHandle = null;
		if(m_SendCompletionHandle!=null){
			m_SendCompletionHandle.CloseCompletionHandle();
			m_SendCompletionHandle = null;
		}
		if (m_ClientSocketChannel != null)
		{
			synchronized (m_ClientSocketChannel) {
				if (m_ClientSocketChannel.isOpen())
				{
					try
					{
						m_ClientSocketChannel.close();
					} catch (IOException e)
					{
						System.out.println("关闭Socket通道错误(" + wRoundID + "):");
						e.printStackTrace();
					}
				}
				m_ClientSocketChannel = null;
			}
		}
		// 通知
		return m_pISocketItemSink.SocketCloseEvent(this);
	}
	/**
	 * 
	 * @param wRoundID
	 * @return
	 */
	public boolean ShutDownSocket(short wRoundID)
	{
		if (wRoundID != m_wRoundID)
			return false;
		if (m_ClientSocketChannel == null)
			return false;
		m_bShutDown = true;
		return true;
	}
	/**
	 * 
	 * @param wRoundID
	 * @param bAllowBatch
	 * @return
	 */
	public boolean AllowBatchSend(short wRoundID, boolean bAllowBatch)
	{
		if (wRoundID != m_wRoundID)
			return false;
		if (m_ClientSocketChannel == null)
			return false;
		m_bAllowBatch = bAllowBatch;
		return true;
	}
	
	public boolean SendData(short wMainCmdID, short wSubCmdID,short wRoundID){
		
		return SendData(wMainCmdID,wSubCmdID,null,(short)0,wRoundID);
	}
	
	public boolean SendData(short wMainCmdID, short wSubCmdID,
			ByteBuffer pData, short wDataSize, short wRoundID){
		if (m_bShutDown == true) return false;
		if (m_wRoundID != wRoundID) return false;
		if (m_dwRecvPacketCount == 0) return false;
		if (IsValidSocket() == false) return false;
		if(wDataSize>0 && pData==null) {
			System.out.println("数据发送源错误(" + wMainCmdID + "|" + wSubCmdID+")");
			return false;
		}
		short wPacketSize = (short) (8 + wDataSize);
		
		// 当前通信版本号
		byte cbVersion = 0;
		// 数据加密后的校验码
		byte cbCheckCode = 0;
		// 数据压入发送缓存
		ByteBuffer sendBuffer = ByteBuffer.allocate(wPacketSize);
		// 数据压入发送缓存
		sendBuffer.put( cbVersion );
		sendBuffer.put( cbCheckCode );
		sendBuffer.putShort( wPacketSize );
		sendBuffer.putShort( wMainCmdID );
		sendBuffer.putShort( wSubCmdID );
		if(pData != null){
			pData.position(0);
			pData.limit(pData.capacity());
			sendBuffer.put(pData);
			pData.clear();
			pData = null;
		}
		m_SendCompletionHandle.AddSendData(sendBuffer);
		return true;
	}
	// ----------------------------------------------------------辅助函数
	/**
	 * 获取索引
	 */
	public short GetIndex() { return m_wIndex; }
	/**
	 * 获取循环计数
	 * @return
	 */
	public short GetRountID() { return m_wRoundID; }

	public int GetSocketID() { return ((m_wIndex & 0xffff) | (m_wRoundID & 0xffff) << 16); }
	/**
	 * 获取客户端IP地址
	 * @return
	 */
	public int GetClientAddr() { return m_dwClientAddr; }
	/**
	 * 获取客户端连接端口
	 * @return
	 */
	public int GetClientPort() { return m_dwClientPort; }
	/**
	 * 连接时长
	 * @return
	 */
	public long GetConnectDuration() { return System.currentTimeMillis() - m_lConnectTime; }
	/**
	 * 获取最后接收时间
	 * @return
	 */
	public long GetRecvTickTime() { return m_lRecvTickTime; }
	/**
	 * 获取最后发送时间
	 * @return
	 */
	public long GetSendTickTime() { return m_lSendTickTime; }
	/**
	 * 获取接收数据包数量
	 * @return
	 */
	public int GetRecvPackageCount() { return m_dwRecvPacketCount; }
	/**
	 * 获取发送数据包数量
	 * @return
	 */
	public int GetSendPackCount() { return m_dwSendPacketCount; }
	/**
	 * 是否可以群发
	 * @return
	 */
	public boolean IsAllowBatch() { return m_bAllowBatch; }
	/**
	 * 判断服务器是否可以主动发送数据
	 * @return
	 */
	public boolean IsReadySend() { return m_dwRecvPacketCount > 0; }
	/**
	 * 获取是否有效
	 * @return
	 */
	public boolean IsValidSocket()
	{
		if (m_ClientSocketChannel == null)
			return false;
		return m_ClientSocketChannel.isOpen();
	}
	//-------------------------------
	//--CompletionHandler
	//-------------------------------
	@Override
	public void completed(AsynchronousSocketChannel channel, ISocketItemSink sink) {
		// 用户连接时间
		m_lConnectTime = System.currentTimeMillis();
		// 第一次连接算是数据接收时间
		m_lRecvTickTime = m_lConnectTime;
		// 保留用户
		m_ClientSocketChannel = channel;
		
		m_pISocketItemSink = sink;
		try
		{
			InetSocketAddress inetAddress = (InetSocketAddress) m_ClientSocketChannel.getRemoteAddress();
			m_dwClientPort = inetAddress.getPort();
			m_dwClientAddr = JUtils._AddressToInt(inetAddress.getHostString());
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		m_pISocketItemSink.SocketAcceptEvent(this);
		
		//开始监听客户端发送的信息
		m_ReadBuffer.clear();
		m_ReadCompletionHandle = new ReadCompletionhandle();
		m_ClientSocketChannel.read(m_ReadBuffer, this, m_ReadCompletionHandle);
		//初始化发送数据
		m_SendCompletionHandle = new SendCompletionHandle(this);
	}
	@Override
	public void failed(Throwable result, ISocketItemSink sink) {
		
	}
	
	//------------------------
	//--自定义
	//------------------------
	
	protected void OnRecvCompleted(int wRecvSize)
	{
		if (wRecvSize <= 0 || wRecvSize >= Globals.SOCKET_TCP_BUFFER)
		{
			CloseSocket(m_wRoundID);
			return;
		}
		// 中断判断
		if (m_bShutDown == true) { return; }
		// 反转缓冲区 开始进行读取操作
		m_ReadBuffer.flip();
		// 累计接收数据缓存大小
		m_wRecvBufferSize += wRecvSize;
		// 记录最近一次接收客户端消息时间
		m_lRecvTickTime = System.currentTimeMillis();
		
		while (m_wRecvBufferSize >= TCP_Head.size)
		{
			byte cbVersion 			= m_ReadBuffer.get();
			//进行版本等校验
			if(cbVersion != 0){
				m_wRecvBufferSize = 0;
				JLogger.severe("数据包版本错误!");
				break;
			}
			byte cbCheckCode		= m_ReadBuffer.get();
			if(cbCheckCode!=0){
				m_wRecvBufferSize = 0;
				JLogger.severe("数据包校验码错误!");
				break;
			}
			short wPacketSize		= m_ReadBuffer.getShort();
			if(wPacketSize < TCP_Head.size){
				JLogger.severe("数据包长度太短(" + wPacketSize + ")!");
				CloseSocket(m_wRoundID);
				return;
			}
			if (m_wRecvBufferSize < wPacketSize)
			{
				m_ReadBuffer.position(m_ReadBuffer.position() - TCP_Head.size);
				m_ReadBuffer.limit( m_ReadBuffer.capacity() );
				break;
			}

			short wMainCmdID = m_ReadBuffer.getShort();
			short wSubCmdID = m_ReadBuffer.getShort();
			if (KernelCMD.MDM_KN_COMMAND == wMainCmdID) {
				// 处理内核命令
				switch(wSubCmdID)
				{
					case KernelCMD.SUB_KN_DETECT_SOCKET:
					{
						break;
					}
					default:
					{
						throw new IllegalArgumentException("非法命令码");
					}
				}
			} else {
				// 将数据拷贝到新的字节数组中
				short wDataSize = (short) (wPacketSize - TCP_Head.size);
				if (wDataSize > 0) {
					ByteBuffer recvBuffer = ByteBuffer.allocate(wDataSize);
					byte[] recvBytes = new byte[wDataSize];
					m_ReadBuffer = m_ReadBuffer.get(recvBytes, 0, wDataSize);
					recvBuffer.put(recvBytes);
					recvBuffer.flip();
					m_pISocketItemSink.SocketReadEvent(wMainCmdID ,wSubCmdID ,recvBuffer, wDataSize ,this);
				} else {
					m_pISocketItemSink.SocketReadEvent(wMainCmdID ,wSubCmdID ,null ,wDataSize ,this);
				}
			}
			
			// 设置变量
			m_dwRecvPacketCount++;
			m_wRecvBufferSize -= wPacketSize;
		}
		if (m_wRecvBufferSize > 0) {
			m_ReadBuffer.compact();
		} else {
			m_ReadBuffer.clear();
		}
	}
	
	public boolean OnSendCompleted()
	{
		return true;
	}
	//-----------------------
	//--读取完成端口
	//-----------------------
	class ReadCompletionhandle implements CompletionHandler<Integer, JSocketItem>{

		@Override
		public void completed(Integer result, JSocketItem attachment) {
			attachment.OnRecvCompleted(result);
			if( attachment.IsValidSocket() ){
				attachment.m_ClientSocketChannel.read(attachment.m_ReadBuffer, attachment, this);
			}
		}

		@Override
		public void failed(Throwable exc, JSocketItem attachment) {
			if( attachment.IsValidSocket() ){
				//JLogger.severe("ReadCompletionhandle:" + exc.getMessage());
				attachment.CloseSocket(attachment.GetRountID());
			}
		}
	}
	//-----------------------
	//--发送完成端口
	//-----------------------
	private class SendCompletionHandle implements CompletionHandler<Integer, ByteBuffer>{
		
		private JSocketItem m_pJSocketItem = null;
		private ConcurrentLinkedQueue<ByteBuffer> m_SendBufferQueue;	//发送数据队列
		private volatile boolean			m_bSendIng;					// 发送标志
		public SendCompletionHandle(JSocketItem pJSocketItem) {
			m_bSendIng = false;
			m_pJSocketItem = pJSocketItem;
			m_SendBufferQueue = new ConcurrentLinkedQueue<ByteBuffer>();
		}
		protected void AddSendData(ByteBuffer pBuffer){
			synchronized (m_SendBufferQueue) {
				if(m_bSendIng){
					m_SendBufferQueue.offer(pBuffer); 
				}else{
					m_bSendIng = true;
					pBuffer.flip();
					m_pJSocketItem.m_ClientSocketChannel.write(pBuffer, pBuffer, this);
				}
			}
		}
		protected void CloseCompletionHandle() {
			m_bSendIng = false;
			m_SendBufferQueue.clear();
			m_SendBufferQueue = null;
			m_pJSocketItem = null;
		}
		@Override
		public void completed(Integer result, ByteBuffer attachment) {
			if(attachment.hasRemaining()){
				System.out.println("SendCompletionHandle:"+attachment.position());
				m_pJSocketItem.m_ClientSocketChannel.write(attachment, attachment, this);
				
			}else{
				attachment.clear();
				attachment = null;
				ByteBuffer pBuffer = null;
				synchronized (m_SendBufferQueue) {
					pBuffer= m_SendBufferQueue.poll();
					if(pBuffer!=null){
						pBuffer.flip();
						m_pJSocketItem.m_ClientSocketChannel.write(pBuffer, pBuffer, this);
					}else{
						m_bSendIng = false;
					}
				}
			}
		}
		@Override
		public void failed(Throwable exc, ByteBuffer attachment) {
			if(m_pJSocketItem!=null && m_pJSocketItem.IsValidSocket() ){
				m_pJSocketItem.CloseSocket(m_pJSocketItem.GetRountID());
			}
			JLogger.severe("SendCompletionHandle:" + exc.getMessage());
		}
	}
	
}

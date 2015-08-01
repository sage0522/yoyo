package yoyo.network.client;

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
import yoyo.network.event.NTY_TCPClientConnectEvent;
import yoyo.network.event.NTY_TCPClientReadEvent;
import yoyo.network.event.NTY_TCPClientShutEvent;
import yoyo.network.event.TCP_Head;
import yoyo.queue.JQueueService;
import yoyo.queue.face.IAddQueueSink;
import yoyo.queue.face.IQueueServiceSink;


public class JTCPClient implements ITCPClient,IQueueServiceSink,CompletionHandler<Void, Void> {

	public static final int EVENT_CLIENT_TCP_READ 		= 0x0004; 	// 读取事件
	public static final int EVENT_CLIENT_TCP_SHUT 		= 0x0005; 	// 关闭事件
	public static final int EVENT_CLIENT_TCP_CONN 		= 0x0006; 	// 连接事件
	
	// 网络状态
	private static final byte SOCKET_STATUS_IDLE 		= 0; 		// 空闲状态
	private static final byte SOCKET_STATUS_WAIT 		= 1; 		// 等待状态
	private static final byte SOCKET_STATUS_CONNECT 	= 2; 		// 连接状态
	
	// 动作定义
	protected static final byte QUEUE_SEND_REQUEST 		= 1; 		// 发送标识
	protected static final byte QUEUE_CONNECT_REQUEST 	= 2; 		// 发送标识
	protected static final byte QUEUE_SAFE_CLOSE 		= 3; 		// 安全关闭
		
	// 关闭原因
	protected static final byte SHUT_REASON_INSIDE		 = 0; 		// 内部原因
	protected static final byte SHUT_REASON_TIME_OUT 	 = 1; 		// 网络超时
	protected static final byte SHUT_REASON_EXCEPTION 	 = 2; 		// 异常关闭
	
	private volatile boolean			m_bService;
	
	private IAddQueueSink				m_pAddQueueSink;
	private JQueueService				m_JQueueService;
	
	//连接变量
	private int							m_dwServiceID;
	private byte 						m_cbSocketStatus;
	private String						m_szServerIP;
	private int							m_dwPort;
	private AsynchronousSocketChannel	m_asynSocketChannel;
	
	// 计数变量
	public long 						m_lSendTickTime; 	// 发送时间
	public long 						m_lRecvTickTime;	 // 接收时间
	public int 							m_dwSendPacketCount; // 发送计数
	public int 							m_dwRecvPacketCount; // 接受计数
	

	//数据缓存
	private short 						m_wRecvDataSize; 	// 接收数据大小
	private ByteBuffer 					m_RecvByteBuffer; 	// 接收数据緩存
	private ReadCompletionHandle		m_ReadCompletionHandle;	//读取-完成端口
	private SendCompletionHandle		m_SendCompletionHandle;	//发送-完成端口
	
	public JTCPClient(){
		m_bService = false;
		
		m_dwServiceID 		= 0;
		m_cbSocketStatus 	= SOCKET_STATUS_IDLE;
		
		m_pAddQueueSink 	= null;
		m_JQueueService 	= null;
		
		m_asynSocketChannel	= null;
		
		m_wRecvDataSize		= 0;
		m_RecvByteBuffer	= ByteBuffer.allocateDirect(Globals.SOCKET_TCP_BUFFER);
		
		m_lSendTickTime = 0;
		m_lRecvTickTime = 0;
		m_dwSendPacketCount = 0;
		m_dwRecvPacketCount = 0;
	}
	//-------------------------------
	//--ITCPClientService
	//-------------------------------
	@Override
	public boolean StartService() {
		if(m_bService){
			JLogger.warning("TCP客户端服务已经启动,操作被忽略!");
			return false;
		}
		
		//启动队列服务
		m_JQueueService = new JQueueService();
		m_JQueueService.SetQueueServiceSink(this);
		if( m_JQueueService.StartService() == false ){
			JLogger.warning("TCP客户端-队列服务启动失败!");
			return false;
		}
		m_bService = true;
		return true;
	}

	@Override
	public boolean StopService() {
		if(!m_bService) return false;
		m_JQueueService.StopService();
		m_JQueueService = null;
		return true;
	}
	@Override
	public boolean SetServiceID(int dwServiceID) {
		m_dwServiceID = dwServiceID;
		return true;
	}

	@Override
	public boolean SetAddQueueSink(IAddQueueSink pIClientSocketAddQueueSink) {
		m_pAddQueueSink = pIClientSocketAddQueueSink;
		return true;
	}

	@Override
	public boolean CloseSocket(byte cbShutReason,String msg) {
		if(m_asynSocketChannel!=null){
			synchronized (m_asynSocketChannel) {
				if ( m_asynSocketChannel.isOpen()) {
					return m_JQueueService.AddToQueue(QUEUE_SAFE_CLOSE, cbShutReason);
				}
			}
			shutSocketChannel(cbShutReason);
		}
		return true;
	}

	@Override
	public boolean Connect(int dwServerIP, int dwPort) {
		return Connect(JUtils._InetNtoa(dwServerIP), dwPort);
	}

	@Override
	public boolean Connect(String szServerIP, int dwPort) {
		if (dwPort == 0 || szServerIP == null) {
			JLogger.severe("TCP客户端-连接失败!");
			return false;
		}
		m_szServerIP	= szServerIP;
		m_dwPort		= dwPort;
		return m_JQueueService.AddToQueue(QUEUE_CONNECT_REQUEST, null);
	}

	@Override
	public boolean SendData(short wMainCmdID, short wSubCmd) {
		return SendData(wMainCmdID,wSubCmd,null,(short)0);
	}

	@Override
	public boolean SendData(short wMainCmdID, short wSubCmd, ByteBuffer pBuffer, short wDataSize) {
		if (m_cbSocketStatus != SOCKET_STATUS_CONNECT) return false;
		return m_JQueueService.AddToQueue( QUEUE_SEND_REQUEST,getSendBuffer(wMainCmdID,wSubCmd,pBuffer,wDataSize) );
	}
	private ByteBuffer getSendBuffer(short wMainCmdID, short wSubCmd, ByteBuffer pBuffer, short wDataSize){
		if(wDataSize > 0 && pBuffer == null ) return null;
		short wPacketSize = (short) (8 + wDataSize);
		ByteBuffer sendBuffer = ByteBuffer.allocate(wPacketSize);
		//数据包版本号
		byte cbVersion = 0;
		//数据包校验
		byte cbCheckCode = 0;
		// 数据压入发送缓存
		sendBuffer.put(cbVersion);
		sendBuffer.put(cbCheckCode);
		sendBuffer.putShort(wPacketSize);
		sendBuffer.putShort(wMainCmdID);
		sendBuffer.putShort(wSubCmd);
		if(wDataSize>0){
			pBuffer.flip();
			sendBuffer.put(pBuffer);
		}
		return sendBuffer;
	}

	//-------------------------------
	//--IQueueServiceSink
	//-------------------------------
	@Override
	public boolean QueueServiceStartSink() {
		
		return true;
	}

	@Override
	public boolean QueueServiceStopSink() {
		shutSocketChannel(SHUT_REASON_INSIDE);
		return true;
	}

	@Override
	public void QueueServiceDataSink(int wIndentifier, Object pData) {
		switch(wIndentifier)
		{
			case QUEUE_SEND_REQUEST:
			{
				ByteBuffer pSendBuffer = (ByteBuffer)pData;
				if(m_SendCompletionHandle!=null){
					m_SendCompletionHandle.AddSendData(pSendBuffer);
				}
				break;
			}
			case QUEUE_CONNECT_REQUEST:
			{
				if(m_szServerIP == null) 
					throw new IllegalArgumentException("服务目标地址错误!");
				if(m_dwPort == 0) 
					throw new IllegalArgumentException("服务目标端口错误!");
				m_cbSocketStatus = SOCKET_STATUS_WAIT;
				try {
					m_asynSocketChannel = AsynchronousSocketChannel.open();
				} catch (IOException e) {
					JLogger.severe("TCP客户端-Socket通道创建失败:" + e.getMessage());
				}
				m_asynSocketChannel.connect(new InetSocketAddress(m_szServerIP, m_dwPort), null, this);
				break;
			}
			case QUEUE_SAFE_CLOSE:
			{
				shutSocketChannel((byte)pData);
				break;
			}
		}
	}
	
	//-------------------------------
	//--自定义
	//-------------------------------
	private void shutSocketChannel(byte cbShutReason){
		
		if(m_ReadCompletionHandle!=null){
			m_ReadCompletionHandle = null;
		}
		if(m_SendCompletionHandle!=null){
			m_SendCompletionHandle = null;
		}
		if(m_asynSocketChannel!=null){
			synchronized (m_asynSocketChannel) {
				if(m_asynSocketChannel.isOpen()){
					try {
						m_asynSocketChannel.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				m_asynSocketChannel = null;
			}
		}
		
		m_dwServiceID = 0;
		m_szServerIP = null;
		m_dwPort = 0;

		m_lSendTickTime = 0;
		m_lRecvTickTime = 0;
		m_dwSendPacketCount = 0;
		m_dwRecvPacketCount = 0;
		
		m_cbSocketStatus = SOCKET_STATUS_IDLE;
		NTY_TCPClientShutEvent shutEvent = new NTY_TCPClientShutEvent(m_dwServiceID,cbShutReason);
		m_pAddQueueSink.AddToQueueSink(EVENT_CLIENT_TCP_SHUT, shutEvent);
	}
	
	protected boolean OnRecvCompleted(int dwRecvSize){
		if (dwRecvSize <= 0 || dwRecvSize > Globals.SOCKET_TCP_BUFFER) return false;
		m_wRecvDataSize += dwRecvSize;
		m_lRecvTickTime = System.currentTimeMillis();
		m_RecvByteBuffer.flip();
		while (m_wRecvDataSize >= TCP_Head.size) {
			byte cbVersion 			= m_RecvByteBuffer.get();
			//进行版本等校验
			if(cbVersion != 0){
				CloseSocket(SHUT_REASON_INSIDE,"TCP客户端数据版本错误");
				return false;
			}
			byte cbCheckCode		= m_RecvByteBuffer.get();
			if(cbCheckCode!=0){
				CloseSocket(SHUT_REASON_INSIDE,"TCP客户端数据校验码错误");
				return false;
			}
			short wPacketSize		= m_RecvByteBuffer.getShort();
			// 校验数据包大小
			if(m_wRecvDataSize < wPacketSize){
				//******************************
				//**发生段包的是直接等待 还是需要从新read  需要进行测试
				//******************************
				m_RecvByteBuffer.position(m_RecvByteBuffer.position() - TCP_Head.size);
				m_RecvByteBuffer.limit(m_RecvByteBuffer.capacity());
				return false;
			}
			m_dwRecvPacketCount++;
			m_wRecvDataSize -= wPacketSize;
			short wMainCmdID = m_RecvByteBuffer.getShort();
			short wSubCmdID = m_RecvByteBuffer.getShort();
			if(wMainCmdID == KernelCMD.MDM_KN_COMMAND){
				switch (wSubCmdID)
				{
					case KernelCMD.SUB_KN_DETECT_SOCKET: // 网络检测
					{
						SendData(KernelCMD.MDM_KN_COMMAND, KernelCMD.SUB_KN_DETECT_SOCKET);
						break;
					}
				}
				continue;
			}
			NTY_TCPClientReadEvent readEvent = null;
			short wdataSize = (short) (wPacketSize-TCP_Head.size);
			if(wdataSize > 0){
				ByteBuffer recvBuffer = ByteBuffer.allocate( wdataSize );
				byte[] recvBytes = new byte[ wdataSize ];
				m_RecvByteBuffer.get(recvBytes, 0, wdataSize);
				recvBuffer.put(recvBytes);
				recvBuffer.flip();
				readEvent = new NTY_TCPClientReadEvent(m_dwServiceID, wMainCmdID, wSubCmdID, recvBuffer, wdataSize);
			}else{
				readEvent = new NTY_TCPClientReadEvent(m_dwServiceID, wMainCmdID, wSubCmdID, null, (short)0);
			}
			if( m_pAddQueueSink.AddToQueueSink(EVENT_CLIENT_TCP_READ,readEvent) == false){
				CloseSocket(SHUT_REASON_INSIDE,"TCP客户端数据处理错误:主命令码:"+readEvent.getMainCmdID()+",子命令码:"+readEvent.getSubCmdID());
				return false;
			}
		}
		//整理已读数据
		if (m_wRecvDataSize > 0) {
			m_RecvByteBuffer.compact();
		}else{
			m_RecvByteBuffer.clear();
		}
		return true;
	}
	protected boolean IsValidSocket() {
		if(m_asynSocketChannel==null){
			return true;
		}
		return m_asynSocketChannel.isOpen();
	}
	//---------------------------
	//--连接完成端口
	//---------------------------
	@Override
	public void completed(Void result, Void attachment) {
		//开始监听
		if(m_ReadCompletionHandle == null){
			m_ReadCompletionHandle = new ReadCompletionHandle();
			m_asynSocketChannel.read(m_RecvByteBuffer, this, m_ReadCompletionHandle);
		}
		if(m_SendCompletionHandle==null){
			//初始化发送数据
			m_SendCompletionHandle = new SendCompletionHandle(this);
		}
		m_cbSocketStatus = SOCKET_STATUS_CONNECT;
		NTY_TCPClientConnectEvent connEvent = new NTY_TCPClientConnectEvent(m_dwServiceID, 0);
		m_pAddQueueSink.AddToQueueSink(EVENT_CLIENT_TCP_CONN, connEvent);
	}
	@Override
	public void failed(Throwable exc, Void attachment) {
		NTY_TCPClientConnectEvent connEvent = new NTY_TCPClientConnectEvent(m_dwServiceID,1);
		m_pAddQueueSink.AddToQueueSink(EVENT_CLIENT_TCP_CONN, connEvent);
		CloseSocket(JTCPClient.SHUT_REASON_TIME_OUT,"TCP客户端连接错误:" + exc.getMessage().trim());
		//StopService();
	}
	
	//---------------------------
	//--发送完成端口
	//---------------------------
	class SendCompletionHandle implements CompletionHandler<Integer, ByteBuffer> {
		private JTCPClient m_pTcpClient = null;
		private ConcurrentLinkedQueue<ByteBuffer> m_SendBufferQueue;	//发送数据队列
		private volatile boolean			m_bSendIng;					// 发送标志
		
		public SendCompletionHandle(JTCPClient pTcpClient) {
			m_bSendIng = false;
			m_pTcpClient = pTcpClient;
			m_SendBufferQueue = new ConcurrentLinkedQueue<ByteBuffer>();
		}
		protected void AddSendData(ByteBuffer pBuffer){
			if(pBuffer==null) return;
			synchronized (m_SendBufferQueue) {
				if(m_bSendIng){
					m_SendBufferQueue.offer(pBuffer); 
				}else{
					m_bSendIng = true;
					pBuffer.flip();
					m_pTcpClient.m_asynSocketChannel.write(pBuffer, pBuffer, this);
				}
			}
		}
		protected void CloseCompletionHandle() {
			m_bSendIng = false;
			m_SendBufferQueue.clear();
			m_SendBufferQueue = null;
			m_pTcpClient = null;
		}
		@Override
		public void completed(Integer result, ByteBuffer attachment) {
			if(attachment.hasRemaining()){
				System.out.println("SendCompletionHandle:"+attachment.position());
				m_pTcpClient.m_asynSocketChannel.write(attachment, attachment, this);
				
			}else{
				attachment.clear();
				attachment = null;
				ByteBuffer pBuffer = null;
				synchronized (m_SendBufferQueue) {
					pBuffer= m_SendBufferQueue.poll();
					if(pBuffer!=null){
						pBuffer.flip();
						m_pTcpClient.m_asynSocketChannel.write(pBuffer, pBuffer, this);
					}else{
						m_bSendIng = false;
					}
				}
			}
		}

		@Override
		public void failed(Throwable exc, ByteBuffer attachment) {
			if(m_pTcpClient!=null && m_pTcpClient.IsValidSocket() ){
				m_pTcpClient.CloseSocket(SHUT_REASON_EXCEPTION,"TCP客户端数据发送错误:" + exc.getMessage());
			}
			JLogger.severe("SendCompletionHandle:" + exc.getMessage());
		}
	}
	//---------------------------
	//--读取数据端口
	//---------------------------
	private class ReadCompletionHandle implements CompletionHandler<Integer, JTCPClient> {

		public void completed(Integer result, JTCPClient attachment) {
			attachment.OnRecvCompleted(result);
			if( attachment.IsValidSocket() ){
				attachment.m_asynSocketChannel.read(attachment.m_RecvByteBuffer, attachment, this);
			}
		}
		public void failed(Throwable exc, JTCPClient attachment) {
			if(attachment.IsValidSocket()){
				attachment.CloseSocket(SHUT_REASON_EXCEPTION,"TCP客户端数据读取数据错误:" + exc.getMessage());
			}
		}
	}
}
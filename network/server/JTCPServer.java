package yoyo.network.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.Executors;

import yoyo.common.logger.JLogger;
import yoyo.common.thread.JThread;
import yoyo.common.utils.JUtils;
import yoyo.network.event.CMD_KN_DetectSocket;
import yoyo.network.event.KernelCMD;
import yoyo.network.event.NTY_TCPServerAcceptEvent;
import yoyo.network.event.NTY_TCPServerReadEvent;
import yoyo.network.event.NTY_TCPServerShutEvent;
import yoyo.queue.JQueueService;
import yoyo.queue.face.IAddQueueSink;
import yoyo.queue.face.IQueueServiceSink;
class JSocketDetectThread extends JThread{
	protected static final long			TIME_DETECT_SOCKET			= 15000;	// 监测时间
	protected long				m_lTimeCount;		// 用于累计倒计时
	protected JTCPServer		m_TCPServer; 		// TCP服务器管理指针
	
	public JSocketDetectThread()
	{
		super("SocketDetect-Thread",Thread.NORM_PRIORITY);
		m_lTimeCount = 0;
		m_TCPServer = null;
	}
	public boolean InitThread(JTCPServer pTCPServer)
	{
		m_lTimeCount = 0;
		m_TCPServer = pTCPServer;
		return true;
	}
	@Override
	protected boolean OnEventThreadStart(){
		return true;
	}
	@Override
	protected boolean OnEventThreadClose(){
		return true;
	}
	@Override
	public boolean OnEventThreadRun()
	{
		// 设置间隔
		sleep(500);
		m_lTimeCount += 500;

		// 每13s发送一次检测
		if (m_lTimeCount >= TIME_DETECT_SOCKET)
		{
			m_lTimeCount = 0;
			m_TCPServer.DetectSocket();
		}
		return true;
	}
	
}
public class JTCPServer implements ITCPServer,IQueueServiceSink,ISocketItemSink {

	public static final int					EVENT_TCP_NETWORK_ACCEPT	= 0x0007;	//应答事件
	public static final int					EVENT_TCP_NETWORK_READ		= 0x0008;	//读取事件
	public static final int					EVENT_TCP_NETWORK_SHUT		= 0x0009;	//关闭事件
	// 动作定义
	private static final byte				QUEUE_SEND_REQUEST			= 1;		// 发送标识
	private static final byte				QUEUE_SAFE_CLOSE			= 2;		// 安全关闭
	private static final byte				QUEUE_ALLOW_BATCH			= 3;		// 允许群发
	private static final byte				QUEUE_DETECT_SOCKET			= 4;		// 检测连接
	private static final byte				QUEUE_SHUTDOWN_REQUEST		= 5;		// 关闭连接

	// 时间定义
	protected static final long					TIME_BREAK_READY			= 15000;	// 中断时间
	protected static final long					TIME_BREAK_CONNECT			= 6000;		// 连接时间
	protected static final long					TIME_DETECT_SOCKET			= 15000;	// 监测时间
	
	private volatile boolean 				m_bService;

	private boolean							m_bDetectThread;
	private long							m_lLastDetect;							// 最后一次心跳检测时间
	private JSocketDetectThread				m_DetectThread;
	
	private JQueueService					m_JQueueService;
	

	// 配置变量
	private int								m_dwServicePort;						// 服务器端口
	private int								m_dwMaxSocketItem;						// 允许最大连接数目
	
	//网络变量
	private int								m_wThreadPoolSize;						// 线程数量
	private int								m_wbacklog;								// 最大等待链接数
	private AsynchronousChannelGroup		m_AsynChannelGroup;						//
	private AsynchronousServerSocketChannel	m_AsynServerSocketChannel;				// 异步Socket通道
	

	private short							m_minPerCapacity;						// 每次申请数组的大小
	private short							m_increaseCapacity;						// 记录数组递增值
	private ArrayList<JSocketItem>			m_StorageSocketItem;					// 存储连接
	private LinkedList<Short>				m_FreeSocketItemIndex;					// 空闲SocketItem索引
	private ArrayList<Short>				m_ActiveSocketItemIndex;				// 工作中SocketItem索引
		
	private IAddQueueSink					m_pIAddQueueSink;						//添加回调事件
	
	public JTCPServer(){
		m_bService 		= false;
		m_bDetectThread	= true;
		
		m_JQueueService = null;
		
		m_lLastDetect	= 0;
		
		m_dwServicePort = 0;
		m_dwMaxSocketItem = 512;
		
		m_wThreadPoolSize = 1;//Runtime.getRuntime().availableProcessors() * 2;
		m_wbacklog = 0;
		m_AsynChannelGroup = null;
		m_AsynServerSocketChannel = null;
		
		m_minPerCapacity = 256;
		m_increaseCapacity = m_minPerCapacity;
		m_StorageSocketItem = new ArrayList<JSocketItem>(m_minPerCapacity);
		
		m_FreeSocketItemIndex = new LinkedList<Short>();
		m_ActiveSocketItemIndex = new ArrayList<Short>(); 
	}
	/**
	 * 
	 * @return
	 */
	public int GetServicePort()
	{
		return m_dwServicePort;
	}
	//-------------------------
	//--ITCPServer
	//-------------------------
	@Override
	public boolean StartService() {
		if(m_bService){
			JLogger.warning("TCP服务器已经启动,操作被忽略!");
			return false;
		}
		
		m_JQueueService = new JQueueService();
		m_JQueueService.SetQueueServiceSink(this);
		
		if(m_JQueueService.StartService() == false){
			JLogger.warning("TCP服务-队列服务启动失败!");
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
		
		m_bService = false;
		return true;
	}
	@Override
	public void InitServerParameter(int dwPort ,int dwMaxSocketItem ,boolean bDetect){
		if(m_bService) {
			JLogger.warning("TCP服务器-配置要要在启动服务之前!");
			return;
		}
		m_dwServicePort = dwPort;
		m_dwMaxSocketItem = dwMaxSocketItem;
		m_bDetectThread = bDetect;
	}

	@Override
	public boolean SetAddQueueSink(IAddQueueSink pIAddQueueSink) {
		m_pIAddQueueSink = pIAddQueueSink;
		return m_pIAddQueueSink!=null;
	}

	@Override
	public boolean SendData(short wMainCmdID, short wSubCmdID, int dwSocketID) {
		return SendData(wMainCmdID,wSubCmdID,null,(short)0,dwSocketID);
	}
	@Override
	public boolean SendData(short wMainCmdID, short wSubCmdID,ByteBuffer pData, short wDataSize, int dwSocketID) {
		if (m_bService == false) return false;
		tagSendDataRequest sendData = new tagSendDataRequest(wMainCmdID, wSubCmdID, pData, wDataSize, dwSocketID);
		return m_JQueueService.AddToQueue(QUEUE_SEND_REQUEST, sendData);
	}
	@Override
	public boolean SendDataBatch(short wMainCmdID,short wSubCmdID,ByteBuffer pData,short wDataSize) {
		return SendData(wMainCmdID,wSubCmdID,pData,wDataSize,0);
	}
	@Override
	public boolean CloseSocket(int dwSocketID) {
		if (m_bService == false) return false;
		
		tagSafeCloseSocket closeSocket = new tagSafeCloseSocket(dwSocketID);
		return m_JQueueService.AddToQueue(QUEUE_SAFE_CLOSE, closeSocket);
	}
	@Override
	public boolean ShutDownSocket(int dwSocketID) {
		if (m_bService == false) return false;
		tagShutDownSocket shutDownSocket = new tagShutDownSocket(dwSocketID);
		return m_JQueueService.AddToQueue(QUEUE_SHUTDOWN_REQUEST, shutDownSocket);
	}
	@Override
	public boolean AllowBatchSend(int dwSocketID, boolean bAllowBatch) {
		if (m_bService == false) return false;
		tagAllowBatchSend allowBatchSend = new tagAllowBatchSend(dwSocketID, bAllowBatch);
		return m_JQueueService.AddToQueue(QUEUE_ALLOW_BATCH, allowBatchSend);
	}
	//-------------------------
	//--自定义
	//-------------------------
	protected void DetectSocket()
	{
		if(m_JQueueService==null) return;
		m_JQueueService.AddToQueue(QUEUE_DETECT_SOCKET, null);
	}

	//-------------------------
	//--IQueueServiceSink
	//-------------------------
	
	@Override
	public boolean QueueServiceStartSink() {
		
		//是否启动检测线程
		if(m_bDetectThread){
			m_DetectThread = new JSocketDetectThread();
			m_DetectThread.InitThread(this);
			if(m_DetectThread.StartThread()==false){
				JLogger.warning("TCP服务器-心跳检查线程启动失败!");
				return false;
			}
		}
		try {
			m_AsynChannelGroup = AsynchronousChannelGroup.withFixedThreadPool(m_wThreadPoolSize, Executors.defaultThreadFactory());
			m_AsynServerSocketChannel = AsynchronousServerSocketChannel.open(m_AsynChannelGroup);
			m_AsynServerSocketChannel.bind(new InetSocketAddress(m_dwServicePort), m_wbacklog);
			
		} catch (IOException e) {
			e.printStackTrace();
			JLogger.warning("TCP服务器-socket服务通道创建错误(" + e.getMessage() + ")");
			return false;
		}
		return waitAcceptSocketItem();
	}

	@Override
	public boolean QueueServiceStopSink() {

		m_lLastDetect = 0;
		if(m_DetectThread!=null){
			m_DetectThread.ChoseThread();
			m_DetectThread = null;
		}
		if(m_AsynServerSocketChannel!=null){
			try {
				m_AsynServerSocketChannel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			m_AsynServerSocketChannel = null;
		}
		m_FreeSocketItemIndex.addAll(m_ActiveSocketItemIndex);
		m_ActiveSocketItemIndex.clear();
		return true;
	}

	@Override
	public void QueueServiceDataSink(int wIndentifier, Object pData) {
		switch (wIndentifier)
		{
			case QUEUE_DETECT_SOCKET:
			{
				int wSocketItemSize = m_ActiveSocketItemIndex.size()-1;
				if(wSocketItemSize<=0) return;
				ArrayList<Short> tempList = new ArrayList<Short>();
				synchronized (m_ActiveSocketItemIndex)
				{
					tempList.addAll(m_ActiveSocketItemIndex);
				}
				CMD_KN_DetectSocket detectSocket = new CMD_KN_DetectSocket();
				short wRoundID = 0, wSocketIndex = 0;
				long lNowTickTime = System.currentTimeMillis();
				long ldwBreakTickTime = Math.max(lNowTickTime - m_lLastDetect, TIME_BREAK_READY);
				m_lLastDetect = lNowTickTime;
				for (int i = 0; i < wSocketItemSize; i++)
				{
					wSocketIndex = tempList.get(i);
					JSocketItem pSocketItem = m_StorageSocketItem.get(wSocketIndex);
					if (pSocketItem == null || (pSocketItem.IsValidSocket() == false)) continue;
					
					wRoundID = pSocketItem.GetRountID();
					long dwRecvTickTime = pSocketItem.GetRecvTickTime();
					// ---------------------------------------------------------------------------
					// 应当再修改一下 , 如果最后一次接收的数据不超过ldwBreakTickTime的话 则不发送数据
					// if( (lNowTickTime - dwRecvTickTime)<ldwBreakTickTime)
					// continue;
					if (dwRecvTickTime >= lNowTickTime) continue;
					
					// 检测连接
					if (pSocketItem.IsReadySend()){
						if ((lNowTickTime - dwRecvTickTime) > ldwBreakTickTime) {
							pSocketItem.CloseSocket(wRoundID);
							continue;
						}else{
							// 发送检测连接数据
							detectSocket.lRecvTime = dwRecvTickTime;
							detectSocket.lSendTime = System.currentTimeMillis();
							pSocketItem.SendData(KernelCMD.MDM_KN_COMMAND, KernelCMD.SUB_KN_DETECT_SOCKET, detectSocket.ToByteBuffer(), detectSocket.size, wRoundID);
						}
					} else {
						if ((lNowTickTime - dwRecvTickTime) > TIME_BREAK_CONNECT) {
							pSocketItem.CloseSocket(wRoundID);
							continue;
						}
					}
				}
				break;
			}
			case QUEUE_SEND_REQUEST:
			{
				tagSendDataRequest sendRequest = (tagSendDataRequest) pData;
				if (sendRequest == null) return;
				if (sendRequest.m_dwSocketID == 0) // 群发
				{
					ArrayList<Short> tempList = new ArrayList<Short>();
					
					synchronized(m_ActiveSocketItemIndex){
						tempList.addAll(m_ActiveSocketItemIndex);
					}
					JSocketItem pSocketItem = null;
					short wIndex = 0;
					for (int i = 0; i < tempList.size(); i++)
					{
						wIndex = tempList.get(i);
						pSocketItem = m_StorageSocketItem.get(wIndex);
						if (pSocketItem == null)
						{
							JLogger.severe("发送数据时,激活的ServerSoketItem为空!");
							continue;
						}
						if (pSocketItem.IsAllowBatch())
						{
							pSocketItem.SendData(sendRequest.m_wMainCmdID, sendRequest.m_wSubCmdID,
												sendRequest.m_pDataBuf, sendRequest.m_wDataSize, 
												pSocketItem.GetRountID());
						}
					}
				}else{
					JSocketItem pSocketItem = enumSocketItem(JUtils._loword(sendRequest.m_dwSocketID));
					if(pSocketItem!=null){
						pSocketItem.SendData(sendRequest.m_wMainCmdID, sendRequest.m_wSubCmdID, sendRequest.m_pDataBuf,
											sendRequest.m_wDataSize, pSocketItem.GetRountID());
					}
				}
				break;
			}
			case QUEUE_ALLOW_BATCH:
			{
				tagAllowBatchSend AllowBatchSend = (tagAllowBatchSend) pData;
				if (AllowBatchSend == null)
					return;
				JSocketItem pSocketItem = enumSocketItem(JUtils._loword(AllowBatchSend.m_dwSocketID));
				if (pSocketItem != null)
				{
					// -------------------------------------------
					pSocketItem.AllowBatchSend(JUtils._hiword(AllowBatchSend.m_dwSocketID), AllowBatchSend.m_cbAllow);
				}
				break;
			}
			case QUEUE_SHUTDOWN_REQUEST:
			{
				tagShutDownSocket shutDownEvent = (tagShutDownSocket) pData;
				if (shutDownEvent == null) return;
				JSocketItem pSocketItem = enumSocketItem(JUtils._loword(shutDownEvent.m_dwSocketID));
				if (pSocketItem == null) return;
				// --------------------------------------------
				pSocketItem.ShutDownSocket(JUtils._hiword(shutDownEvent.m_dwSocketID));
				break;
			}
			case QUEUE_SAFE_CLOSE:
			{
				tagSafeCloseSocket closeEvent = (tagSafeCloseSocket) pData;
				if (closeEvent == null) return;
				JSocketItem pSocketItem = enumSocketItem(JUtils._loword(closeEvent.m_dwSocketID));
				if (pSocketItem == null) return;
				// --------------------------------------------
				pSocketItem.CloseSocket(JUtils._hiword(closeEvent.m_dwSocketID));
				break;
			}
		}
	}
	
	
	//-------------------------------
	//--自定义
	//-------------------------------
	private boolean waitAcceptSocketItem()
	{
		// 获取空闲SocketItem
		JSocketItem pWaitSocketItem = activeSocketItem();
		if (pWaitSocketItem != null)
		{
			m_AsynServerSocketChannel.accept(this, pWaitSocketItem);
			return true;
		}
		return false;
	}
	private JSocketItem activeSocketItem()
	{
		JSocketItem activeSocketItem = null;
		short activeSocketItemIndex = -1;
		synchronized (m_FreeSocketItemIndex) {
			if(m_FreeSocketItemIndex.size() > 0){
				activeSocketItemIndex = m_FreeSocketItemIndex.pop();
				activeSocketItem = m_StorageSocketItem.get(activeSocketItemIndex);
			}
		}
		if(activeSocketItem == null){
			synchronized (m_StorageSocketItem) {
				
				activeSocketItemIndex = (short) m_StorageSocketItem.size();
				if (activeSocketItemIndex >= m_increaseCapacity)
				{
					m_StorageSocketItem.ensureCapacity(m_minPerCapacity); // 可以提高插入效率
					m_increaseCapacity += activeSocketItemIndex;
				}
				activeSocketItem = new JSocketItem(activeSocketItemIndex);
				m_StorageSocketItem.add(activeSocketItem);
			}
		}
		synchronized (m_ActiveSocketItemIndex) {
			// 如果连接超过最大值 发出警告
			if (m_ActiveSocketItemIndex.size() >= m_dwMaxSocketItem)
			{
				JLogger.severe("当前连接数量:" + m_ActiveSocketItemIndex.size()+",最大连接数量:"+m_dwMaxSocketItem);
			}
			m_ActiveSocketItemIndex.add(activeSocketItemIndex);
		}
		return activeSocketItem;
	}
	private boolean freeSocketItem(JSocketItem pServerSocketItem)
	{
		if (pServerSocketItem == null)
		{
			JLogger.severe("释放SocketItem为空!");
			return false;
		}
		short wSocketIndex = pServerSocketItem.GetIndex();
		synchronized (m_ActiveSocketItemIndex) {
			int index = m_ActiveSocketItemIndex.indexOf(wSocketIndex);
			if(index!=-1){
				m_ActiveSocketItemIndex.remove(index);
			}
		}
		synchronized(m_FreeSocketItemIndex){
			m_FreeSocketItemIndex.add(wSocketIndex);
		}
		return  pServerSocketItem.ResetSocketItemData(wSocketIndex);
	}
	private JSocketItem enumSocketItem(short wIndexID)
	{
		synchronized (m_StorageSocketItem) {
			if (wIndexID < m_StorageSocketItem.size())
			{
				return m_StorageSocketItem.get(wIndexID);
			}
		}
		return null;
	}
	//-------------------------------
	//--ISocketItemSink
	//-------------------------------
	@Override
	public boolean SocketAcceptEvent(JSocketItem pSocketItem) {
		// 从新等待链接客户端
		waitAcceptSocketItem();
		if (pSocketItem == null) {
			JLogger.severe("应答消息事件 pSocketItem 为空!");
			return false;
		}
		// 通知TCP服务事件
		NTY_TCPServerAcceptEvent acceptEvent = new NTY_TCPServerAcceptEvent(pSocketItem.GetClientAddr(),
																			pSocketItem.GetSocketID());
		return m_pIAddQueueSink.AddToQueueSink(EVENT_TCP_NETWORK_ACCEPT, acceptEvent);
	}
	@Override
	public boolean SocketReadEvent(short wMainCmdID, short wSubCmdID,
			ByteBuffer pBuffer, short wDataSize, JSocketItem pSocketItem) {
		if (pSocketItem == null) {
			JLogger.severe("读取消息 pSocketItem 为空!");
			return false;
		}
		int dwSocketID = pSocketItem.GetSocketID();
		NTY_TCPServerReadEvent readEvent = new NTY_TCPServerReadEvent(dwSocketID, wMainCmdID, wSubCmdID, pBuffer, wDataSize);
		return m_pIAddQueueSink.AddToQueueSink(EVENT_TCP_NETWORK_READ, readEvent);
	}
	@Override
	public boolean SocketCloseEvent(JSocketItem pSocketItem) {
		if (pSocketItem == null) {
			JLogger.severe("关闭消息 pSocketItem 为空!");
			return false;
		}
		int dwSocketID = pSocketItem.GetSocketID();
		int dwClientAddr = pSocketItem.GetClientAddr();
		long lActiveTime = pSocketItem.GetConnectDuration();
		// ----------------------------------------------------------------------------------
		// 需要注意的是 这个地方会不会导致 队列到达后调用关闭时,dwSocketID的对象已经被 freeSocketItem
		NTY_TCPServerShutEvent shutEvent = new NTY_TCPServerShutEvent(dwClientAddr, lActiveTime, dwSocketID);
		m_pIAddQueueSink.AddToQueueSink(EVENT_TCP_NETWORK_SHUT,shutEvent);
		// ----------------------------------------------------------------------------------
		return freeSocketItem(pSocketItem);
	}
	
	
	//-------------------------
	//--自定义内部类
	//------------------------
	class tagSendDataRequest
	{
		protected short			m_wMainCmdID;	// 主命令码
		protected short			m_wSubCmdID;	// 子命令码
		protected int			m_dwSocketID;	// 连接索引
		protected short			m_wDataSize;	// 数据大小
		protected ByteBuffer	m_pDataBuf;	// 携带数据

		public tagSendDataRequest(short wMainCmdID, short wSubCmdID,
				ByteBuffer pData, short wDataSize, int dwSocketID)
		{
			m_wMainCmdID = wMainCmdID;
			m_wSubCmdID = wSubCmdID;
			m_pDataBuf = pData;
			m_wDataSize = wDataSize;
			m_dwSocketID = dwSocketID;
		}
	}
	class tagSafeCloseSocket
	{
		protected int	m_dwSocketID;	// 连接索引
		public tagSafeCloseSocket(int dwSocketID)
		{
			m_dwSocketID = dwSocketID;
		}
	}
	class tagShutDownSocket
	{
		protected int	m_dwSocketID;	// 连接索引
		public tagShutDownSocket(int dwSocketID)
		{
			m_dwSocketID = dwSocketID;
		}
	}
	class tagAllowBatchSend
	{
		protected int		m_dwSocketID;	// 连接索引
		protected boolean	m_cbAllow;		// 允许标识
		public tagAllowBatchSend(int dwSocketID, boolean cbAllow)
		{
			m_dwSocketID = dwSocketID;
			m_cbAllow = cbAllow;
		}
	}
}

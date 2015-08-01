package yoyo.attemper;

import java.nio.ByteBuffer;

import yoyo.attemper.face.IAttemplerService;
import yoyo.common.logger.JLogger;
import yoyo.database.JDBSink;
import yoyo.database.event.NTY_DBEvent;
import yoyo.network.client.JTCPClient;
import yoyo.network.event.NTY_TCPClientConnectEvent;
import yoyo.network.event.NTY_TCPClientReadEvent;
import yoyo.network.event.NTY_TCPClientShutEvent;
import yoyo.network.event.NTY_TCPServerAcceptEvent;
import yoyo.network.event.NTY_TCPServerReadEvent;
import yoyo.network.event.NTY_TCPServerShutEvent;
import yoyo.network.server.JTCPServer;
import yoyo.queue.JQueueService;
import yoyo.queue.face.IAddQueueSink;
import yoyo.queue.face.IQueueServiceSink;
import yoyo.timer.JTimerService;
import yoyo.timer.event.NTY_TimerEvent;

public abstract class JAttemperService implements IAttemplerService,IAddQueueSink,IQueueServiceSink{

	protected volatile boolean		m_bService;
	protected JQueueService			m_JQueueService;
	
	
	public JAttemperService(){
		m_bService 		= false;
		m_JQueueService = null;
	}
	
	//-------------------------------
	//--IAttemplerEngine
	//-------------------------------
	@Override
	public boolean StartService() {
		
		if(m_bService){
			JLogger.warning("调度服务已经启动,操作被忽略!");
			return false;
		}
		//设置统一队列服务
		m_JQueueService = new JQueueService();
		m_JQueueService.SetQueueServiceSink(this);
		
		if(m_JQueueService.StartService() == false){
			JLogger.severe("调度-队列服务启动失败!");
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

	//-------------------------------
	//--IAddQueueSink
	//-------------------------------
	@Override
	public boolean AddToQueueSink(int dwIdentifier, Object pData) {
		if(!m_bService) return false;
		return m_JQueueService.AddToQueue(dwIdentifier, pData);
	}

	//-------------------------------
	//--IQueueServiceSink
	//-------------------------------
	@Override
	public abstract boolean QueueServiceStartSink();

	@Override
	public abstract boolean QueueServiceStopSink();

	@Override
	public void QueueServiceDataSink(int wIndentifier, Object pData) {
		switch(wIndentifier)
		{
			case JTimerService.EVENT_TIMER:
			{
				NTY_TimerEvent timerEvent = (NTY_TimerEvent)pData;
				if(OnEventTimer(timerEvent.getTimerID(),timerEvent.getLeftRepeat(),timerEvent.getBindParamter()) == false)
				{
					JLogger.severe("计时器事件处理错误:" + timerEvent.getTimerID());
				}
				break;
			}
			case JDBSink.EVENT_DATABASE:
			{
				NTY_DBEvent dbEvent = (NTY_DBEvent)pData;
				if(OnEventDataBase(dbEvent.getRequestID(),dbEvent.getSocketID(),dbEvent.getData())==false)
				{
					JLogger.severe("数据库结果处理错误:" + dbEvent.getRequestID());
				}
				break;
			}
			case JTCPServer.EVENT_TCP_NETWORK_ACCEPT:
			{
				NTY_TCPServerAcceptEvent acceptEvent = (NTY_TCPServerAcceptEvent)pData;
				if(OnServerAcceptEvent(acceptEvent.getSocketID(),acceptEvent.getClientAddr()) == false){
					JLogger.severe("TCP服务器连接处理错误:" + acceptEvent.getClientAddr());
				}
				break;
			}
			case JTCPServer.EVENT_TCP_NETWORK_READ:
			{
				NTY_TCPServerReadEvent readEvent = (NTY_TCPServerReadEvent)pData;
				if(OnServerReadEvent(readEvent.getSocketID(), readEvent.getMainCmdID(), readEvent.getSubCmdID(),
						readEvent.getBuffer(), readEvent.getDataSize()) == false){
					JLogger.severe("TCP服务器读取错误:" + readEvent.getMainCmdID() + "-" +readEvent.getSubCmdID());
				}
				break;
			}
			case JTCPServer.EVENT_TCP_NETWORK_SHUT:
			{
				NTY_TCPServerShutEvent shutEvent = (NTY_TCPServerShutEvent)pData;
				if(OnServerShutEvent(shutEvent.getSocketID(), shutEvent.getClientAddr(), shutEvent.getActiveTime())==false){
					JLogger.severe("TCP服务器关闭错误:"+shutEvent.getClientAddr());
				}
				break;
			}
			case JTCPClient.EVENT_CLIENT_TCP_CONN:
			{
				NTY_TCPClientConnectEvent connectEvent = (NTY_TCPClientConnectEvent)pData;
				if(OnClientConnectEvent(connectEvent.getServiceID(), connectEvent.getErrorCode())==false){
					JLogger.severe("TCP客户端连接错误:"+connectEvent.getServiceID());
				}
				break;
			}
			case JTCPClient.EVENT_CLIENT_TCP_READ:
			{
				NTY_TCPClientReadEvent clientReadEvent = (NTY_TCPClientReadEvent)pData;
				if(OnClientReadEvent(clientReadEvent.getServiceID(), 
									clientReadEvent.getMainCmdID(), clientReadEvent.getSubCmdID(), 
									clientReadEvent.getBuffer(), clientReadEvent.getDataSize())==false){
					JLogger.severe("TCP客户端读取错误:"+clientReadEvent.getMainCmdID()+"-"+clientReadEvent.getSubCmdID());
				}
				break;
			}
			case JTCPClient.EVENT_CLIENT_TCP_SHUT:
			{
				NTY_TCPClientShutEvent clientShutEvent = (NTY_TCPClientShutEvent)pData;
				if(OnClientShutEvent(clientShutEvent.getServiceID(), clientShutEvent.getShutReason())==false){
					JLogger.severe("TCP客户端关闭错误:"+clientShutEvent.getServiceID());
				}
				break;
			}
		}
	}

	@Override
	public boolean OnEventTimer(int dwTimerID, int dwLeftRepeat, Object bindParameter){
		return false;
	}
	@Override
	public boolean OnEventDataBase(int dwRequestID, int dwSocketID, Object pData){
		return false;
	}
	@Override
	public boolean OnServerAcceptEvent(int dwSocketID,int dwClientAddr){
		return false;
	}
	@Override
	public boolean OnServerReadEvent(int dwSocketID,short wMainCmdID,short wSubCmdID,ByteBuffer pBuffer,short wDataSize){
		return false;
	}
	@Override
	public boolean OnServerShutEvent(int dwSocketID,int dwClientAddr,long lActiveTime){
		return false;
	}
	@Override
	public boolean OnClientConnectEvent(int serviceID ,int errorCode){
		return false;
	}
	@Override
	public boolean OnClientReadEvent(int dwServiceID,short wMainCmdID ,short wSubCmdID ,ByteBuffer pBuffer,short wDataSize){
		return false;
	}
	@Override
	public boolean OnClientShutEvent(int serviceID ,byte cbShutReason){
		return false;
	}
}

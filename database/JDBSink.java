package yoyo.database;

import yoyo.common.logger.JLogger;
import yoyo.common.thread.JThread;
import yoyo.database.event.NTY_DBEvent;
import yoyo.database.face.IDBService;
import yoyo.database.face.IDBSink;
import yoyo.queue.face.IAddQueueSink;
public abstract class JDBSink extends JThread implements IDBSink {

	public static final int EVENT_DATABASE 			= 0x0003;
	
	protected volatile boolean 		m_bService;
	protected volatile boolean		m_bExcuting;
	protected IAddQueueSink			m_pDBAddQueueSink;
	
	
	private IDBService				m_pIDBService;
	private static int index = 0;
	public JDBSink(){
		super("JDBSink" + (index++),Thread.NORM_PRIORITY);
		m_bService 			= false;
		m_bExcuting 		= false;
		m_pIDBService		= null;
		m_pDBAddQueueSink 	= null;
	}
	
	@Override
	public boolean isExcuting() {
		return m_bExcuting;
	}

	@Override
	public boolean DBEngineStart() {
		if(m_pDBAddQueueSink == null){
			JLogger.severe("数据库的 添加队列钩子(m_pDBAddQueueSink)不存在!");
			return false;
		}
		if(m_pIDBService == null){
			JLogger.severe("数据库服务(m_pIDBService)不存在!");
			return false;
		}
		if( this.StartThread()==false ){
			JLogger.severe("数据库线程启动失败!");
			return false;
		}
		return true;
	}

	@Override
	public boolean DBEngineStop() {
		
		this.ChoseThread();
		m_pDBAddQueueSink = null;
		m_pIDBService = null;
		return true;
	}

	@Override
	public abstract boolean DBEngineRequest(int dwRequestID, int dwSocketID, Object pData);

	@Override
	public boolean SetAddQueueSink(IAddQueueSink pIDBAddQueueSink) {
		m_pDBAddQueueSink = pIDBAddQueueSink;
		return true;
	}
	
	@Override
	public boolean SetIDBService(IDBService pDBService) {
		m_pIDBService = pDBService;
		return m_pIDBService != null;
	}
	
	protected void AddResultToQueue(int requestID ,int socketID ,Object pData){
		NTY_DBEvent dbEvent = new NTY_DBEvent(requestID, socketID, pData);
		m_pDBAddQueueSink.AddToQueueSink(EVENT_DATABASE, dbEvent);
	}
	//--------------------------
	//--Thread
	//--------------------------
	@Override
	protected boolean OnEventThreadStart(){
		return true;
	}
	@Override
	protected boolean OnEventThreadClose(){
		return true;
	}
	@Override
	protected boolean OnEventThreadRun(){
		NTY_DBEvent dbEvent = m_pIDBService.GetDBEvent();
		if(dbEvent!=null){
			if( this.DBEngineRequest(dbEvent.getRequestID(),dbEvent.getSocketID(),dbEvent.getData()) == false)
			{
				JLogger.severe("数据库执行错误:" + dbEvent.getRequestID());
				return true;
			}
		}
		return true;
	}
	
}

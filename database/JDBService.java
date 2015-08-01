package yoyo.database;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import yoyo.common.logger.JLogger;
import yoyo.database.event.NTY_DBEvent;
import yoyo.database.face.IDBService;
import yoyo.database.face.IDBSink;

public class JDBService implements IDBService {

	private volatile boolean 					m_bService;
	
	private LinkedBlockingQueue<NTY_DBEvent>	m_DBEventQueue;
	private ArrayList<IDBSink>					m_pDBEngineSinks;
	
	public JDBService(){
		m_bService = false;
		m_DBEventQueue = new LinkedBlockingQueue<NTY_DBEvent>();
		m_pDBEngineSinks = new ArrayList<IDBSink>();
	}
	@Override
	public boolean StartService() {
		if(m_bService) {
			JLogger.warning("数据库引擎已启动,操作被忽略!");
			return true;
		}
		
		if(m_pDBEngineSinks.size() == 0){
			JLogger.severe("数据库处理钩子不存在!");
			return false;
		}
		
		for(int i = 0;i<m_pDBEngineSinks.size();i++)
		{
			if( m_pDBEngineSinks.get(i).DBEngineStart() == false ){
				JLogger.severe("数据库处理钩子启动失败!");
				return false;
			}
		}
		
		m_bService = true;
		return true;
	}

	@Override
	public boolean StopService() {
		if(!m_bService) return true;
		
		for(int i = 0;i<m_pDBEngineSinks.size();i++)
		{
			if( m_pDBEngineSinks.get(i).DBEngineStop() == false ){
				JLogger.severe("停止数据库处理钩子时错误!");
				return false;
			}
		}
		m_pDBEngineSinks.clear();
		m_bService = false;
		return true;
	}
	@Override
	public boolean  AddDBSink(IDBSink pDBSink){
		if(m_bService || pDBSink== null) return false;
		pDBSink.SetIDBService( this );
		m_pDBEngineSinks.add(pDBSink);
		return true;
	}

	@Override
	public boolean PostDBRequest(int dwRequestID, int dwSocketID, Object pData) {
		if(!m_bService) {
			JLogger.severe("数据库服务器尚未启动成功,发送请求失败!");
			return false;
		}
		NTY_DBEvent dbEvent = new NTY_DBEvent(dwRequestID, dwSocketID, pData);
		try {
			m_DBEventQueue.put(dbEvent);
		} catch (InterruptedException e) {
			JLogger.severe("数据库请求添加失败["+dwRequestID+"]:" + e.getMessage());
		}
		return true;
	}
	@Override
	public NTY_DBEvent GetDBEvent() {
		NTY_DBEvent dbEvent = null;
		try {
			dbEvent = m_DBEventQueue.take();
		} catch (InterruptedException e) {
			if(m_bService){
				JLogger.severe("数据库获取请求失败:" + e.getMessage());
			}
		}
		return dbEvent;
	}

}

package yoyo.database.face;

import yoyo.common.face.IServiceModule;
import yoyo.database.event.NTY_DBEvent;

public interface IDBService extends IServiceModule {
	
	/**
	 * 
	 * @param pDBSink
	 * @return
	 */
	boolean AddDBSink(IDBSink pDBSink);
	
	/**
	 * 
	 * @return
	 */
	boolean PostDBRequest(int dwRequestID,int dwSocketID,Object pData);
	
	NTY_DBEvent GetDBEvent();
}

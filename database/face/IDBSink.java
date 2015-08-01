package yoyo.database.face;

import yoyo.queue.face.IAddQueueSink;

public interface IDBSink {
	/**
	 * 
	 * @return
	 */
	boolean isExcuting();
	/**
	 * 
	 * @return
	 */
	boolean DBEngineStart();
	
	/**
	 * 
	 * @return
	 */
	boolean DBEngineStop();
	
	/**
	 * request data base
	 * 
	 * @param dwRequestID
	 * @param dwSocketID
	 * @param pData
	 * @return
	 */
	boolean DBEngineRequest(int dwRequestID,int dwSocketID,Object pData);
	/**
	 * 
	 * @param pIDBAddQueueSink
	 * @return
	 */
	boolean SetAddQueueSink(IAddQueueSink pIDBAddQueueSink);
	
	boolean SetIDBService(IDBService pDBService);
}

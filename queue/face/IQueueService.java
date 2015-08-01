package yoyo.queue.face;

import yoyo.common.face.IServiceModule;

/**
 * Queue Service
 * @author tp
 */
public interface IQueueService extends IServiceModule {
	
	/**
	 * Set queue service sink
	 * @param pIQueueServiceSink
	 * @return
	 */
	public boolean SetQueueServiceSink(IQueueServiceSink pIQueueServiceSink);
	
	/**
	 * Add data to queue 
	 * @param dwIdentifier
	 * @param pData
	 * @return
	 */
	public boolean AddToQueue(int dwIdentifier,Object pData);
}

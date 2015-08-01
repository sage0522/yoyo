package yoyo.queue.face;
/**
 * 
 * Queue Service Sink
 * @author tiant_000
 *
 */
public interface IQueueServiceSink {
	/**
	 * sink Service Start 
	 * @return
	 */
	public boolean QueueServiceStartSink();
	/**
	 * sink Service Stop
	 * @return
	 */
	public boolean QueueServiceStopSink();
	/**
	 * sink queue date handle
	 * @param wIndentifier
	 * @param pData
	 */
	public void QueueServiceDataSink(int wIndentifier,Object pData);
}

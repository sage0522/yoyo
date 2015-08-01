package yoyo.queue.face;

public interface IAddQueueSink {
	/**
	 * 
	 * @param dwIdentifier
	 * @param pData
	 * @return
	 */
	boolean AddToQueueSink(int dwIdentifier, Object pData);
}

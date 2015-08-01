package yoyo.timer.face;

import yoyo.common.face.IServiceModule;
import yoyo.queue.face.IAddQueueSink;

/**
* Timer Service
**/
public interface ITimerService extends IServiceModule {
	/**
	 *  Set Timer Queue
	 * @param pITimerAddQueueSink
	 * @return
	 */
	public boolean SetAddQueueSink(IAddQueueSink pITimerAddQueueSink);
	
	/**
	 * Set Timer
	 * @param dwTimerID
	 * @param lElapse
	 * @param wRepeat
	 * @param bindParamter
	 * @return
	 */
	public boolean SetTimer(int dwTimerID,long lElapse,int dwRepeat, Object bindParamter);
	
	/**
	 * Kill Timer
	 * @param dwTimerID
	 * @return
	 */
	public boolean KillTimer(int dwTimerID);

	/**
	 * Kill All Timer
	 * @return
	 */
	public boolean KillAllTimer();
}

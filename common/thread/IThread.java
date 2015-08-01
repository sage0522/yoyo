package yoyo.common.thread;
/**
 *  IThread 
 * @author tp
 */
public interface IThread {
		
	/**
	 *  Whether the current thread running
	 * @return
	 */
	public boolean IsRunning();
	
	/**
	 * Start current thread
	 * @return
	 */
	public  boolean StartThread();
	
	/**
	 * Close current thread
	 * @see ChoseThread(long lMillSeconds)
	 * @return
	 */
	public boolean ChoseThread();
	
	/**
	 * sleep current thread
	 * @param timeout
	 */
	public void sleep(long timeout);
}

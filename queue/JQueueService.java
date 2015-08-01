package yoyo.queue;

import java.util.concurrent.LinkedBlockingQueue;

import yoyo.common.logger.JLogger;
import yoyo.common.thread.JThread;
import yoyo.queue.face.IQueueService;
import yoyo.queue.face.IQueueServiceSink;

/**
 * Queue Data
 * 
 * @author tp
 *
 */
class JQueueData {

	protected int dwIdentifier;
	protected Object pData;

	public JQueueData(int identifier, Object data) {
		this.dwIdentifier = identifier;
		this.pData = data;
	}
}

/**
 * Queue Service Thread
 * 
 * @author tp
 *
 */
class JQueueServiceThread extends JThread {

	private JQueueService m_pJQueueService;
	private static int index = 0;

	public JQueueServiceThread() {
		super("JQueueService-Thread" + (index++), Thread.NORM_PRIORITY);
	}

	public boolean InitThread(JQueueService pQueueService) {
		if (IsRunning()) {
			return false;
		}
		if (pQueueService == null) {
			return false;
		}

		m_pJQueueService = pQueueService;
		return true;
	}

	@Override
	protected boolean OnEventThreadStart() {
		return m_pJQueueService.OnQueueServiceStart();
	}

	@Override
	protected boolean OnEventThreadClose() {
		return m_pJQueueService.OnQueueServiceClose();
	}

	@Override
	protected boolean OnEventThreadRun() {
		return m_pJQueueService.OnQueueServiceRun();
	}
}

/**
 * Queue Service
 * 
 * @author tp
 *
 */
public class JQueueService implements IQueueService {

	private volatile boolean m_bService;
	private JQueueServiceThread m_queueServiceThread;
	private LinkedBlockingQueue<JQueueData> m_queueData;

	private IQueueServiceSink m_pQueueServiceSink;

	public JQueueService() {
		m_bService = false;
		m_queueServiceThread = null;
		m_pQueueServiceSink = null;
		m_queueData = new LinkedBlockingQueue<JQueueData>();
	}

	@Override
	public boolean StartService() {
		if (m_bService) {
			JLogger.warning("队列服务已经启动,启动将被忽略!");
			return true;
		}
		if (m_pQueueServiceSink == null) {
			JLogger.severe("队列服务回调接口为空!");
			return false;
		}
		m_queueServiceThread = new JQueueServiceThread();
		m_queueServiceThread.InitThread(this);
		// Start Thread
		if (m_queueServiceThread.StartThread() == false) {
			JLogger.severe("队列服务线程启动失败!");
			return false;
		}
		m_bService = true;
		return true;
	}

	@Override
	public boolean StopService() {
		if (!m_bService)
			return false;
		m_bService = false;
		m_queueServiceThread.ChoseThread();
		m_queueServiceThread = null;
		m_queueData.clear();
		return true;
	}

	@Override
	public boolean SetQueueServiceSink(IQueueServiceSink pIQueueServiceSink) {
		if (m_bService)
			return false;

		m_pQueueServiceSink = pIQueueServiceSink;

		return m_pQueueServiceSink != null;
	}

	@Override
	public boolean AddToQueue(int dwIdentifier, Object pData) {

		try {
			m_queueData.put(new JQueueData(dwIdentifier, pData));
		} catch (InterruptedException e) {
			JLogger.severe("队列插入数据错误:" + e.getMessage());
			return false;
		}
		return true;
	}

	protected boolean OnQueueServiceStart() {
		return m_pQueueServiceSink.QueueServiceStartSink();
	}

	protected boolean OnQueueServiceClose() {
		return m_pQueueServiceSink.QueueServiceStopSink();
	}

	protected boolean OnQueueServiceRun() {
		try {
			JQueueData queueData = m_queueData.take();
			if (queueData != null) {
				m_pQueueServiceSink.QueueServiceDataSink(
						queueData.dwIdentifier, queueData.pData);
			}
		} catch (InterruptedException e) {
			if(m_bService){
				JLogger.severe("队列获取数据错误:" + e.getMessage());
			}
		}
		return true;
	}
}

package yoyo.timer.event;

public class NTY_TimerEvent {
	private int	m_dwTimerID;
	public int getTimerID() {
		return m_dwTimerID;
	}
	
	private int m_dwLeftRepeat;
	public int getLeftRepeat(){
		return m_dwLeftRepeat;
	}
	
	private Object	m_bindParameter;
	public Object getBindParamter() {
		return m_bindParameter;
	}
	
	public NTY_TimerEvent(int dwTimerID, int dwLeftRepeat, Object bindParameter) {
		m_dwTimerID 	= dwTimerID;
		m_dwLeftRepeat	= dwLeftRepeat;
		m_bindParameter = bindParameter;
	}
}

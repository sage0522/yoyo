package yoyo.common.logger;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import yoyo.common.thread.JThread;
import yoyo.common.utils.DateHelper;

class JLoggerThread extends JThread{

	public JLoggerThread(){
		super("Logger", Thread.NORM_PRIORITY);
	}
	private JLogger m_pJLogger;
	protected boolean InitThread(JLogger pJLogger){
		if(IsRunning()) return false;
		m_pJLogger = pJLogger;
		return true;
	}
	protected boolean OnEventThreadStart(){
		return m_pJLogger.OnJLoggerStart();
	}
	protected boolean OnEventThreadRun(){
		return m_pJLogger.OnJLoggerRun();
	}
	protected boolean OnEventThreadClose(){
		return m_pJLogger.OnJLoggerStop();
	}
}
class LoggerRecord{
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	private String message;
	public String getMessage(){
		return message;
	}
	private int level;
	public int getLevel(){
		return level;
	}
	public LoggerRecord(Level pLevel,String msg){
		level = pLevel.intValue();
		this.message = "[" + dateFormat.format(new Date()) + "-" + pLevel.getLocalizedName()
		+" "+ Thread.currentThread().getName() +"] " + msg;
	}
}
/**
 * Global Logger
 * @author tp
 */
public class JLogger {
	
	private static JLogger instance;
	private static JLogger getJLogger(){
		return instance==null?(instance = new JLogger()):instance;
	}
	private volatile boolean m_bInit;
	private Date m_today;
	private LinkedBlockingQueue<LoggerRecord> m_LoggerQueue;
	private JLoggerThread m_pLoggerThread;
	
	private File		m_LoggerFile;
	
	private JTextComponent m_pTextComponent;
	
	public static void SetTextComponent(JTextComponent pTextComponent) {
		getJLogger().m_pTextComponent = pTextComponent;
	}
	
	public JLogger(){
		if(!m_bInit){
			m_today			= new Date();	
			m_LoggerQueue 	= new LinkedBlockingQueue<LoggerRecord>();
			initFileHandler();
			//启动服务
			m_pLoggerThread = new JLoggerThread();
			m_pLoggerThread.InitThread(this);
			m_pLoggerThread.StartThread();
			m_bInit = true;
		}
	}
	
	private void initFileHandler(){
		
		StringBuffer filePathBuffer = new StringBuffer();
		filePathBuffer.append(System.getProperty("user.dir"));
		filePathBuffer.append(File.separator+"log");
		//检查是否存在Log日志的路径
		m_LoggerFile = new File(filePathBuffer.toString());
		 if (!m_LoggerFile.exists()) {
			 m_LoggerFile.mkdir();
		 }
		 SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		 m_today = new Date();
		 filePathBuffer.append(File.separator+dateFormat.format( m_today ) +".log");
		 m_LoggerFile = new File(filePathBuffer.toString());
		 if (!m_LoggerFile.exists()) {
			 try {
				m_LoggerFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		 }
	}
	private void AddLogger(Level level,String msg){
		try {
			m_LoggerQueue.put(new LoggerRecord(level, msg));
		} catch (InterruptedException e) {
		}
	}
	
	public static void fine(String msg){
		getJLogger().AddLogger(Level.FINE,msg);
	}
	public static void config(String msg){
		getJLogger().AddLogger(Level.CONFIG,msg);
	}
	public static void info(String msg){
		getJLogger().AddLogger(Level.INFO,msg);
	}
	public static void warning(String msg){
		getJLogger().AddLogger(Level.WARNING,msg);
	}
	public static void severe(String msg){
		getJLogger().AddLogger(Level.SEVERE,msg);
	}
	
	
	protected boolean OnJLoggerStart(){
		return true;
	}
	protected boolean OnJLoggerStop(){
		return true;
	}
	
	protected boolean OnJLoggerRun(){
		try {
			LoggerRecord loggerRecord = m_LoggerQueue.take();
			//检查日期
			if(DateHelper.isSameDate( m_today, new Date())==false){
				initFileHandler();
			}
			if(loggerRecord.getLevel()>=800){
				FileWriter fw = new FileWriter(m_LoggerFile.getAbsoluteFile(),true);
				BufferedWriter bufferWriter = new BufferedWriter(fw);
				bufferWriter.write(loggerRecord.getMessage() + "\r\n");
				bufferWriter.close();
				System.err.println(loggerRecord.getMessage());
				if(m_pTextComponent!=null){
					AppendTextComonent(loggerRecord.getLevel(), loggerRecord.getMessage()+"\n" );
				}
			}else{
				System.out.println( loggerRecord.getMessage() );
				if(m_pTextComponent!=null){
					AppendTextComonent(loggerRecord.getLevel(), loggerRecord.getMessage()+"\n" );
				}
			}
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
	        PrintWriter pw = new PrintWriter(sw);
	        e.printStackTrace(pw);
	        AddLogger(Level.SEVERE, sw.toString());
		}
		return true;
	}
	private void AppendTextComonent(int level,String msg){
		Document doc  = m_pTextComponent.getDocument();
		SimpleAttributeSet attr = new SimpleAttributeSet();
		switch(level)
		{
			case 800:
			{
				StyleConstants.setForeground(attr, new Color(75, 175, 75));
				break;
			}
			case 900:
			{
				StyleConstants.setForeground(attr, new Color(175, 175, 75));
				break;
			}
			case 1000:
			{
				StyleConstants.setForeground(attr, new Color(250, 50, 50));
				break;
			}
			default:
			{
				StyleConstants.setForeground(attr, Color.BLACK);
			}
		}
		try {
			doc.insertString(doc.getLength(), msg, attr);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}
	private boolean stopLogger(){
		if(m_pLoggerThread!=null){
			m_pLoggerThread.ChoseThread();
			m_pLoggerThread = null;
		}
		if(m_LoggerQueue!=null){
			m_LoggerQueue.clear();
			m_LoggerQueue = null;
		}
		m_bInit = false;
		return true;
	}
	public static boolean StopService() {
		return getJLogger().stopLogger();
	}
}  

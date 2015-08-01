package yoyo.groovy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.codehaus.groovy.control.CompilationFailedException;

import groovy.lang.GroovyClassLoader;
import yoyo.common.exception.SingleCaseException;
import yoyo.common.logger.JLogger;
import yoyo.common.utils.HashHelper;

class GClassInfo{
	protected int key;
	protected String name;
	protected Class<?> target;
}
public class JGroovyLoader {
	private static JGroovyLoader _instance;
	public static JGroovyLoader getInstance(){
		return _instance==null?(_instance = new JGroovyLoader()):_instance;
	}

	private GroovyClassLoader m_GClassLoader;
	
	private ArrayList< GClassInfo > m_ClassInfo;
	public JGroovyLoader() {
		if(_instance!=null){
			try {
				throw new SingleCaseException();
			} catch (Exception e) {
				JLogger.severe(e.getMessage());
			}
			return;
		}
		_instance = this;
		m_GClassLoader = new GroovyClassLoader();
		m_ClassInfo = new ArrayList<GClassInfo>();
	}
	
	public String LoadGroovyFile(String file){
		
		try {

			StringBuffer filePathBuffer = new StringBuffer();
			filePathBuffer.append(System.getProperty("user.dir"));
			String separator= File.separator;
			file.replaceAll("\\\\", separator);
			filePathBuffer.append(separator+"groovy"+separator+file);
			
			Class<?> clazz = m_GClassLoader.parseClass(new File( filePathBuffer.toString() ));
			GClassInfo info = new GClassInfo();
			info.key = HashHelper.DJBHash(file);
			info.name = clazz.getName();
			info.target = clazz;
			m_ClassInfo.add(info);
			return info.name;
		} catch (CompilationFailedException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Object newInstance(String name){
		int size = m_ClassInfo.size();
		GClassInfo info = null;
		for(int i = 0;i<size;i++){
			info = m_ClassInfo.get(i);
			if(info.name == name){
				try {
					return info.target.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					e.printStackTrace();
					return null;
				}
			}
		}
		return null;
	}
}

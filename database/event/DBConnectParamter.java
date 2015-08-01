package yoyo.database.event;

public class DBConnectParamter {

	private String					m_SQLType;
	private String					m_DBUrl;
	private int						m_DBPort;
	private String					m_DBName;
	
	public String getUrl(){
		
		return "jdbc:"+m_SQLType+"://" + m_DBUrl + ":" + m_DBPort + "/" + m_DBName +"?useUnicode=true&characterEncoding=GB2312&autoReconnect=true&failOverReadOnly=false";
	}
	
	private String					m_DBUser;
	public String getUser(){
		return m_DBUser;
	}
	
	private String					m_DBPassword;
	public String getPassword(){
		return m_DBPassword;
	}
	public DBConnectParamter(String dbUrl,int dbPort,String dbName,String dbUser,String dbPassword){
		InitParamter(dbUrl,dbPort,dbName,dbUser,dbPassword,"mysql");
	}
	
	public DBConnectParamter(String dbUrl,int dbPort,String dbName,String dbUser,String dbPassword,String SQLType){
		InitParamter(dbUrl,dbPort,dbName,dbUser,dbPassword,SQLType);
	}
	
	private void InitParamter(String dbUrl,int dbPort,String dbName,String dbUser,String dbPassword,String SQLType){
		m_DBUrl 		= dbUrl;
		m_DBPort 		= dbPort;
		m_DBName 		= dbName;
		m_DBUser 		= dbUser;
		m_DBPassword 	= dbPassword;
		m_SQLType 		= SQLType;
	}
}

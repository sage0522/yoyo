package yoyo.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


import java.sql.Statement;
import java.sql.Types;

import yoyo.common.logger.JLogger;
import yoyo.database.event.DBConnectParamter;
import yoyo.database.face.IDBConnection;

public class JDBConnection implements IDBConnection {

	private volatile boolean	m_bConnected;
	//数据库信息变量
	private DBConnectParamter 	m_DBParamter;
	//数据库
	private Connection	m_Connection;
	
	@Override
	public boolean OpenConnection(DBConnectParamter paramter) {
		if(m_bConnected) {
			JLogger.warning("数据库已经连接,操作被忽略!");
			return false;
		}
		
		m_DBParamter = paramter;
		
		try {
			m_Connection = DriverManager.getConnection(m_DBParamter.getUrl(),m_DBParamter.getUser(),m_DBParamter.getPassword());
		} catch (SQLException e) {
			JLogger.severe("数据库连接错误:"+ e.getMessage() 
					+"("  + m_DBParamter.getUrl() 
					+ "|" + m_DBParamter.getUser() 
					+ "|" + m_DBParamter.getPassword()+")");
			return false;
		}
		m_bConnected = true;
		return true;
	}

	@Override
	public void CloseConnection() {
		if(!m_bConnected) return;
		
		try {
			m_Connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		m_Connection = null;
		m_bConnected = false;
	}
	
	public void setAutoCommit(boolean autoCommit){
		if(!m_bConnected) return;
		try {
			m_Connection.setAutoCommit(autoCommit);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public void Commit(boolean autoCommit){
		if(!m_bConnected) return;
		try {
			m_Connection.commit();
			m_Connection.setAutoCommit(autoCommit);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	@Override
	public int GetResultSetCount(ResultSet resultSet) {
		if(resultSet==null) return 0;
		int count = 0;
		try {
			resultSet.last();
			count = resultSet.getRow();
			resultSet.beforeFirst();
		} catch (Exception e) {
			count = 0;
		}
		return count;
	}
	@Override
	public Statement GetStatement() throws SQLException {
		return m_Connection.createStatement();
	}

	@Override
	public PreparedStatement GetPrepareStatement(String sql) throws SQLException {
		return m_Connection.prepareStatement(sql);
	}

	@Override
	public CallableStatement GetCallableStatement(String szSPName,int paramCount) throws SQLException {
		return GetCallableStatement(szSPName,paramCount,true);
	}
	@Override
	public CallableStatement GetCallableStatement(String szSPName,int paramCount,boolean bErrorCode) throws SQLException {
		String exeSQL = "{CALL " + szSPName + "(";
		int i = 0;
		paramCount = bErrorCode?(paramCount+1) : paramCount;
		for (i = 0; i < paramCount; i++)
		{
			exeSQL += ((paramCount - 1) == i) ? "?" : "?,";
		}
		exeSQL += ")}";
		CallableStatement result = m_Connection.prepareCall(exeSQL);
		if(bErrorCode){
			result.registerOutParameter("$errorCode", Types.INTEGER);
		}
		return result;
	}
	
	public int GetErrorCode(CallableStatement statement){
		
		try {
			return statement.getInt("$errorCode");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 9999;
	}
	
	@Override
	public void CloseStatement(Statement statement) {
		if(statement!=null){
			try {
				if(statement.isClosed() == false){
					statement.clearWarnings();
					statement.clearBatch();
					statement.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

}

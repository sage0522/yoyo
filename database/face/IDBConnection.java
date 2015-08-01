package yoyo.database.face;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import yoyo.database.event.DBConnectParamter;

/**
 * data base connection
 * @author tiant_000
 *
 */
public interface IDBConnection {

	/**
	 * open connection
	 * @param paramter
	 * @return
	*/
	boolean OpenConnection(DBConnectParamter paramter);
	
	/**
	 * close connection
	 */
	void CloseConnection();
	
	/**
	 * 
	 * @return
	 * @throws SQLException 
	 */
	int GetResultSetCount(ResultSet resultSet);
	Statement GetStatement() throws SQLException;
	PreparedStatement GetPrepareStatement(String sql) throws SQLException;
	CallableStatement GetCallableStatement(String szSPName,int paramCount) throws SQLException;
	CallableStatement GetCallableStatement(String szSPName,int paramCount,boolean bErrorCode) throws SQLException;
	
	
	/**
	 * 
	 * @param statement
	 * @return
	 */
	int GetErrorCode(CallableStatement statement);
	/**
	 * 
	 **/
	void CloseStatement( Statement statement );
	
	/**
	 * 
	 * @param autoCommit
	 */
	void setAutoCommit(boolean autoCommit);
	
	/**
	 *  @param autoCommit
	 */
	void Commit(boolean autoCommit);
}

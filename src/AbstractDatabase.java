import java.sql.*;

import javax.servlet.http.HttpSession;

public abstract class AbstractDatabase<Element>
{
	private java.sql.Connection _connection = null;
	
	protected AbstractDatabase(HttpSession session)
	throws SQLException
	{
		//make sure we have a connection present
		retrieveConnection(session);
		
		String tableName = getTableName();
		
		if(!tableExists(tableName))
			createTable();		
	}
	
	private boolean tableExists(String tableName)
	throws SQLException
	{
		PreparedStatement statement = getConnection().prepareStatement
		(
			"SELECT name from sqlite_master WHERE type = 'table' AND name = '?'"
		);
		
		statement.setString(0, tableName);
		ResultSet result = statement.executeQuery();
		return result.next(); //when this succeeds we have at least one row
	}
	
	protected java.sql.Connection getConnection()
	{
		return _connection;
	}
	
	protected java.sql.Connection retrieveConnection(HttpSession session)
	throws java.sql.SQLException
	{
		final String KEY = "DatabaseConnection";
		
		if(_connection == null)
		{			
			//try to get database of session
			_connection = (java.sql.Connection)session.getAttribute(KEY);
			
			if(_connection == null)
			{
				//open a new connection to database
				org.sqlite.JDBC jdbc = new org.sqlite.JDBC();
				_connection = jdbc.connect("node.db", null);
				session.setAttribute(KEY, _connection);
			}
		}
	
		return _connection;
	}
	
	protected void execute(String sqlCommand)
	throws SQLException
	{
		Statement statement = getConnection().createStatement();
		try
		{
		    statement.executeUpdate(sqlCommand);
		}
		finally
		{
			statement.close();
		}
	}
	
	protected abstract String getTableName();
	
	protected abstract void createTable()
	throws SQLException;
	
	public abstract void delete(Element element)
	throws SQLException;
}

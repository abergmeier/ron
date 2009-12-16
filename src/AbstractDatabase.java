import java.sql.*;

public abstract class AbstractDatabase<Element>
{
	private java.sql.Connection _connection;
	
	protected AbstractDatabase()
	throws SQLException
	{
		String tableName = getTableName();
		
		if(!tableExists(tableName))
			createTable(tableName);		
	}
	
	private boolean tableExists(String tableName)
	{
		"SELECT name from sqlite_master WHERE type = 'table' AND name = '" + tableName + '";
	}
	
	protected java.sql.Connection getConnection()
	throws java.sql.SQLException
	{
		if(_connection != null)
		{
			_connection = connect(java.lang.String url, java.util.Properties info);
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
	
	protected abstract void createTable(String tableName)
	throws SQLException;
	
	public abstract void delete(Element element)
	throws SQLException;
}

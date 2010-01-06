
	
package org.ron.servlet;

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.http.HttpSession;

/**
 * @author andreas
 *
 * @param <Element>
 */
public abstract class AbstractDatabase<Element>
implements Collection<Element>
{
	final int FIRSTCOLUMN = 1;
	private java.sql.Connection _connection = null;
	
	protected AbstractDatabase(HttpSession session)
	throws SQLException, ClassNotFoundException
	{
		//make sure we have a connection present
		retrieveConnection(session);
		
		if(!tableExists())
			createTable();		
	}
	
	private boolean tableExists()
	throws SQLException
	{
		PreparedStatement statement = getConnection().prepareStatement
		(
			"SELECT name from sqlite_master WHERE type = 'table' AND name = ?"
		);
		
		try
		{
			statement.setString(1, getTableName());
			ResultSet result = statement.executeQuery();
			return result.getString(1) != null; //when this succeeds we have at least one row
		}
		finally
		{
			finallyCloseStatement(statement);
		}
	}
	
	public int size()
	{
		try
		{
			return getInt("SELECT COUNT(*) FROM " + getTableName());
		}
		catch (SQLException e)
		{
			throw wrapInRuntimeException(e);
		}
	}
	
	protected java.sql.Connection getConnection()
	{
		return _connection;
	}
	
	protected java.sql.Connection retrieveConnection(HttpSession session)
	throws java.sql.SQLException, ClassNotFoundException
	{
		final String KEY = "DatabaseConnection";
		
		if(_connection == null)
		{			
			//try to get database of session
			_connection = (java.sql.Connection)session.getAttribute(KEY);
			
			if(_connection == null)
			{
				//open a new connection to database
				Class.forName("org.sqlite.JDBC");
				_connection = DriverManager.getConnection("jdbc:sqlite:server.db");
			    
/*				
				org.sqlite.JDBC jdbc = new org.sqlite.JDBC();
				_connection = jdbc.connect("node.db", null);
*/				
				if(_connection == null)
					throw new NullPointerException("Could not create database");
				
				session.setAttribute(KEY, _connection);
			}
		}
	
		return _connection;
	}
	
	protected int execute(String sqlCommand)
	throws SQLException
	{
		Statement statement = getConnection().createStatement();
		
		try
		{
		    return statement.executeUpdate(sqlCommand);
		}
		finally
		{
			finallyCloseStatement(statement);
		}
	}
	
	protected String getString(String sqlCommand)
	throws SQLException
	{
		Statement statement = getConnection().createStatement();
		
		try
		{
			ResultSet result = statement.executeQuery(sqlCommand);
			if(!result.next())
				throw new SQLException();
			
			return result.getString(FIRSTCOLUMN);
		}
		finally
		{			
			finallyCloseStatement(statement);
		}
	}
	
	protected int getInt(String sqlCommand)
	throws SQLException
	{
		Statement statement = getConnection().createStatement();
		
		try
		{
			ResultSet result = statement.executeQuery(sqlCommand);
			if(!result.next())
				throw new SQLException();
			
			return result.getInt(FIRSTCOLUMN);
		}
		finally
		{			
			finallyCloseStatement(statement);
		}
	}
	
	protected int deleteFromTable(String where)
	throws SQLException
	{
		return execute("DELETE FROM " + getTableName() + " WHERE " + where);
	}

	public Object[] toArray()
	{
		int size = size();
		
		Object[] result = new Object[size];
			
		toArray(result);
			
		return result;
	}
	
	public Iterator<Element> iterator()
	{
		ResultSet result = null; 
		
		try
		{
			result = getAll();
			ArrayList<Element> nodes = new ArrayList<Element>(size());
			
			while(result.next())
			{
				nodes.add(get(result));
			}
			
			return nodes.iterator();
		}
		catch (SQLException e)
		{
			throw wrapInRuntimeException(e);
		}
		finally
		{
			finallyCloseStatement(result);
		}
	}
	
	public <T> T[] toArray(T[] array)
	{
		int size = size(); 
		if (array.length < size) 
			array = (T[])java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), size); 
		  
		Iterator<Element> it = iterator(); 
		
		for(int i = 0; i != size; i++)
			array[i] = (T)it.next();
		
		if (array.length > size) array[size] = null; 
			return array; 
	}
	
	protected void finallyCloseStatement(Statement statement)
	{
		if(statement == null)
			return;
		
		try
		{
			statement.close();
		}
		catch (SQLException e)
		{
			//suppress throwing
			e.printStackTrace();
		}
	}
	
	protected void finallyCloseStatement(ResultSet resultSet)
	{
		if(resultSet == null)
			return;
		
		try
		{
			resultSet.getStatement().close();
		}
		catch (SQLException e)
		{
			//suppress throwing
			e.printStackTrace();
		}
	}
	
	protected void finallyCloseStatement(PreparedStatement statement)
	{
		if(statement == null)
			return;
		
		try
		{
			statement.close();
		}
		catch (SQLException e)
		{
			//suppress throwing
			e.printStackTrace();
		}
	}
	
	protected RuntimeException wrapInRuntimeException(Exception cause)
	{
		return new RuntimeException(cause);
	}
	
	protected abstract ResultSet getAll()
	throws SQLException;
	
	protected abstract Element get(ResultSet result)
	throws SQLException;
	
	protected ResultSet getWhere(String where)
	throws SQLException
	{
		Statement statement = getConnection().createStatement();
		return statement.executeQuery
		(
			"SELECT " + getSQLFields() + " " +
			"FROM " + getTableName() + " " +
			"WHERE " + where
		);
	}
	
	protected abstract String getSQLFields();
	
	protected abstract String getTableName();
	
	protected abstract void createTable()
	throws SQLException;
	
	public void clear()
	{
		try
		{
			deleteFromTable("1=1");
		}
		catch(SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
	}
	
	public boolean remove(Object o)
	{
		return remove((Element)o);
	}
	
	public boolean contains(Object arg0)
	{
		return contains((Element)arg0);
	}
}

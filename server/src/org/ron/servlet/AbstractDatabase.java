
	
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
	private ReentrantReadWriteLock _lock = new ReentrantReadWriteLock();
	
	protected AbstractDatabase(HttpSession session)
	throws SQLException, ClassNotFoundException
	{
		//make sure we have a connection present
		retrieveConnection(session);
		
		if(!tableExists())
			createTable();		
	}
	
	protected ReadWriteLock getLock()
	{
		return _lock;
	}
	
	private boolean tableExists()
	throws SQLException
	{
		PreparedStatement statement = getConnection().prepareStatement
		(
			"SELECT name from sqlite_master WHERE type = 'table' AND name = ?"
		);
		
		Lock readLock = null;
		try
		{
			statement.setString(1, getTableName());
			readLock = getLock().readLock();
			readLock.lock();
			ResultSet result = statement.executeQuery();
			return result.next(); //when this succeeds we have at least one row
		}
		finally
		{
			if(readLock != null)
				readLock.unlock();
			
			statement.close();
		}
	}
	
	public int size()
	{
		Lock readLock = getLock().readLock();
		try
		{
			return getInt("SELECT COUNT(*) FROM " + getTableName());
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			readLock.unlock();
		}
		
		return 0;
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
		
		Lock writeLock = null;
		
		try
		{
			writeLock = getLock().writeLock();
			writeLock.lock();
		    return statement.executeUpdate(sqlCommand);
		}
		finally
		{
			writeLock.unlock();
			
			statement.close();
		}
	}
	
	protected String getString(String sqlCommand)
	throws SQLException
	{
		Statement statement = getConnection().createStatement();
		Lock readLock = null;
		
		try
		{
			readLock = getLock().readLock();
			readLock.lock();
			ResultSet result = statement.executeQuery(sqlCommand);
			if(!result.next())
				throw new SQLException();
			
			return result.getString(FIRSTCOLUMN);
		}
		finally
		{
			if(readLock != null)
				readLock.unlock();
			
			statement.close();
		}
	}
	
	protected int getInt(String sqlCommand)
	throws SQLException
	{
		Statement statement = getConnection().createStatement();
		
		Lock readLock = null;
		
		try
		{
			readLock = getLock().readLock();
			readLock.lock();
			ResultSet result = statement.executeQuery(sqlCommand);
			if(!result.next())
				throw new SQLException();
			
			return result.getInt(FIRSTCOLUMN);
		}
		finally
		{
			if(readLock != null)
				readLock.unlock();
			
			statement.close();
		}
	}
	
	protected int deleteFromTable(String where)
	throws SQLException
	{
		return execute("DELETE FROM " + getTableName() + " WHERE " + where);
	}

	public Object[] toArray()
	{
		getLock().readLock().lock();
		
		try
		{
			int size = size();
		
			Object[] result = new Object[size];
			
			toArray(result);
			
			return result;
		}
		finally
		{
			getLock().readLock().unlock();
		}
	}
	
	public Iterator<Element> iterator()
	{
		getLock().readLock().lock();
		
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
			e.printStackTrace();
		}
		finally
		{
			getLock().readLock().unlock();
			
			if(result != null)
				try
				{
					result.getStatement().close();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
		}
		
		return null;
	}
	
	public <T> T[] toArray(T[] array)
	{
		getLock().readLock().lock();
	
		try
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
		finally
		{
			getLock().readLock().unlock();
		}
	}	
	
	protected NullPointerException wrapInNullException(Exception cause)
	{
		NullPointerException exception = new NullPointerException();
		exception.initCause(cause);
		return exception;
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

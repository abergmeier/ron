
	
package org.ron.servlet;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import javax.vecmath.Vector2f;

/**
 * @author Andreas Bergmeier
 *
 * @param <Element>
 */
public abstract class AbstractDatabase<Element>
implements Collection<Element>
{
	private class StatementKey
	{
		private int _connectionId;
		private int _connectionHashCode;
		
		public StatementKey(int connectionId, Connection connection)
		{
			_connectionId = connectionId;
			_connectionHashCode = connection.hashCode();
		}
		
		@Override
		public boolean equals(Object object)
		{
			StatementKey key = (StatementKey)object;
			
			return _connectionId == key._connectionId
				&& _connectionHashCode == key._connectionHashCode;
		}
	}
	
	private final int FIRSTCOLUMN = 1;
	private HashMap<StatementKey, PreparedStatement> _connectionMap = new HashMap<StatementKey, PreparedStatement>();
	
	private ThreadLocal<Connection> _threadConnections = new ThreadLocal<Connection>();
		
	protected AbstractDatabase(Connection connection)
	throws SQLException
	{
		setConnection(connection);
		
		validateTable();
	}
	
	/** Currently just creates table when not already existent
	 * @throws SQLException
	 */
	private void validateTable()
	throws SQLException
	{
		createTable();
	}
	
	public void close()
	{
		synchronized(_connectionMap)
		{
			for(PreparedStatement statement : _connectionMap.values())
			{
				try
				{
					statement.close();
				}
				catch(SQLException exception)
				{
					if(!exception.getMessage().startsWith("Already closed"))
						exception.printStackTrace();
				}
			}
			
			_connectionMap.clear();		
		}
	}
	
	public void setConnection(Connection connection)
	{
		_threadConnections.set(connection);
	}
	
	private static Set<Integer> _uniqueRandomKeys = new TreeSet<Integer>();
	
	protected static int getUniqueRandom()
	{
		synchronized(_uniqueRandomKeys)
		{
			int result;
			
			while(true)
			{
				result = new Random().nextInt();
				
				if(_uniqueRandomKeys.add(result))
					return result;
			}				
		}
	}
	
	public int size()
	{
		try
		{
			return getInt("SELECT COUNT(*) FROM " + getSQLTableName());
		}
		catch (SQLException e)
		{
			throw wrapInRuntimeException(e);
		}
	}
	
	
	/**
	 *  
	 * @return The connection associated with the calling thread
	 */
	protected java.sql.Connection getConnection()
	{
		return _threadConnections.get();
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
	
	protected static float getLatitude(Vector2f vector)
	{
		return vector.getX();
	}
	
	protected static float getLongitude(Vector2f vector)
	{
		return vector.getY();
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
	
	protected float getFloat(ResultSet result, int columnIndex)
	throws SQLException
	{
		float value = result.getFloat(columnIndex);
		
		if(result.wasNull())
			value = Float.NaN;
		
		return value;
	}
	
	protected int deleteFromTable(String where)
	throws SQLException
	{
		return execute("DELETE FROM " + getSQLTableName() + " WHERE " + where);
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
		Savepoint save;
		try
		{
			save = setSavepoint();
		}
		catch (SQLException e1)
		{
			throw wrapInRuntimeException(e1);
		}
		
		try
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
			finally
			{
				result.close();
			}
		}
		catch(SQLException exception)
		{
			rollback(save);			
			throw wrapInRuntimeException(exception); 
		}
		finally
		{
			releaseSavepoint(save);
		}
	}
	
	public boolean isEmpty()
	{
		return size() == 0;
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
	
	protected boolean commit()
	{
		try
		{
			getConnection().commit();
			return true;
		}
		catch (SQLException exception)
		{
			exception.printStackTrace();
			return false;
		}
	}
	
	protected Savepoint setSavepoint()
	throws SQLException
	{
		return getConnection().setSavepoint();
	}
	
	protected void rollback()
	throws SQLException
	{
		getConnection().rollback();
	}
	
	protected void rollback(Savepoint savepoint)
	{
		try
		{
			getConnection().rollback(savepoint);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	protected void releaseSavepoint(Savepoint savepoint)
	{
		try
		{
			getConnection().releaseSavepoint(savepoint);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
	
	protected PreparedStatement getPreparedStatement(int key, String sql)
	throws SQLException
	{
		PreparedStatement statement = null;
		
		synchronized(_connectionMap)
		{
			StatementKey statementKey = new StatementKey(key, getConnection());
			statement = _connectionMap.get(statementKey);
			
			//not yet prepared for this connection
			if(statement == null)
			{
				statement = getConnection().prepareStatement(sql);
				_connectionMap.put(statementKey, statement);
			}
		}
		
		return statement;		
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
	
	protected RuntimeException wrapInRuntimeException(SQLException cause)
	{
		return SQLiteConnection.wrapInRuntimeException(cause);
	}
	
	private static final int PS_GETALL = getUniqueRandom();
	private final String PS_GETALL_SQL =
		"SELECT " + getSQLFields() + " " +
		"FROM " + getSQLTableName() + " " +
		"ORDER BY " + getSQLOrder();
	
	/**
	 * Returns a Result set containing all Nodes
	 * @return
	 * @throws SQLException
	 */
	protected ResultSet getAll()
	throws SQLException
	{
		PreparedStatement statement = getPreparedStatement
		(
			PS_GETALL,
			PS_GETALL_SQL
		);
		
		try
		{
			synchronized(statement)
			{
				return statement.executeQuery();
			}
		}
		catch(RuntimeException exception)
		{
			finallyCloseStatement(statement);
			throw exception;
		}
		catch(SQLException exception)
		{
			finallyCloseStatement(statement);
			throw exception;
		}
		
		//do not close the statement since the ResultSet
		//has to be accessed outside of method
	}
	
	protected abstract Element get(ResultSet result)
	throws SQLException;
	
	protected ResultSet getWhere(String where)
	throws SQLException
	{
		Statement statement = getConnection().createStatement();
		
		try
		{
			return statement.executeQuery
			(
				"SELECT " + getSQLFields() + " " +
				"FROM " + getSQLTableName() + " " +
				"WHERE " + where
			);
		}
		catch(RuntimeException exception)
		{
			finallyCloseStatement(statement);
			throw exception;
		}
		catch(SQLException exception)
		{
			finallyCloseStatement(statement);
			throw exception;
		}
	}
	
	protected abstract String getSQLIdColumn();
	
	protected abstract String getSQLFields();
	
	protected abstract String getSQLTableName();
	
	protected abstract String getSQLOrder();
	
	//protected abstract Player getPlayer(ResultSet result);
	
	protected abstract void createTable()
	throws SQLException;
	
	protected Collection<?> getDependentDatabase()
	{
		return null;
	}
	
	public void clear()
	{
		Savepoint save;
		try
		{		
			save = setSavepoint();
		}
		catch (SQLException e)
		{
			throw wrapInRuntimeException(e);
		}
		
		try
		{
			//attemt to clear all dependencies first
			Collection<?> dependent = getDependentDatabase();
			
			if(dependent != null)
				dependent.clear();
			
			//now really delete
			deleteFromTable("1=1");
		}
		catch(SQLException exception)
		{
			rollback(save);
			throw wrapInRuntimeException(exception);
		}
		catch(RuntimeException exception)
		{
			rollback(save);
			throw exception;
		}
		finally
		{
			releaseSavepoint(save);
		}
	}
	
	private final int PS_GETINSERT = getUniqueRandom();
	private final String PS_GETLAST =
		"SELECT " + getSQLFields() + " " +
		"FROM " + getSQLTableName() + " " +
		"WHERE ROWID = last_insert_rowid()";
	
	protected Element getLastInserted()
	throws SQLException
	{
		Savepoint save = getConnection().setSavepoint();
		
		try
		{
			PreparedStatement statement = getPreparedStatement
			(
				PS_GETINSERT,
				PS_GETLAST
			);
			
			synchronized(statement)
			{	
				ResultSet result = statement.executeQuery();
				
				try
				{
					if(result.next())
						return get(result);
					
					return null;
				}
				finally
				{
					result.close();
				}
			}
		}
		catch(SQLException exception)
		{
			rollback(save);
			throw exception;
		}
		catch(RuntimeException exception)
		{
			rollback(save);
			throw exception;
		}
		finally
		{
			getConnection().releaseSavepoint(save);
		}
	}
	
	protected static String getIdList(Collection<?> objects)
	{
		String idList = null;
		
		for(IntegerIdObject object : (Collection<IntegerIdObject>)objects)
		{
			if(idList == null)
				idList = ((Integer)object.getId()).toString();
			
			idList += "," + object.getId();
		}
		
		return idList;
	}
/*	
	public boolean remove(Object o)
	{
		return remove((Element)o);
	}
	
	public boolean contains(Object arg0)
	{
		return contains((Element)arg0);
	}
*/
}

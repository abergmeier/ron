package org.ron.servlet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Collection;
import java.util.Set;

public class PlayerDatabase
extends AbstractDatabase<Player>
implements Set<Player>
{
	public static final String SQLTABLENAME = "PLAYER";
	public static final String SQLIDCOLUMN = "ID";
	private static final String SQLFIELDS = SQLIDCOLUMN + ",LAT,LNG,NAME,BITS";
	private static final String SQLORDER = SQLIDCOLUMN + " ASC";
	private NodeDatabase _nodes;
	
	public PlayerDatabase(Connection connection)
	throws SQLException
	{
		super(connection);
		_nodes = new NodeDatabase(this);
	
		setConnection(connection);
	}
	
	public NodeDatabase getNodes()
	{
		return _nodes;
	}
	
	protected String getSQLIdColumn()
	{
		return SQLIDCOLUMN;
	}
	
	protected String getSQLFields()
	{
		return SQLFIELDS;
	}
	
	protected String getSQLTableName()
	{
		return SQLTABLENAME;
	}
	
	protected String getSQLOrder()
	{
		return SQLORDER;
	}
	
	@Override
	public void setConnection(Connection connection)
	{
		if(_nodes != null)
			_nodes.setConnection(connection);
		
		super.setConnection(connection);
	}
/*	
	//creates a new database on the session if necessary 
	public static PlayerDatabase get(HttpSession session)
	throws SQLException, ClassNotFoundException
	{
		final String KEY = "PlayerDatabase";
		PlayerDatabase instance = (PlayerDatabase)session.getAttribute(KEY);
		
		if(instance == null)
		{
			instance = new PlayerDatabase(session);
			session.setAttribute(KEY, instance);
		}
	
		return instance;
	}
	*/
	
	protected void createTable()
	throws SQLException
	{
		execute
		(
				"CREATE TABLE IF NOT EXISTS " + SQLTABLENAME + " " + 
				"(" +
//TODO: Reenable autoincrement after presentation
/*
    				"ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
*/
    				"ID INTEGER PRIMARY KEY NOT NULL," +
    				"LAT REAL NULL," +
    				"LNG REAL NULL," +
    				"NAME TEXT NOT NULL," +
    				"BITS INTEGER NULL" +
    			");"
		);
	}
	
	protected int getPlayerId(ResultSet result)
	throws SQLException
	{
		return result.getInt(1);
	}
	
	private static final int PS_RESULTSET = getUniqueRandom();
	
	protected ResultSet getResultSet(Player player)
	throws SQLException
	{
		PreparedStatement statement = getPreparedStatement
		(
			PS_RESULTSET,
			"SELECT " + SQLFIELDS + " " +
			"FROM " + SQLTABLENAME + " " +
			"WHERE ID = ?"
		);
		
		ResultSet result = null;
		
		try
		{
			synchronized(statement)
			{
				statement.setInt(1, player.getId());
				result = statement.executeQuery();
				
				if(result.next())
					return result;
			}
			
			throw new SQLException("NO RESULT!");
		}
		catch(SQLException exception)
		{
			finallyCloseStatement(result);
			throw exception;
		}
		catch(RuntimeException exception)
		{
			finallyCloseStatement(result);
			throw exception;
		}
	}
	
	public float getLatitude(Player player)
	throws SQLException
	{
		ResultSet result = getResultSet(player);
		try
		{
			return getLatitude(result);
		}
		finally
		{
			finallyCloseStatement(result);
		}
	}
	
	public float getLongitude(Player player)
	throws SQLException
	{
		ResultSet result = getResultSet(player);
		
		try
		{
			return getLongitude(result);
		}
		finally
		{
			finallyCloseStatement(result);
		}
	}
	
	protected float getLatitude(ResultSet result)
	throws SQLException
	{
		return result.getFloat(2);
	}
	
	protected float getLongitude(ResultSet result)
	throws SQLException
	{
		return result.getFloat(3);
	}
	
	protected Player get(ResultSet result)
	throws SQLException
	{
		return get(getPlayerId(result));
	}
	
	private static final int BIT_LOST = 1 << 0;
	
	public boolean hasLost(Player player)
	throws SQLException
	{
		try
		{
			return (getPlayerBits(player) & BIT_LOST) == BIT_LOST;
		}
		catch(NoResultException exception)
		{
			return true;
		}
	}
	
	protected boolean hasLost(ResultSet result)
	throws SQLException
	{
		try
		{
			return (getPlayerBits(result) & BIT_LOST) == BIT_LOST;
		}
		catch(NoResultException exception)
		{
			return true;
		}
	}
	
	public void setLost(Player player, boolean set)
	throws SQLException
	{
		setPlayerBits(player, getPlayerBits(player) | BIT_LOST);
	}
	
	private static final int PS_SELECTPLAYER = getUniqueRandom();
	
	protected ResultSet getResult(Player player)
	throws SQLException
	{
		PreparedStatement statement = getPreparedStatement
		(
			PS_SELECTPLAYER,
			"SELECT " + SQLFIELDS + " " + 
			"FROM " + SQLTABLENAME + " " + 
			"WHERE ID = ?"
		);

		synchronized(statement)
		{
			statement.setInt(1, player.getId());
			
			ResultSet result = statement.executeQuery();
		
			try
			{
				if(result.next())
					return result;
				
				throw new NoResultException("NO RESULT");
			}
			catch(SQLException exception)
			{
				result.close();
				throw exception;
			}
			catch(RuntimeException exception)
			{
				result.close();
				throw exception;
			}
		}
	}
	
	protected int getPlayerBits(Player player)
	throws SQLException
	{
		ResultSet result = getResult(player);
		
		try
		{		
			return getPlayerBits(result);
		}
		finally
		{
			result.close();
		}
	}
	
	protected void setPlayerBits(Player player, int bits)
	throws SQLException
	{
		execute
		(
			"UPDATE " + SQLTABLENAME + " " + 
			"SET BITS = " + bits + " " +
			"WHERE ID = " + player.getId()
		);			
	}
	
	public Player get(int playerId)
	throws SQLException
	{
		//TODO: check for existance
		return new PlayerImpl(playerId, this);
	}
	
	protected int getPlayerBits(ResultSet result)
	throws SQLException
	{
		return result.getInt(5);
	}
	
	protected String getPlayerName(ResultSet result)
	throws SQLException
	{
		return result.getString(4);
	}
	
	public String getPlayerName(Player player)
	throws SQLException
	{
		ResultSet result = getResultSet(player);
		
		try
		{
			return getPlayerName(result);
		}
		finally
		{
			finallyCloseStatement(result);
		}
	}

	public void setPosition(Player player, Position position)
	throws SQLException
	{
		setPosition(player, position.getLatitude(), position.getLongitude()); 
	}
	
	private static final int PS_POSITION = getUniqueRandom(); 
	
	public void setPosition(Player player, float lat, float lng)
	throws SQLException
	{
		PreparedStatement statement = getPreparedStatement
		(
			PS_POSITION,
			"UPDATE " + SQLTABLENAME + " " +
			"SET LAT = ?, LNG = ?" +
			"WHERE ID = ?"
		);
		
		synchronized(statement)
		{
			statement.setFloat(1, lat);
			statement.setFloat(2, lng);
			statement.setInt(3, player.getId());
			statement.executeUpdate();			
		}
	}

	public boolean isEmpty()
	{
		return size() == 0;
	}

	public boolean add(Player player)
	{
		throw new UnsupportedOperationException();
	}
	
	public Player add(String playerName, float lat, float lng)
	throws SQLException
	{
		Savepoint save = getConnection().setSavepoint();
		
		try
		{
			execute
			(
				"INSERT INTO " + SQLTABLENAME + " " +
				"(NAME, LAT, LNG) VALUES " +
				"('" + playerName + "'," + lat + "," + lng + ");"
			);
			
			return getLastInserted();
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

	public boolean addAll(Collection<? extends Player> arg0)
	{
		throw new UnsupportedOperationException();
	}
	
	public boolean contains(Object object)
	{
		Player player = (Player)object;
		
		try
		{
			return getString
			(
				"SELECT ID " +
				"FROM " + SQLTABLENAME + " " +
				"WHERE ID = " + player.getId()
			) != null;
		}
		catch (SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
	}

	public boolean containsAll(Collection<?> arg0)
	{
		for(Object object : arg0)
		{
			if(contains(object))
				continue;
			
			return false;
		}
		return true;
	}
	
	@Override
	public Collection<?> getDependentDatabase()
	{
		return _nodes;
	}
	
	@Override
	public boolean remove(Object object)
	{
		Connection connection = getConnection();
		Savepoint save;
		try
		{
			save = connection.setSavepoint();
		}
		catch (SQLException e1)
		{
			throw wrapInRuntimeException(e1);
		}
		
		Player player = (Player)object;
		
		try
		{
			//check for other players
			ResultSet result;
			try
			{
				result = getWhere("ID <> " + player.getId());
			}
			catch (SQLException exception)
			{
				throw wrapInRuntimeException(exception);
			}
	
			boolean allLost = true;
			
			try
			{
				while(result.next())
				{
					allLost = allLost && hasLost(result);
				}
			}
			catch(SQLException exception)
			{
				throw wrapInRuntimeException(exception);
			}
			finally
			{
				try
				{
					result.close();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
			}
			
			//all have already lost - no dependencies anymore
			//we can delete all players
			if(allLost)
			{
				clear();
				return true;
			}
			else
			{
				setLost(player, true);
				return false;
			}
		}
		catch(SQLException exception)
		{
			try
			{
				getConnection().rollback(save);
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			
			throw wrapInRuntimeException(exception);
		}
		finally
		{
			try
			{
				getConnection().releaseSavepoint(save);
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public boolean removeAll(Collection<?> players)
	{
		String idList = getIdList(players);
		
		if(idList == null)
			return false;
		
		try
		{
			return deleteFromTable("ID NOT IN (" + idList + ")") > 0;
		}
		catch (SQLException e)
		{
			throw wrapInRuntimeException(e);
		}
	}

	@Override
	public boolean retainAll(Collection<?> players)
	{		
		String idList = getIdList(players);
		
		if(idList == null)
			return false;
		
		try
		{
			return deleteFromTable("ID NOT IN (" + idList + ")") > 0;
		}
		catch (SQLException e)
		{
			throw wrapInRuntimeException(e);
		}
	}
}

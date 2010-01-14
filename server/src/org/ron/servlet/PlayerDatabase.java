package org.ron.servlet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Set;

public class PlayerDatabase
extends AbstractDatabase<Player>
implements Set<Player>
{
	private final String SQLTABLENAME = "PLAYER";
	private final String SQLFIELDS = "ID,LAT,LNG,NAME,BITS";
	private final String SQLORDER = "ID ASC";
	private NodeDatabase _nodes;
	
	public PlayerDatabase(Connection connection)
	throws SQLException
	{
		super(connection);
		_nodes = new NodeDatabase(this);
	}
	
	public NodeDatabase getNodes()
	{
		return _nodes;
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
				"CREATE TABLE " + SQLTABLENAME + " " + 
				"(" +
    				"ID INTEGER PRIMARY KEY NOT NULL," +
    				"LAT REAL NULL," +
    				"LNG REAL NULL," +
    				"NAME TEXT NOT NULL," +
    				"BITS INTEGER NULL" +
    			")"
		);
	}
	
	protected int getPlayerId(ResultSet result)
	throws SQLException
	{
		return result.getInt(1);
	}
	
	private static final int RESULTSET = getUniqueRandom();
	
	protected ResultSet getResultSet(Player player)
	throws SQLException
	{
		PreparedStatement statement = getPreparedStatement
		(
			RESULTSET,
			"SELECT " + SQLFIELDS + " " +
			"FROM " + SQLTABLENAME + " " +
			"WHERE ID = ?"
		);
		
		ResultSet result = null;

		statement.setInt(1, player.getId());
		result = statement.executeQuery();
			
		if(result.next())
			return result;

		finallyCloseStatement(statement);
		throw new RuntimeException("FUCKUP");
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
		return result.getFloat(1);
	}
	
	protected float getLongitude(ResultSet result)
	throws SQLException
	{
		return result.getFloat(2);
	}
	
	protected Player get(ResultSet result)
	throws SQLException
	{
		return new Player(getPlayerId(result), this);
	}
	
	private static final int BIT_LOST = 1 << 0;
	
	public boolean hasLost(Player player)
	throws SQLException
	{
		return (getPlayerBits(player) & BIT_LOST) == BIT_LOST;
	}
	
	public void setLost(Player player, boolean set)
	throws SQLException
	{
		setPlayerBits(player, getPlayerBits(player) | BIT_LOST);
	}
	
	private static final int SELECTPLAYER = getUniqueRandom();
	
	protected ResultSet getResult(Player player)
	throws SQLException
	{
		PreparedStatement statement = getPreparedStatement
		(
			SELECTPLAYER,
			"SELECT " + SQLFIELDS + " " + 
			"FROM " + SQLTABLENAME + " " + 
			"WHERE ID = ?"
		);
		
		statement.setInt(1, player.getId());
		ResultSet result = statement.executeQuery();

		result.next();
		
		//TODO: check for null
		return result;
	}
	
	protected int getPlayerBits(Player player)
	throws SQLException
	{
		ResultSet result = null;
		
		try
		{
			result = getResult(player);
			return getPlayerBits(result);
		}
		finally
		{
			finallyCloseStatement(result);
		}
	}
	
	protected void setPlayerBits(Player player, int bits)
	{
		
	}
	
	public Player get(int playerId)
	throws SQLException
	{
		//TODO: check for existance
		return new Player(playerId, this);
	}
	
	protected int getPlayerBits(ResultSet result)
	throws SQLException
	{
		return result.getInt(4);
	}
	
	protected String getPlayerName(ResultSet result)
	throws SQLException
	{
		return result.getString(3);
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
	
	public void setPosition(Player player, float lat, float lng)
	throws SQLException
	{
		final int KEY = getUniqueRandom();
		
		PreparedStatement statement = getPreparedStatement
		(
			KEY,
			"UPDATE " + SQLTABLENAME + " " +
			"SET LAT = ?, LNG = ?" +
			"WHERE ID = ?"
		);
		
		try
		{
			statement.setFloat(1, lat);
			statement.setFloat(2, lng);
			statement.setInt(3, player.getId());
			statement.executeQuery();			
		}
		finally
		{
			finallyCloseStatement(statement);
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
		boolean autoCommit = getConnection().getAutoCommit();
		getConnection().setAutoCommit(false);
		boolean commited = false;
		ResultSet result = null;
		
		try
		{
			execute
			(
				"INSERT INTO " + SQLTABLENAME + " " +
				"(NAME, LAT, LNG) VALUES " +
				"('" + playerName + "'," + lat + "," + lng + ")"
			);
			
			int id;
			
			try
			{
				result = getWhere("WHERE NAME = '" + playerName + "'");
				id = getPlayerId(result);
			}
			finally
			{
				finallyCloseStatement(result);
			}
			
			commited = commit();
			return new Player(id, this);
		}
		finally
		{
			if(!commited)
				rollback();
			
			getConnection().setAutoCommit(autoCommit);
		}
	}

	public boolean addAll(Collection<? extends Player> arg0)
	{
		throw new UnsupportedOperationException();
	}
	
	public boolean contains(Object object)
	{
		Player player = (Player)object;
		
		if(player.getId() == null)
			return false; //needs id to be saved
		
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
	
	public boolean remove(Object object)
	{
		try
		{
			return deleteFromTable("ID = " + ((Player)object).getId()) > 0;
		}
		catch(SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
	}
	
	public boolean removeAll(Collection<?> arg0)
	{
		boolean changed = false;
		
		for(Object object : arg0)
		{
			changed = remove(object) || changed; 
		}
		
		return changed;
	}

	public boolean retainAll(Collection<?> arg0)
	{
		String playerIds = "";
	
		for(Object object : arg0)
		{
			if(!fromDatabase((Player)object))
				continue;
			
			playerIds += ((Player)object).getId() + ",";
		}
		
		if(playerIds.endsWith(","))
			playerIds = playerIds.substring(0, playerIds.length() - 2);
			
		try
		{
			int changes = execute("DELETE FROM " + SQLTABLENAME + " WHERE ID NOT LIKE (" + playerIds + ")");
			return changes > 0;
		}
		catch (SQLException e)
		{
			throw wrapInRuntimeException(e);
		}
	}
	
	protected boolean fromDatabase(Object object)
	{
		if(!(object instanceof Player))
			return false; //wrong object
		
		Player player = (Player)object;
		if(player.getId() == null)
			return false; //not in database yet
		
		return true;
	}
}

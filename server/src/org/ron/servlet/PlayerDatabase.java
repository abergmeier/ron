package org.ron.servlet;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Set;

import javax.servlet.http.HttpSession;

public class PlayerDatabase
extends AbstractDatabase<Player>
implements Set<Player>
{
	private final String SQLTABLENAME = "PLAYER";
	private final String SQLFIELDS = "ID,LAT,LNG,NAME";
	private final String SQLORDER = "ID";
	
	protected PlayerDatabase(HttpSession session)
	throws SQLException, ClassNotFoundException
	{
		super(session);
	}
	
	protected String getSQLFields()
	{
		return SQLFIELDS;
	}
	
	protected String getTableName()
	{
		return SQLTABLENAME;
	}
	
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
    				"NAME TEXT NOT NULL" +
    			")"
		);
	}
	
	protected int getPlayerId(ResultSet result)
	throws SQLException
	{
		return result.getInt(1);
	}
	
	protected ResultSet getResultSet(Player player)
	throws SQLException
	{
		Statement statement = getConnection().createStatement();
		
		ResultSet result = null;
		try
		{
			result = statement.executeQuery
			( 
				"SELECT " + SQLFIELDS + " " +
				"FROM " + SQLTABLENAME + " " +
				"WHERE ID = " + player.getId()
			);
			
			if(result.next())
				return result;
		}
		catch(SQLException exception)
		{
			finallyCloseStatement(statement);
			throw exception;
		}	
		
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
	
	public float getLongtitude(Player player)
	throws SQLException
	{
		ResultSet result = getResultSet(player);
		
		try
		{
			return getLongtitude(result);
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
	
	protected float getLongtitude(ResultSet result)
	throws SQLException
	{
		return result.getFloat(2);
	}
	
	protected Player get(ResultSet result)
	throws SQLException
	{
		return new Player(getPlayerId(result), this);
	}
	
	public Player get(int playerId)
	throws SQLException
	{
		Statement statement = getConnection().createStatement();
		
		try
		{
			ResultSet result = statement.executeQuery("SELECT " + SQLFIELDS + " FROM " + SQLTABLENAME + " WHERE ID = " + playerId);

			if(!result.next() || result.getString(1) == null)
				throw new IllegalArgumentException("Id not taken");
			
			return new Player(playerId, this);
		}
		finally
		{
			finallyCloseStatement(statement);
		}
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
		setPosition(player, position.getLatitude(), position.getLongtitude()); 
	}
	
	public void setPosition(Player player, float lat, float lng)
	throws SQLException
	{
		PreparedStatement statement = getConnection().prepareStatement("UPDATE PLAYER SET LAT = ?, LNG = ? WHERE ID = ?");
		
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
		execute
		(
			"INSERT INTO " + SQLTABLENAME + " " +
			"(NAME, LAT, LNG) VALUES " +
			"('" + playerName + "'," + lat + "," + lng + ")"
		);
		
		int id = getInt("SELECT ID FROM PLAYER WHERE NAME = '" + playerName + "'");
		return new Player(id, this);
	}

	public boolean addAll(Collection<? extends Player> arg0)
	{
		throw new UnsupportedOperationException();
	}
	
	public boolean contains(Player player)
	throws SQLException
	{
		if(player.getId() == null)
			return false; //needs id to be saved
		
		return getString
		(
			"SELECT ID " +
			"FROM " + SQLTABLENAME + " " +
			"WHERE ID = " + player.getId()
		) != null;
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
	
	protected ResultSet getAll()
	throws SQLException
	{
		Statement statement = getConnection().createStatement();
		
		return statement.executeQuery
		(
			"SELECT " + SQLFIELDS + " " +
			"FROM " + SQLTABLENAME + " " +
			"ORDER BY " + SQLORDER
		);
	}
	
	public boolean remove(Player player)
	throws SQLException
	{
		return deleteFromTable("ID = " + player.getId()) > 0;
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
			int changes = execute("DELETE FROM " + getTableName() + " WHERE ID NOT LIKE (" + playerIds + ")");
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

package org.ron.servlet;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
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
	throws SQLException
	{
		super(session);
	}
	
	protected String getTableName()
	{
		return SQLTABLENAME;
	}
	
	//creates a new database on the session if necessary 
	public static PlayerDatabase get(HttpSession session)
	throws SQLException
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
				"CREATE TABLE " + SQLTABLENAME + " "
				+ "("
    				+ "ID INTEGER PRIMARY KEY NOT NULL,"
    				+ "LAT REAL NULL,"
    				+ "LNG REAL NULL,"
    				+ "NAME TEXT NOT NULL"
    			+ ")"
		);
	}
	
	protected int getPlayerId(ResultSet result)
	throws SQLException
	{
		return result.getInt(0);
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
			
			if(!result.first())
			{
				try
				{
					statement.close();
				}
				catch(SQLException e)
				{
					//cannot do more than try
					//and log
					e.printStackTrace();
				}
				
				return null;
			}
			
			return result;
		}
		catch (SQLException e)
		{
			try
			{
				statement.close();
			}
			catch(SQLException closE)
			{
				//cannot do more than try
				//and log
				closE.printStackTrace();
			}
			
			throw e;
		}
	}
	
	public float getLatitude(Player player)
	throws SQLException
	{
		getLock().readLock().lock();
		ResultSet result = null;
		try
		{
			result = getResultSet(player);
			return getLatitude(result);
		}
		finally
		{
			getLock().readLock().unlock();
			if(result != null)
				result.getStatement().close();
		}
	}
	
	public float getLongtitude(Player player)
	throws SQLException
	{
		getLock().readLock().lock();
		ResultSet result = null;
		
		try
		{
			result = getResultSet(player);
			return getLongtitude(result);
		}
		finally
		{
			getLock().readLock().unlock();
			
			if(result != null)
				result.getStatement().close();
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
		getLock().readLock().lock();
		Statement statement = null;
		
		try
		{
			statement = getConnection().createStatement();
			ResultSet result = statement.executeQuery("SELECT " + SQLFIELDS + " FROM " + SQLTABLENAME + " WHERE ID = " + playerId);

			if(!result.first() || result.getString(0) == null)
				throw new IllegalArgumentException("Id not taken");
			
			return new Player(playerId, this);
		}
		finally
		{
			getLock().readLock().unlock();
			
			if(statement != null)
				statement.close();
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
		getLock().readLock().lock();
		
		ResultSet result = null;
		
		try
		{
			result = getResultSet(player);
			return getPlayerName(result);
		}
		finally
		{
			getLock().readLock().unlock();
			
			if(result != null)
				result.close();
		}
	}

	public void setPosition(Player player, Position position)
	throws SQLException
	{
		PreparedStatement statement = getConnection().prepareStatement("UPDATE PLAYER SET LAT = ?, LNG = ? WHERE ID = ?");
		
		try
		{
			statement.setFloat(0, position.getLatitude());
			statement.setFloat(1, position.getLongtitude());
			statement.setInt(2, player.getId());
			statement.executeQuery();			
		}
		finally
		{
			statement.close();
		}
	}

	@Override
	public void clear()
	{
		Statement statement = null;
		Savepoint save = null;
		
		try
		{
			save = getConnection().setSavepoint();
			
			statement = getConnection().createStatement();
			NodeDatabase nodes;
			nodes.clear();
			
			statement.execute("DELETE FROM " + getTableName());
			
			getConnection().releaseSavepoint(save);
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			
			if(save != null)
				getConnection().rollback(save);
		}
		finally
		{
			if(statement == null)
				return;
			
			try
			{
				statement.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}		
	}

	@Override
	public boolean isEmpty()
	{
		return size() == 0;
	}

	@Override
	public boolean add(Player player)
	{
		if(player.getId() != null && contains(player))
			return false; //already present
		
		String playerName;
		
		try
		{
			playerName = player.getName();
			execute("INSERT INTO " + getTableName() + " (NAME) VALUES ('" + playerName + "')");		
			player.setId(getInt("SELECT ID FROM PLAYER WHERE NAME = '" + playerName + "'"));
			return true;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public boolean addAll(Collection<? extends Player> arg0)
	{
		boolean changed = false;
		
		for(Player player : arg0)
		{
			changed = add(player) || changed;
		}
		
		return changed;
	}

	@Override
	public boolean contains(Object arg0)
	{
		try
		{
			return contains((Player)arg0);
		} 
		catch (SQLException e)
		{
			throw wrapInNullException(e);
		}
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

	@Override
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
	
	public boolean remove(Object object)
	{
		try
		{
			return remove((Player)object);
		} 
		catch (SQLException e)
		{
			throw new UnsupportedOperationException(e);
		}
	}
	
	public boolean remove(Player player)
	throws SQLException
	{
		return deleteFromTable("ID = " + player.getId()) > 0;
	}

	@Override
	public boolean removeAll(Collection<?> arg0)
	{
		boolean changed = false;
		
		for(Object object : arg0)
		{
			changed = remove(object) || changed; 
		}
		
		return changed;
	}

	@Override
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
			e.printStackTrace();
		}
		
		return false;
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

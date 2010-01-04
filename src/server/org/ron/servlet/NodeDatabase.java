package org.ron.servlet;

import java.lang.reflect.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import javax.servlet.http.HttpSession;
import javax.vecmath.Vector2f;

public class NodeDatabase
extends AbstractDatabase<Node>
implements Set<Node>
{
	private final String SQLTABLENAME = "NODE";
	private final String SQLFIELDS = "PLAYERID,LAT,LNG,TIME";
	private final String SQLORDER = "TIME ASC";
	
	protected NodeDatabase(HttpSession session)
	throws SQLException 
	{
		super(session);
	}
	
	public static NodeDatabase get(HttpSession session)
	throws SQLException
	{	
		final String KEY = "NodeDatabase";
		
		NodeDatabase instance = (NodeDatabase)session.getAttribute(KEY);
		
		if(instance == null)
		{
			instance = new NodeDatabase(session);
			session.setAttribute(KEY, instance);
		}
		
		return instance;
	} 
	
	protected String getTableName()
	{
		return SQLTABLENAME;
	}
	
	protected void createTable()
	throws SQLException
	{
		execute
		(
				"BEGIN;" +
				"CREATE TABLE \"" + SQLTABLENAME + "\" " +
				"(" +
					"\"PLAYERID\" INTEGER NOT NULL, " +
					"\"LAT\" REAL NOT NULL, " +
					"\"LNG\" REAL NOT NULL, " +
					"\"TIME\" INTEGER NOT NULL" +
				");" +
				"CREATE UNIQUE INDEX \"position-index\" on node (LAT ASC, LNG ASC);" +
				"COMMIT;"
		);
	}
	
	public NodeUpdate getNew(Player player, Calendar time)
	throws SQLException
	{
		PreparedStatement statement = getConnection().prepareStatement
		(
			"SELECT " + SQLFIELDS + " " +				
			"FROM " + SQLTABLENAME + " " +
			"WHERE " +
				"TIME >= ? " +
			 	"AND PLAYERID <> ?" +
			"ORDER BY PLAYERID, " + SQLORDER
		);
		
		Lock readLock = null;
		
		try
		{
			statement.setInt(0, time.get(Calendar.SECOND));
			statement.setInt(1, player.getId());
			
			readLock = getLock().readLock();
			readLock.lock();
			ResultSet result = statement.executeQuery();
			
			NodeUpdate update = new NodeUpdate();
			NodeUpdate.PlayerNodes playerNodes = null;
			
			int lastPlayerId = Integer.MIN_VALUE;
			int playerId;
			
			Position position;
			for(result.first(), update.setTime(result.getInt(3)); result.next();)
			{
				playerId = getPlayerId(result);
				
				if(lastPlayerId != playerId)
				{
					lastPlayerId = playerId;
					playerNodes = update.newPlayer(playerId);
				}
				
				position = get(result);
				playerNodes.addNode(position);			
			}
			
			return update;
		}
		finally
		{
			if(readLock != null)
				readLock.unlock();
			
			statement.close();
		}		
	}
	
	@Override
	public void clear()
	{
		try
		{
			deleteFromTable("1=1");
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public boolean add(Node element)
	{
		PreparedStatement statement = null;
		Lock writeLock = null;
		
		try
		{
			Player player = element.getPlayer();
			statement = getConnection().prepareStatement("INSERT INTO " + SQLTABLENAME + " (PLAYERID, LAT, LNG, TIME) VALUES (?, ?, ? , ?)");
			statement.setInt(0, player.getId());
			Position position = player.getPosition();
			statement.setFloat(1, position.getLatitude());
			statement.setFloat(2, position.getLongtitude());
			
			statement.setInt(3, Calendar.getInstance().get(Calendar.SECOND));
			
			writeLock = getLock().writeLock();
			writeLock.lock();
			statement.execute();
		}
		catch (SQLException e)
		{
			throw new IllegalArgumentException(e);
		}
		finally
		{
			if(writeLock != null)
				writeLock.unlock();
			
			if(statement != null)
				try
				{
					statement.close();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
		}
		
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends Node> c)
	{
		boolean changed = false;
		
		for(Node node : c)
		{
			changed = add(node) || changed;
		}
		
		return changed;
	}

	@Override
	public boolean contains(Object o)
	{
		try
		{
			return contains((Node)o);
		}
		catch (SQLException e)
		{
			throw wrapInNullException(e);
		}
	}
	
	public boolean contains(Node node)
	throws SQLException
	{
		return getString
		(
			"SELECT PLAYERID FROM " + SQLTABLENAME + " " +
			"WHERE " +
				"PLAYERID = " + node.getPlayer().getId() + "," +
				"LAT = " + node.getLatitude() + "," + 
				"LNG = " + node.getLongtitude()
		) == null;
	}

	@Override
	public boolean containsAll(Collection<?> c)
	{
		boolean changed = false;
		
		for(Object object : c)
		{
			try
			{
				changed = contains((Node)object) || changed;
			}
			catch (SQLException e)
			{
				throw wrapInNullException(e);
			}
		}
		
		return changed;
	}

	protected Node get(ResultSet result)
	{
		result.getInt(0);
		Player player;
		
		return new Node(player, getLatitude(result), getLongtitude(result);
	}
	
	protected int getPlayerId(ResultSet result)
	throws SQLException
	{
		getLock().readLock().lock();
		
		try
		{
			return result.getInt(0);
		}
		finally
		{
			getLock().readLock().unlock();
		}
	}
	
	protected float getLatitude(ResultSet result)
	throws SQLException
	{
		getLock().readLock().lock();
		
		try
		{
			return result.getFloat(1);
		}
		finally
		{
			getLock().readLock().unlock();
		}		
	}
	
	protected float getLongtitude(ResultSet result)
	throws SQLException
	{
		getLock().readLock().lock();
		
		try
		{
			return result.getFloat(2);
		}
		finally
		{
			getLock().readLock().unlock();
		}
	}
	
	/**
	 * Returns a Result set containing all Nodes
	 * Before use a lock has to be set
	 * @return
	 * @throws SQLException
	 */
	protected ResultSet getAll()
	throws SQLException
	{	
		Statement statement = getConnection().createStatement();
		
		ResultSet result = statement.executeQuery
		( 
			"SELECT " + SQLFIELDS + " " +
			"FROM " + SQLTABLENAME + " " +
			"ORDER BY " + SQLORDER
		);
		
		return result;
		//do not close the statement since the ResultSet
		//has to be accessed outside of method
	}
	
	public int indexOf(Node node)
	throws SQLException
	{
		getLock().readLock().lock();
		
		ResultSet result = null;
		
		try
		{
			result = getAll();
			 
			for(int i = 0; result.next(); i++)
			{
				if
				(
					getLatitude(result) == node.getLatitude()
					&& getLongtitude(result) == node.getLongtitude()
				)
				{
					if(getPlayerId(result) == node.getPlayer().getId())
						return i;
					else
						//since every coordinate is unique
						//there is no Node with another player
						break;
				}
			}
			
			return -1;
		}
		finally
		{
			getLock().readLock().unlock();
			if(result != null)
				result.getStatement().close();
		}
	}

	@Override
	public boolean isEmpty()
	{
		return size() == 0;
	}

	@Override
	public boolean remove(Object o)
	{
		try
		{
			return remove((Node)o);
		}
		catch (SQLException e)
		{
			throw new UnsupportedOperationException(e);
		}
	}
	
	public boolean remove(Node node)
	throws SQLException
	{
		return deleteFromTable
		(
			"PLAYERID = " + node.getPlayer().getId() + ", " +
			"LAT = " + node.getLatitude() + ", " +
			"LNG = " + node.getLongtitude()
		) > 0;
	}

	private String collectWhere(Collection<?> c)
	{
		Position position;
		
		if(c.size() == 0)
			return null;
		
		String[] pairs = new String[c.size()];
		
		Iterator<?> iterator = c.iterator();
		
		for(int i = 0; i != pairs.length; i++)
		{
			position = (Position)iterator.next();
			
			pairs[i] = "LAT=" + position.getLatitude() + " AND LNG=" + position.getLongtitude();
		}
			
		String where = "(" + pairs[0] + ")";
		
		for(int i = 1; i != pairs.length; i++)
		{
			where += " OR (" + pairs[i] + ")";
		}
		
		return where;
	}
	
	@Override
	public boolean removeAll(Collection<?> c)
	{
		try
		{
			return deleteFromTable(collectWhere(c)) > 0;
		}
		catch (SQLException e)
		{
			throw wrapInNullException(e);
		}
	}

	@Override
	public boolean retainAll(Collection<?> c)
	{
		String where = collectWhere(c);
		
		try
		{	
			return deleteFromTable("NOT (" + where + ")") > 0;
		}
		catch (SQLException e)
		{
			throw wrapInNullException(e);
		}
	}
}

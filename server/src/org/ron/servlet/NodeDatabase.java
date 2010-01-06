package org.ron.servlet;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import javax.servlet.http.HttpSession;

public class NodeDatabase
extends AbstractDatabase<Node>
implements Set<Node>
{
	private final String SQLTABLENAME = "NODE";
	private final String SQLFIELDS = "PLAYERID,LAT,LNG,TIME";
	private final String SQLORDER = "TIME ASC";
	
	private PlayerDatabase _players;
	
	protected NodeDatabase(HttpSession session, PlayerDatabase players)
	throws SQLException, ClassNotFoundException
	{
		super(session);
		_players = players;		
	}
	
	public static NodeDatabase get(HttpSession session)
	throws SQLException, ClassNotFoundException
	{	
		final String KEY = "NodeDatabase";
		
		NodeDatabase instance = (NodeDatabase)session.getAttribute(KEY);
		
		if(instance == null)
		{
			instance = new NodeDatabase(session, PlayerDatabase.get(session));
			session.setAttribute(KEY, instance);
		}
		
		return instance;
	}
	
	protected String getSQLFields()
	{
		return SQLFIELDS;
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
			statement.setInt(1, time.get(Calendar.SECOND));
			statement.setInt(2, player.getId());
			
			readLock = getLock().readLock();
			readLock.lock();
			ResultSet result = statement.executeQuery();
			
			NodeUpdate update = new NodeUpdate();
			NodeUpdate.PlayerNodes playerNodes = null;
			
			int lastPlayerId = Integer.MIN_VALUE;
			int playerId;
			
			Position position;
			for(result.next(), update.setTime(result.getInt(4)); result.next();)
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
	
	public boolean add(Player player, float lat, float lng)
	{
		return add(new Node(player, lat, lng));
	}

	public boolean add(Node element)
	{
		PreparedStatement statement = null;
		Lock writeLock = null;
		
		try
		{
			Player player = element.getPlayer();
			statement = getConnection().prepareStatement("INSERT INTO " + SQLTABLENAME + " (PLAYERID, LAT, LNG, TIME) VALUES (?, ?, ? , ?)");
			statement.setInt(1, player.getId());
			Position position = player.getPosition();
			statement.setFloat(2, position.getLatitude());
			statement.setFloat(3, position.getLongtitude());
			
			statement.setInt(4, Calendar.getInstance().get(Calendar.SECOND));
			
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

	public boolean addAll(Collection<? extends Node> c)
	{
		boolean changed = false;
		
		for(Node node : c)
		{
			changed = add(node) || changed;
		}
		
		return changed;
	}
	
	public boolean contains(Node node)
	{
		try
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
		catch(SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
	}

	public boolean containsAll(Collection<?> c)
	{
		boolean changed = false;
		
		for(Object object : c)
		{
			changed = contains((Node)object) || changed;
		}
		
		return changed;
	}

	protected Node get(ResultSet result)
	throws SQLException
	{
		return new Node(new Player(getPlayerId(result), _players), getLatitude(result), getLongtitude(result));
	}
	
	protected Node get(float lat, float lng)
	throws SQLException
	{
		ResultSet result = getWhere("LAT = " + lat + " AND LNG = " + lng);
		
		try
		{
			return get(result);	
		}
		finally
		{
			result.getStatement().close();
		}		
	}
	
	protected int getPlayerId(ResultSet result)
	throws SQLException
	{
		getLock().readLock().lock();
		
		try
		{
			return result.getInt(1);
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
			return result.getFloat(2);
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
			return result.getFloat(3);
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

	public boolean isEmpty()
	{
		return size() == 0;
	}

	public boolean remove(Node node)
	throws SQLException
	{
		return deleteFromTable
		(
			"PLAYERID = " + node.getPlayer().getId() + " AND " +
			"LAT = " + node.getLatitude() + " AND " +
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
	
	public Node[] toArray(Player player)
	throws SQLException
	{
		getLock().readLock().lock();
		ResultSet result = null;
		
		try
		{
			int size = size();
			Node[] nodes = new Node[size];
			
			result = getWhere("PLAYERID = " + player.getId());
			
			for(int i = 0; result.next(); i++)
			{
				nodes[i] = get(result);			
			}
			
			return nodes;
		}
		finally
		{
			if(result != null)
				result.getStatement().close();
			
			getLock().readLock().unlock();
		}
	}
}

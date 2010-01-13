package org.ron.servlet;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class NodeDatabase
extends AbstractDatabase<Node>
implements Set<Node>
{
	private static final String SQLTABLENAME = "NODE";
	private static final String SQLFIELDS = "PLAYERID,LAT,LNG,TIME";
	private static final String SQLORDER = "TIME ASC";
	
	private PlayerDatabase _players;
	private SegmentDatabase _segments;
	
	public NodeDatabase(PlayerDatabase players)
	throws SQLException
	{
		super(players.getConnection());
		_players = players;
		_segments = new SegmentDatabase(this);
	}
	
	public SegmentDatabase getSegments()
	{
		return _segments;
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
	
	protected void createTable()
	throws SQLException
	{
		execute
		(
				"BEGIN;" +
				"CREATE TABLE " + SQLTABLENAME + " " +
				"(" +
					"PLAYERID INTEGER NOT NULL, " +
					"LAT REAL NOT NULL, " +
					"LNG REAL NOT NULL, " +
					"TIME INTEGER NOT NULL" +
				");" +
				"CREATE UNIQUE INDEX \"position-index\" on node (LAT ASC, LNG ASC);" +
				"COMMIT;"
		);
	}	
	
	@Override
	public boolean add(Node element)
	{
		throw new UnsupportedOperationException("Node cannot be added"); 
	}

	private static final int PS_INSERTKEY = getUniqueRandom();
	private static final int PS_INSERTSELECTKEY = getUniqueRandom();
	
	public boolean addForPlayerPosition(Player player)
	throws SQLException
	{
		PreparedStatement statement = null;
		
		boolean autoCommit = getConnection().getAutoCommit();
		getConnection().setAutoCommit(false);
		boolean commited = false;
		
		try
		{
			statement = getPreparedStatement
			(
				PS_INSERTKEY,
				"INSERT INTO " + SQLTABLENAME + " " + 
				"(PLAYERID, LAT, LNG, TIME) VALUES " +
				"(?, ?, ? , ?)"
			);

			Position position = player.getPosition();
			float lat = position.getLatitude();
			float lng = position.getLongitude();
			
			synchronized(statement)
			{
				statement.setInt(1, player.getId());
				statement.setFloat(2, lat);
				statement.setFloat(3, lng);
				
				statement.setInt(4, Calendar.getInstance().get(Calendar.SECOND));
				statement.execute();
				
				statement.close(); //cleanup executed statement
			}
			
			statement = getPreparedStatement
			(
				PS_INSERTSELECTKEY,
				"SELECT TOP(" + SQLFIELDS + ") " +
				"FROM " + SQLTABLENAME + " " + 
				"WHERE " +
					"PLAYERID = ? AND " +
					"LAT <> ? AND " +
					"LNG <> ? " +
				"ORDER BY " + SQLORDER
			);
			
			synchronized(statement)
			{
				statement.setInt(1, player.getId());
				statement.setFloat(2, lat);
				statement.setFloat(3, lng);
				
				ResultSet result = statement.executeQuery();
				
				if(!result.next())
					return true; //we're done
				
				try
				{	
					lat = getLatitude(result);
				}
				catch(NullPointerException exception)
				{
					return true; //no row returned - done
				}
			
				//there's more than one node so create segments
				boolean returnValue = _segments.add(lat, getLatitude(result), position.getLatitude(), position.getLongitude());
				commited = commit();
				return returnValue;
			}
		}
		finally
		{
			if(!commited)
				rollback();
			
			finallyCloseStatement(statement);
			getConnection().setAutoCommit(autoCommit);
		}
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
	
	@Override
	public boolean contains(Object object)
	{
		Node node = (Node)object;
		try
		{
			return getString
			(
				"SELECT PLAYERID FROM " + SQLTABLENAME + " " +
				"WHERE " +
					"PLAYERID = " + node.getPlayer().getId() + "," +
					"LAT = " + node.getLatitude() + "," + 
					"LNG = " + node.getLongitude()
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
			finallyCloseStatement(result);
		}		
	}
	
	protected int getPlayerId(ResultSet result)
	throws SQLException
	{
		return result.getInt(1);
	}
	
	protected float getLatitude(ResultSet result)
	throws SQLException
	{
		return result.getFloat(2);
	}
	
	protected float getLongtitude(ResultSet result)
	throws SQLException
	{
		return result.getFloat(3);
	}
	
	public int indexOf(Node node)
	throws SQLException
	{
		ResultSet result = getAll();
		
		try
		{
			for(int i = 0; result.next(); i++)
			{
				if
				(
					getLatitude(result) == node.getLatitude()
					&& getLongtitude(result) == node.getLongitude()
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
			finallyCloseStatement(result);
		}
	}

	public boolean isEmpty()
	{
		return size() == 0;
	}

	@Override
	public boolean remove(Object object)
	{
		Node node = (Node)object;
		
		try
		{
			return deleteFromTable
			(
				"PLAYERID = " + node.getPlayer().getId() + " AND " +
				"LAT = " + node.getLatitude() + " AND " +
				"LNG = " + node.getLongitude()
			) > 0;
		}
		catch (SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
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
			
			pairs[i] = "LAT=" + position.getLatitude() + " AND LNG=" + position.getLongitude();
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
			throw wrapInRuntimeException(e);
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
			throw wrapInRuntimeException(e);
		}
	}
	
	public Node[] toArray(Player player)
	throws SQLException
	{
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
			finallyCloseStatement(result);			
		}
	}
}

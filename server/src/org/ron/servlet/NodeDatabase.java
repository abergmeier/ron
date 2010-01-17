package org.ron.servlet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class NodeDatabase
extends AbstractDatabase<Node>
implements Set<Node>
{
	public static final String SQLIDCOLUMN = "ID";
	public static final String SQLTABLENAME = "NODE";
	private static final String SQLFIELDS = "PLAYERID,LAT,LNG," + SQLIDCOLUMN;
	private static final String SQLORDER = "PLAYERID ASC";
	
	private PlayerDatabase _players;
	private SegmentDatabase _segments;
	
	public NodeDatabase(PlayerDatabase players)
	throws SQLException
	{
		super(players.getConnection());
		_players = players;
		_segments = new SegmentDatabase(this);
		
		setConnection(_players.getConnection());
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
	
	@Override
	public void setConnection(Connection connection)
	{
		if(_segments != null)
			_segments.setConnection(connection);
		
		super.setConnection(connection);
	}
	
	@Override
	protected void createTable()
	throws SQLException
	{
		
		execute
		(
				"CREATE TABLE IF NOT EXISTS " + SQLTABLENAME + " " +
				"(" +
					"PLAYERID INTEGER NOT NULL," +
					"LAT REAL NOT NULL, " +
					"LNG REAL NOT NULL," +
					"ID INTEGER PRIMARY KEY NOT NULL," +
					"FOREIGN KEY (PLAYERID) REFERENCES " + PlayerDatabase.SQLTABLENAME + "(" + PlayerDatabase.SQLIDCOLUMN + ")" +
				");" +
				"CREATE UNIQUE INDEX \"position-index\" on node (LAT, LNG);"
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
		
		Savepoint save = getConnection().setSavepoint();
		/*
		getConnection().setAutoCommit(false);
		boolean commited = false;
		*/
		
		try
		{
			statement = getPreparedStatement
			(
				PS_INSERTKEY,
				"INSERT INTO " + SQLTABLENAME + " " + 
				"(PLAYERID, LAT, LNG) VALUES " +
				"(?, ?, ?);"
			);

			Position position = player.getPosition();
			float lat = position.getLatitude();
			float lng = position.getLongitude();
			
			synchronized(statement)
			{
				statement.setInt(1, player.getId());
				statement.setFloat(2, lat);
				statement.setFloat(3, lng);
				statement.execute();
				
				statement.close(); //cleanup executed statement
			}
			
			//get the last 2 nodes
			statement = getPreparedStatement
			(
				PS_INSERTSELECTKEY,
				"SELECT " + SQLFIELDS + " " +
				"FROM " + SQLTABLENAME + " " + 
				"WHERE " +
					"PLAYERID = ? " +
				"ORDER BY ID DESC," + SQLORDER + " " +
				"LIMIT 2"				
			);
			
			synchronized(statement)
			{
				statement.setInt(1, player.getId());
				
				//check whether there are more than 1 node present
				ResultSet result = statement.executeQuery();
				
				try
				{
					if(!result.next())
						throw new SQLException("INSERT FAILED!?");
					
					//we have the current inserted
					Node endNode = get(result);
						
					if(!result.next())
						return true; //we're done
					
					Node startNode = get(result);
					
					try
					{	
						lat = getLatitude(result);
					}
					catch(NullPointerException exception)
					{
						return true; //no row returned - done
					}
				
					//there's more than one node so create segments
					boolean returnValue = _segments.add(startNode, endNode);
					//commited = commit();
					return returnValue;
				}
				finally
				{
					result.close();
				}
			}
		}
		catch(RuntimeException exception)
		{
			rollback(save);
			throw exception;
		}
		catch(SQLException exception)
		{
			rollback(save);
			throw exception;
		}
		finally
		{
			getConnection().releaseSavepoint(save);
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
		return get(getPlayerId(result), getNodeId(result), getLatitude(result), getLongtitude(result));
	}
	
	protected Node get(int playerId, int nodeId, float latitude, float longitude)
	throws SQLException
	{
		return new Node(_players.get(playerId), nodeId, latitude, longitude);
	}
	
	protected Node get(int id)
	throws SQLException
	{
		ResultSet result = getWhere("ID = " + id);
		
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
	
	protected int getNodeId(ResultSet result)
	throws SQLException
	{
		return result.getInt(4);
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

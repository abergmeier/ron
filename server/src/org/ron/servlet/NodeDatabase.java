package org.ron.servlet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Collection;
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
					"ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
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
	
	public Segment addForPlayerPosition(Player player)
	throws SQLException
	{
		Savepoint save = getConnection().setSavepoint();
		
		try
		{
			PreparedStatement statement = getPreparedStatement
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
					
					Node endNode;
					
					try
					{
						//we have the current inserted
						endNode = get(result);
					}
					catch(NullPointerException exception)
					{
						throw new SQLException("INSERT FAILED!?");
					}
					
					if
					(
						(endNode.getLatitude() == 0f && endNode.getLongitude() == 0f)
						|| (endNode.getLatitude() == Float.NaN && endNode.getLongitude() == Float.NaN)
					)
						throw new SQLException("SERIOUS FUCKUP!");
						
					if(!result.next())
						return null; //we're done
					
					try
					{
						Node startNode = get(result);
						
						//there's more than one node so create segment
						return _segments.add(startNode, endNode);
					}
					catch(NullPointerException exception)
					{
						//we're done
						//seems like it found just one entry
						return null;
					}
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
		return new NodeImpl(_players.get(playerId), nodeId, latitude, longitude);
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
		int value = result.getInt(1);
		if(result.wasNull())
			throw new NullPointerException("SQL NULL");
		
		return value;
	}
	
	protected float getLatitude(ResultSet result)
	throws SQLException
	{
		float value = result.getFloat(2);
		if(result.wasNull())
			throw new NullPointerException("SQL NULL");
		
		return value;
	}
	
	protected float getLongtitude(ResultSet result)
	throws SQLException
	{
		float value = result.getFloat(3);
		if(result.wasNull())
			throw new NullPointerException("SQL NULL");
		
		return value;
	}
	
	protected int getNodeId(ResultSet result)
	throws SQLException
	{
		int value = result.getInt(4);
		if(result.wasNull())
			throw new NullPointerException("SQL NULL");
		
		return value;
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
	
	public boolean remove(Player player)
	throws SQLException
	{
		Savepoint save = getConnection().setSavepoint();
				
		try
		{
			ResultSet result = getWhere("PLAYERID = " + player.getId());
			ArrayList<Node> nodes = new ArrayList<Node>();
			
			try
			{				
				while(result.next())
				{
					nodes.add(get(result));
				}
			}
			finally
			{
				result.close();
			}
			
			return removeAll(nodes);
		}
		finally
		{
			getConnection().releaseSavepoint(save);
		}
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
	
	@Override
	protected Collection<?> getDependentDatabase()
	{
		return _segments;
	}
	
	public boolean removeAll(Collection<?> c)
	{
		String idList = getIdList(c);
		
		if(idList == null)
			return false;
		
		Savepoint save;
		try
		{
			save = getConnection().setSavepoint();
		}
		catch (SQLException e1)
		{
			throw wrapInRuntimeException(e1);
		}
		
		try
		{
			_segments.removeAllNodes((Collection<Node>)c);
			return deleteFromTable("ID IN (" + idList + ")") > 0;
		}
		catch (SQLException e)
		{
			throw wrapInRuntimeException(e);
		}
		finally
		{
			try
			{
				getConnection().releaseSavepoint(save);
			}
			catch (SQLException e)
			{
				throw wrapInRuntimeException(e);
			}
		}

	}

	public boolean retainAll(Collection<?> c)
	{
		throw new UnsupportedOperationException();
		/*
		String idList = getIdList(c);
		
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
		*/
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

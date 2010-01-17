package org.ron.servlet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Calendar;
import java.util.Collection;

public class SegmentDatabase
extends AbstractDatabase<Segment>
{
	public static final String SQLIDCOLUMN = "ID";
	private static final String SQLFIELDS = SQLIDCOLUMN + ", STARTNODE, ENDNODE, TIME";
	public static final String SQLTABLENAME = "SEGMENT";
	private static final String SQLORDER = SQLIDCOLUMN + " ASC";
	
	private final NodeDatabase _nodes;
	private final ViewDatabase _views;
	
	public SegmentDatabase(NodeDatabase nodes)
	throws SQLException
	{
		super(nodes.getConnection());
		_nodes = nodes;
		
		_views = new ViewDatabase(this);
		
		setConnection(_nodes.getConnection());
	}
	
	@Override
	protected void createTable()
	throws SQLException
	{
		execute
		(
			"CREATE TABLE IF NOT EXISTS " + SQLTABLENAME + " " +
			"(" +
				"ID INTEGER PRIMARY KEY NOT NULL," +
				"STARTNODE INTEGER NOT NULL," + 
				"ENDNODE INTEGER NOT NULL," +
				"TIME INT NOT NULL," +
				"FOREIGN KEY (STARTNODE) REFERENCES " + NodeDatabase.SQLTABLENAME + "(" + NodeDatabase.SQLIDCOLUMN + ")," +
				"FOREIGN KEY (ENDNODE) REFERENCES " + NodeDatabase.SQLTABLENAME + "(" + NodeDatabase.SQLIDCOLUMN + ")" +
			")"
		);	
	}
	
	public ViewDatabase getViews()
	{
		return _views;
	}

	@Override
	protected Segment get(ResultSet result)
	throws SQLException
	{
		return new SegmentImpl
		(
			getSegmentId(result),
			_nodes.get(getSegmentStartNodeId(result)),
			_nodes.get(getSegmentEndNodeId(result)),
			getSegmentTime(result)
		);
	}
	
	protected Segment get(int segmentId, int playerid, int startNodeId, float startLatitude, float startLongitude, int endNodeId, float endLatitude, float endLongitude, int time)
	throws SQLException
	{
		return new SegmentImpl
		(
			segmentId,
			_nodes.get(playerid, startNodeId, startLatitude, startLongitude),
			_nodes.get(playerid, endNodeId, endLatitude, endLongitude),
			getSegmentTime(time)
		);
	}
	
	protected Segment get(int segmentId)
	throws SQLException
	{
		ResultSet result = getWhere("ID = " + segmentId);
		
		try
		{
			return get(result);
		}
		finally
		{
			result.close();
		}
	}
	
	protected int getSegmentId(ResultSet result)
	throws SQLException
	{
		return result.getInt(1);
	}
	
	private int getSegmentStartNodeId(ResultSet result)
	throws SQLException
	{
		return result.getInt(2);	
	}
	
	protected Calendar getSegmentTime(ResultSet result)
	throws SQLException
	{
		return getSegmentTime(result.getInt(4));
	}
	
	protected Calendar getSegmentTime(int timeS)
	throws SQLException
	{
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.SECOND, timeS);
		return calendar;
	}
	
	private int getSegmentEndNodeId(ResultSet result)
	throws SQLException
	{
		return result.getInt(3);
	}

	@Override
	protected String getSQLFields()
	{
		return SQLFIELDS;
	}

	@Override
	protected String getSQLTableName()
	{
		return SQLTABLENAME;
	}
	
	@Override
	protected String getSQLOrder()
	{
		return SQLORDER;
	}
	
	@Override
	public void setConnection(Connection connection)
	{
		if(_views != null)
			_views.setConnection(connection);
		
		super.setConnection(connection);
	}

	@Override
	public boolean add(Segment segment)
	{
		throw new UnsupportedOperationException();
	}
	
	private static final int PS_INSERT = getUniqueRandom();
	
	public boolean add(Node startNode, Node endNode)
	throws SQLException
	{
		return add(startNode.getId(), endNode.getId());
	}
	
	public boolean add(int startNodeId, int endNodeId)
	throws SQLException
	{
		Savepoint save = getConnection().setSavepoint();
		/*
		boolean autoCommit = getConnection().getAutoCommit();
		getConnection().setAutoCommit(false);
		*/
		
		PreparedStatement statement = null;
		//boolean commited = false;
		
		try
		{
			statement = getPreparedStatement
			(
				PS_INSERT,
				"INSERT INTO " + SQLTABLENAME + " " + 
				"(STARTNODE, ENDNODE, TIME) VALUES " + 
				"(?,?,?)"
			);
			
			synchronized(statement)
			{
				statement.setFloat(1, startNodeId);
				statement.setFloat(2, endNodeId);
				statement.setFloat(3, Calendar.getInstance().get(Calendar.SECOND));
						
				statement.executeUpdate();
			}
			
			//commited = commit();
			return true;
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
			/*
			if(!commited)
				rollback();
			
			getConnection().setAutoCommit(autoCommit);
			*/
			getConnection().releaseSavepoint(save);
		}
	}
	
	protected int getId(ResultSet result)
	throws SQLException
	{
		return result.getInt(1);
	}
	
	private static final int PS_GETPLAYER = getUniqueRandom();
	
	public Segment[] toArray(Player player, Calendar time)
	throws SQLException
	{	
		Savepoint save = getConnection().setSavepoint();
		
		try
		{
			int size = size(player);
			
			Segment[] segments = new Segment[size];
			
			PreparedStatement statement = getPreparedStatement
			(
				PS_GETPLAYER,
				"SELECT " + SQLFIELDS + " " +
				"FROM " + SQLTABLENAME + " " +
				"WHERE PLAYERID = ? AND TIME >= ?" + 
				"ORDER BY " + SQLORDER
			);
			
			synchronized(statement)
			{
				statement.setInt(1, player.getId());
				statement.setInt(2, time.get(Calendar.SECOND));
				
				ResultSet result = statement.executeQuery();
				
				try
				{
					for(int i = 0; result.next(); i++)
					{
						segments[i] = get(result);
					}
				}
				finally
				{
					result.close();
				}
			}
			
			return segments;
		}
		finally
		{
			getConnection().releaseSavepoint(save);
		}
	}

	private static final int PS_SIZE = getUniqueRandom();
	
	public int size(Player player)
	throws SQLException
	{
		PreparedStatement statement = getPreparedStatement
		(
			PS_SIZE,
			"SELECT COUNT(*) " +
			"FROM " + SQLTABLENAME + " " +
			"WHERE ENDNODE IN (SELECT " + NodeDatabase.SQLTABLENAME + "." + NodeDatabase.SQLIDCOLUMN + " FROM " + NodeDatabase.SQLTABLENAME + " WHERE PLAYERID = ?)"
		);
		
		synchronized(statement)
		{
			statement.setInt(1, player.getId());
			ResultSet result = statement.executeQuery();
			
			try
			{		
				result.next();
				
				return result.getInt(1);
			}
			finally
			{
				result.close();
			}
		}
	}

	@Override
	public boolean addAll(Collection<? extends Segment> segments)
	{
		boolean changed = false;
		
		for(Segment segment : segments)
		{
			changed = add(segment) || changed;
		}

		return changed;
	}

	private static final int PS_CONTAINS = getUniqueRandom();
	@Override
	public boolean contains(Object object)
	{
		Segment segment = (Segment)object;
		PreparedStatement statement = null;
		
		try
		{
			statement = getPreparedStatement
			(
				PS_CONTAINS,
				"SELECT " + SQLFIELDS + " " +
				"FROM " + SQLTABLENAME + " " +
				"WHERE " +
					"PLAYERID = ? AND " +
					"START_LAT = ? AND " +
					"START_LNG = ? AND " +
					"END_LAT = ? AND " +
					"END_LNG = ?" 
			);

			synchronized(statement)
			{
				statement.setInt(1, segment.getPlayer().getId());
				statement.setFloat(2, segment.getStart().getLatitude());
				statement.setFloat(3, segment.getStart().getLongitude());
				statement.setFloat(4, segment.getEnd().getLatitude());
				statement.setFloat(5, segment.getEnd().getLongitude());

				ResultSet result = statement.executeQuery();
				
				try
				{					
					if(!result.next())
						return false; //we don't have any result
					
					return result.getString(1) != null;
				}
				finally
				{
					result.close();
				}
			}
		}
		catch(SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
	}

	@Override
	public boolean containsAll(Collection<?> objects)
	{
		for(Object object : objects)
		{
			if(!contains(object))
				return false;
		}
		
		return true;
	}
	
	private String getWhere(Segment segment)
	{
		return
			"PLAYERID = " + segment.getPlayer().getId() + " AND " +
			"START_LAT = " + segment.getStart().getLatitude() + " AND " +
			"START_LNG = " + segment.getStart().getLongitude() + " AND " +
			"END_LAT = " + segment.getEnd().getLatitude() + " AND " +
			"END_LNG = " + segment.getEnd().getLongitude();			
	}

	@Override
	public boolean remove(Object object)
	{
		try
		{
			return deleteFromTable
			(
				getWhere((Segment)object)
			) > 0;
		}
		catch (SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
	}

	@Override
	public boolean removeAll(Collection<?> objects)
	{
		boolean changed = false;
		
		for(Object object : objects)
		{
			changed = remove((Segment)object) || changed;
		}
		
		return changed;
	}

	@Override
	public boolean retainAll(Collection<?> arg0)
	{
		throw new UnsupportedOperationException();
	}
}

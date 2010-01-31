package org.ron.servlet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
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
				"ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
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
	protected String getSQLIdColumn()
	{
		return SQLIDCOLUMN;
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
	
	public Segment add(Node startNode, Node endNode)
	throws SQLException
	{
		return add(startNode.getId(), endNode.getId());
	}
	
	public Segment add(int startNodeId, int endNodeId)
	throws SQLException
	{
		Savepoint save = getConnection().setSavepoint();
		
		try
		{
			PreparedStatement statement = getPreparedStatement
			(
				PS_INSERT,
				"INSERT INTO " + SQLTABLENAME + " " + 
				"(STARTNODE, ENDNODE, TIME) VALUES " + 
				"(?,?,?)"
			);
			
			synchronized(statement)
			{
				statement.setInt(1, startNodeId);
				statement.setInt(2, endNodeId);
				statement.setInt(3, Calendar.getInstance().get(Calendar.SECOND));
						
				statement.executeUpdate();
			}
			
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
	
	protected int getId(ResultSet result)
	throws SQLException
	{
		return result.getInt(1);
	}
	
	private static final int PS_GETPLAYER = getUniqueRandom();
	
	public Collection<Segment> getAll(Player player)
	throws SQLException
	{
		//make sure we get all updates
		return getAll(player, 0);
	}
	
	protected Collection<Segment> getAll(Player player, Calendar time)
	throws SQLException
	{
		return getAll(player, time.get(Calendar.SECOND));
	}
	
	protected Collection<Segment> getAll(Player player, int seconds)
	throws SQLException
	{	
		PreparedStatement statement = getPreparedStatement
		(
			PS_GETPLAYER,
			"SELECT " + SQLFIELDS + " " +
			"FROM " + SQLTABLENAME + " " +
			"WHERE ENDNODE IN (SELECT " + NodeDatabase.SQLTABLENAME + "." + NodeDatabase.SQLIDCOLUMN + " FROM " + NodeDatabase.SQLTABLENAME + " WHERE PLAYERID = ?) AND TIME >= ? " + 
			"ORDER BY " + SQLORDER
		);
			
		synchronized(statement)
		{
			statement.setInt(1, player.getId());
			statement.setInt(2, seconds);
			
			ArrayList<Segment> segments = new ArrayList<Segment>(size(player));
			
			Savepoint save = setSavepoint();
			
			try
			{				
				ResultSet result = statement.executeQuery();
			
				try
				{				
					while(result.next())
					{
						segments.add(get(result));
					}
				}
				finally
				{
					result.close();
				}
				
				return segments;
			}
			finally
			{
				releaseSavepoint(save);
			}
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
					"STARTNODE = ? AND " +
					"ENDNODE = ?"  
			);

			synchronized(statement)
			{
				statement.setInt(1, segment.getStart().getId());
				statement.setInt(2, segment.getEnd().getId());

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
			"STARTNODE = " + segment.getStart().getId() + " AND " +
			"ENDNODE = " + segment.getEnd().getId();			
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
	
	public boolean removeAllNodes(Collection<Node> nodes)
	throws SQLException
	{
		String idList = getIdList(nodes);
		
		if(idList == null)
			return false; //no element found
		
		Savepoint save = getConnection().setSavepoint();
		
		try
		{
			ResultSet result = getWhere
			(
				"STARTNODE IN (" + idList + ") OR " +
				"ENDNODE IN (" + idList + ")"
			);
			
			try
			{
				return removeAll(result);
			}
			finally
			{
				result.close();
			}	
		}
		finally
		{
			getConnection().releaseSavepoint(save);
		}
	}
	
	private boolean removeAll(ResultSet result)
	throws SQLException
	{
		ArrayList<Segment> segments = new ArrayList<Segment>();
		
		while(result.next())
		{			
			segments.add(get(result));
		}
		
		return removeAll(segments);
	}
	
	@Override
	protected Collection<?> getDependentDatabase()
	{
		return _views;
	}

	@Override
	public boolean removeAll(Collection<?> objects)
	{
		Collection<Segment> segments = (Collection<Segment>)objects;
		
		String idList = getIdList(segments);
		
		if(idList == null)
			return false;
		
		Savepoint save;
		try
		{
			save = setSavepoint();
		}
		catch(SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
		
		try
		{
			_views.removeAllSegments(segments);
			return deleteFromTable("ID IN (" + idList + ")") > 0;
		}
		catch(SQLException e)
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
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean retainAll(Collection<?> arg0)
	{
		throw new UnsupportedOperationException();
	}
}

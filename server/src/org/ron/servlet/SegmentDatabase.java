package org.ron.servlet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Calendar;
import java.util.Collection;

import javax.vecmath.Vector2f;

public class SegmentDatabase
extends AbstractDatabase<Segment>
{
	public static final String SQLIDCOLUMN = "ID";
	private static final String SQLFIELDS = SQLIDCOLUMN + ", PLAYERID, START_LAT, START_LNG, END_LAT, END_LNG, TIME";
	public static final String SQLTABLENAME = "SEGMENT";
	private static final String SQLORDER = SQLIDCOLUMN + " ASC";
	
	private final NodeDatabase _nodes;
	private final ViewDatabase _views;
	
	public SegmentDatabase(NodeDatabase nodes)
	throws SQLException
	{
		super(nodes.getConnection());
		_nodes = nodes;
		
		_views = new ViewDatabase(_nodes);
		
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
				"PLAYERID INTEGER NOT NULL," +
				"START_LAT REAL NOT NULL," + 
				"START_LNG REAL NOT NULL," +
				"END_LAT REAL NOT NULL," +
				"END_LNG REAL NOT NULL," +
				"TIME INT NOT NULL," +
				"FOREIGN KEY (PLAYERID) REFERENCES " + PlayerDatabase.SQLTABLENAME + "(" + PlayerDatabase.SQLIDCOLUMN + ")" +
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
		return new Segment(getSegmentId(result), getSegmentStart(result), getSegmentEnd(result), getSegmentTime(result));
	}
	
	protected int getSegmentId(ResultSet result)
	throws SQLException
	{
		return result.getInt(1);
	}
	
	protected Node getSegmentStart(ResultSet result)
	throws SQLException
	{
		return _nodes.get(result.getFloat(3), result.getFloat(4));
	}
	
	protected Calendar getSegmentTime(ResultSet result)
	throws SQLException
	{
		Calendar calendar = Calendar.getInstance();
		calendar.set(0, 0, 0, 0, 0, result.getInt(5));
		return calendar;
	}
	
	protected Node getSegmentEnd(ResultSet result)
	throws SQLException
	{
		return _nodes.get(result.getFloat(5), result.getFloat(6));
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
		Vector2f start = segment.getStart();
		Vector2f end = segment.getEnd();
		
		try
		{
			return add(getLatitude(start), getLongitude(start), getLatitude(end), getLongitude(end));
		}
		catch (SQLException e)
		{
			throw wrapInRuntimeException(e);
		}
	}
	
	private static final int PS_INSERT = getUniqueRandom();
	private static final int PS_INSERT_SELECT = getUniqueRandom();
	
	public boolean add(float startLat, float startLng, float endLat, float endLng)
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
				"(START_LAT, START_LNG, END_LAT, END_LNG, TIME) VALUES " + 
				"(?,?,?,?,?)"
			);
			
			synchronized(statement)
			{
				statement.setFloat(1, startLat);
				statement.setFloat(2, startLng);
				statement.setFloat(3, endLat);
				statement.setFloat(4, endLng);
				statement.setFloat(5, Calendar.getInstance().get(Calendar.SECOND));
						
				statement.executeUpdate();
			}
			
			statement = getPreparedStatement
			(
				PS_INSERT_SELECT,
				"SELECT " + SQLFIELDS + " " +
				"FROM " + SQLTABLENAME + " " +
				"WHERE " + 
					"START_LAT = ? AND " +
					"START_LNG = ? AND " +
					"END_LAT = ? AND " +
					"END_LNG = ?"
			);
			
			synchronized(statement)
			{		
				statement.setFloat(1, startLat);
				statement.setFloat(2, startLng);
				statement.setFloat(3, endLat);
				statement.setFloat(4, endLng);
				ResultSet result = statement.executeQuery();
				
				try
				{
					result.next();
				
					getId(result);
				}
				finally
				{
					result.close();
				}
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
			"SELECT COUNT(ID) " +
			"FROM " + SQLTABLENAME + " " +
			"WHERE PLAYERID = ?"
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

package org.ron.servlet;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.vecmath.Vector2f;

import org.ron.Collision;
import org.ron.PositionCollision;

public class ViewDatabase
extends AbstractDatabase<ViewSegment>
{
	private static final String SQLFIELDS = "PLAYERID, SEGMENTID, START_LAT, START_LNG, END_LAT, END_LNG";
	private static final String SQLTABLENAME = "SEGMENTVIEW";
	private static final String SQLORDER = "PLAYERID ASC, SEGMENTID ASC, START_LAT ASC";
	
	private final NodeDatabase _nodes;
	
	public ViewDatabase(NodeDatabase nodes)
	throws SQLException
	{
		super(nodes.getConnection());
		_nodes = nodes;
	}	

	public void getUpdate(ClientWriter writer, Player player, Segment[] segments)
	throws SQLException, PositionCollision
	{	
		Vector2f[] collidingVectors = new Vector2f[]{new Vector2f(), new Vector2f()}; 
		Vector2f[] buffer = new Vector2f[2];
		boolean succeeded;
		
		for(Segment segment : segments)
		{
			if(contains(player, segment))
				continue; //already at device
			
			//we actually have to test now
			succeeded = Collision.GetIntersections(player.getLatitude(), player.getLongitude(), segment.getStart(), segment.getEnd(), collidingVectors);
			
			if(!succeeded)
				continue; //no intersection
			
			buffer[0] = segment.getStart();
			buffer[1] = segment.getEnd();
			
			if(Arrays.equals(collidingVectors, buffer))
			{
				add(player, segment); //insert whole segment
				writer.add(segment);
			}
			else
			{
				add(player, segment, collidingVectors[0], collidingVectors[1]); //add partial segment
				writer.add(segment, collidingVectors[0], collidingVectors[1]);
			}			
		}
	}


	@Override
	protected void createTable()
	throws SQLException
	{
		execute
		(
			"CREATE TABLE " + SQLTABLENAME + " " +
			"(" +
				"PLAYERID INTEGER NOT NULL" +
				"SEGMENTID INTEGER NOT NULL," + 
				"START_LAT REAL NULL," +
				"START_LNG REAL NULL," +
				"END_LAT REAL NULL" +
				"END_LNG REAL NULL" +
			");"
		);
	}

	@Override
	protected ViewSegment get(ResultSet result)
	throws SQLException
	{
		return new ViewSegment
		(
			getSegmentId(result),
			getPlayerId(result),
			getStartNode(result),
			getEndNode(result)
		);
	}
	
	protected int getSegmentId(ResultSet result)
	throws SQLException
	{
		return result.getInt(2);
	}
	
	protected float getStartLatitude(ResultSet result)
	throws SQLException
	{
		return getFloat(result, 3);
	}
	
	protected float getStartLongitude(ResultSet result)
	throws SQLException
	{
		return getFloat(result, 4);
	}
	
	protected float getEndLatitude(ResultSet result)
	throws SQLException
	{
		return getFloat(result, 5);
	}
	
	protected float getEndLongitude(ResultSet result)
	throws SQLException
	{
		return getFloat(result, 6);
	}
	
	protected Node getStartNode(ResultSet result)
	throws SQLException
	{
		return _nodes.get(getStartLatitude(result), getStartLongitude(result));
	}
	
	protected Node getEndNode(ResultSet result)
	throws SQLException
	{
		return _nodes.get(getEndLatitude(result), getEndLongitude(result));
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
	public boolean add(ViewSegment segment)
	{
		Node start = segment.getSubStart();
		Node end = segment.getSubEnd();
		try
		{
			return add(segment.getPlayerId(), segment.getSegmentId(), start.getLatitude(), start.getLongitude(), end.getLatitude(), end.getLongitude());
		}
		catch (SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
	}
	
	protected boolean add(Player player, Segment segment)
	throws SQLException
	{
		return add(player.getId(), segment.getId(), Float.NaN, Float.NaN, Float.NaN, Float.NaN);
	}
	
	protected boolean add(Player player, Segment segment, Vector2f start, Vector2f end)
	throws SQLException
	{
		return add(player.getId(), segment.getId(), getLatitude(start), getLongitude(start), getLatitude(end), getLongitude(end));
	}
	
	private static final int PS_INSERT = getUniqueRandom();
	private static final int PS_INSERT_WHOLE = getUniqueRandom();
	
	protected boolean add(int playerId, int segmentId, float startLat, float startLng, float endLat, float endLng)
	throws SQLException
	{
		PreparedStatement statement;
		boolean isWhole;
		
		if
		(
			startLat == Float.NaN
			&& startLng == Float.NaN
			&& endLat == Float.NaN
			&& endLng == Float.NaN
		)
		{
			statement = getPreparedStatement
			(
				PS_INSERT_WHOLE,
				"INSERT INTO " + SQLTABLENAME + " " +
				"(PLAYERID, SEGMENTID) VALUES" +
				"(?, ?)"
			);
			isWhole = true;
		}
		else
		{
			statement = getPreparedStatement
			(
				PS_INSERT,
				"INSERT INTO " + SQLTABLENAME + " " +
				"(PLAYERID, SEGMENTID, START_LAT, START_LNG, END_LAT, END_LNG) VALUES" +
				"(?, ?, ?, ?, ?, ?)"
			);
			isWhole = false;
		}

		synchronized(statement)
		{
			statement.setInt(1, playerId);
			statement.setInt(2, segmentId);
		
			if(!isWhole)
			{
				//set partial segment
				statement.setFloat(2, startLat);
				statement.setFloat(3, startLng);
				statement.setFloat(4, endLat);
				statement.setFloat(5, endLng);
			}
			
			return statement.executeUpdate() > 0;
		}
	}

	@Override
	public boolean addAll(Collection<? extends ViewSegment> collection)
	{
		boolean added = false;
		
		for(ViewSegment segment : collection)
		{
			added = add(segment) || added;
		}
		
		return added;
	}
	
	@Override
	public boolean contains(Object object)
	{
		ViewSegment segment = (ViewSegment)object;
		try
		{
			return contains(segment.getPlayerId(), segment.getSegmentId());
		}
		catch (SQLException e)
		{
			throw wrapInRuntimeException(e);
		}
	}
	
	protected boolean contains(Player player, Segment segment)
	throws SQLException
	{
		return contains(player.getId(), segment.getId());
	}
	
	private static final int PS_CONTAINS = getUniqueRandom();
	
	protected boolean contains(int playerId, int segmentId)
	throws SQLException
	{
		PreparedStatement statement = getPreparedStatement
		(
			PS_CONTAINS,
			"SELECT " + SQLFIELDS + " " +
			"FROM " + SQLTABLENAME + " " +
			"WHERE " +
				"PLAYERID = ? AND SEGMENTID = ?"
		);
		
		try
		{
			statement.setInt(1, playerId);
			statement.setInt(2, segmentId);
			
			ResultSet result = statement.executeQuery();
			
			if(!result.next())
				return false;
			
			//TODO: check for NULL!
			
			//when latitudes are not set - the whole segment
			//is in view
			
			return getStartLatitude(result) == Float.NaN
			&& getStartLongitude(result) == Float.NaN
			&& getEndLatitude(result) == Float.NaN
			&& getEndLongitude(result) == Float.NaN;
		}
		finally
		{
			finallyCloseStatement(statement);
		}		
	}

	@Override
	public boolean containsAll(Collection<?> collection)
	{
		if(collection.size() == 0)
			return true;
		
		int size = 0;
		
		try
		{
			ResultSet result = getWhere(buildWhere((Collection<ViewSegment>)collection));
			
			for(; result.next(); size++)
			{
			}
		}
		catch(SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
		
		return size == collection.size();
	}

	@Override
	public boolean isEmpty()
	{
		return size() == 0;
	}

	private String getWhere(ViewSegment segment)
	{
		return getWhere(segment.getPlayerId(), segment.getSegmentId(), segment.getSubStart(), segment.getSubEnd());
	}

	private String getWhere(int playerId, int segmentId, Vector2f start, Vector2f end)
	{
		String where = 
		"(" +
			"PLAYERID = " + playerId + " " +
			"SEGMENTID = " + segmentId + " ";
		
		if(start != null)
		{
			where +=
				"AND START_LAT = " + getLatitude(start) + " " +
				"AND START_LNG = " + getLongitude(start) + " ";
		}
		
		if(end != null)
		{
			where +=
				"AND END_LAT = " + getLatitude(end) + " " +
				"AND END_LNG = " + getLongitude(end);
		}
		
		where += ")";
		
		return where;
	}

	private String buildWhere(Collection<ViewSegment> collection)
	{
		Iterator<ViewSegment> iterator = collection.iterator();
		
		if(!iterator.hasNext())
			return null;
		
		ViewSegment segment = iterator.next();
		
		String result = getWhere(segment);
			
		while(iterator.hasNext())
		{
			result += "OR " + getWhere(iterator.next());
		}
		
		return result;
	}

	@Override
	public boolean remove(Object object)
	{
		ViewSegment segment = (ViewSegment)object;
		
		try
		{
			return deleteFromTable(getWhere(segment)) > 0;
		}
		catch(SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
	}

	@Override
	public boolean removeAll(Collection<?> collection)
	{
		try
		{
			return deleteFromTable(buildWhere((Collection<ViewSegment>)collection)) > 0;
		}
		catch(SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
	}

	protected int getPlayerId(ResultSet result)
	throws SQLException
	{
		return result.getInt(1);
	}

	@Override
	public boolean retainAll(Collection<?> collection)
	{
		try
		{
			return deleteFromTable("NOT (" + buildWhere((Collection<ViewSegment>)collection) + ")") > 0;
		}
		catch(SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
	}
}
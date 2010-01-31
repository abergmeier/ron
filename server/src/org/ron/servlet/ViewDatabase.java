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
	private static final String SQLORDER = "PLAYERID ASC, SEGMENTID ASC, START_LAT ASC, START_LNG ASC";
	
	private static final float RADIUS = 50;
	
	private final SegmentDatabase _segments;
	
	public ViewDatabase(SegmentDatabase segments)
	throws SQLException
	{
		super(segments.getConnection());
		_segments = segments;
	}
	
	private float calculateSightRadius(Vector2f posVector, float meters)
	{
		return 1f / 111.3f / 1000f * meters; 
	}

	public void getUpdate(ClientWriter writer, Player player, Segment[] segments)
	throws SQLException, PositionCollision
	{	
		Vector2f[] collidingVectors = new Vector2f[]{new Vector2f(), new Vector2f()}; 
		Vector2f[] buffer = new Vector2f[2];
		boolean succeeded;
		
		Vector2f posVector = new Vector2f(player.getPosition().getLatitude(), player.getPosition().getLongitude());
		float radius = calculateSightRadius(posVector, RADIUS);
		
		for(Segment segment : segments)
		{
			if(segment == null)
				continue;
			
			if(contains(player, segment))
				continue; //already at device
			
			//we actually have to test now
			succeeded = Collision.GetIntersections(posVector, radius, segment.getStart().toVector(), segment.getEnd().toVector(), collidingVectors);
			
			if(!succeeded)
				continue; //no intersection or player already knows
			
			buffer[0] = segment.getStart().toVector();
			buffer[1] = segment.getEnd().toVector();
			
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
			"CREATE TABLE IF NOT EXISTS " + SQLTABLENAME + " " +
			"(" +
				"PLAYERID INTEGER NOT NULL," +
				"SEGMENTID INTEGER NOT NULL," + 
				"START_LAT REAL NULL," +
				"START_LNG REAL NULL," +
				"END_LAT REAL NULL," +
				"END_LNG REAL NULL," +
				"FOREIGN KEY (PLAYERID) REFERENCES " + PlayerDatabase.SQLTABLENAME + "(" + PlayerDatabase.SQLIDCOLUMN + ")," +
				"FOREIGN KEY (SEGMENTID) REFERENCES " + SegmentDatabase.SQLTABLENAME + "(" + SegmentDatabase.SQLIDCOLUMN + ")" +
			");"
		);
	}

	@Override
	protected ViewSegment get(ResultSet result)
	throws SQLException
	{
		return new ViewSegmentImpl
		(
			getSegment(result),
			getPlayerId(result),
			getStartLatitude(result),
			getStartLongitude(result),
			getEndLatitude(result),
			getEndLongitude(result)
		);
	}
	
	protected Segment getSegment(ResultSet result)
	throws SQLException
	{
		return _segments.get(getSegmentId(result));
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
	
	@Override
	protected String getSQLIdColumn()
	{
		throw new UnsupportedOperationException();
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
		throw new UnsupportedOperationException();
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
				statement.setFloat(3, startLat);
				statement.setFloat(4, startLng);
				statement.setFloat(5, endLat);
				statement.setFloat(6, endLng);
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
			return contains(segment.getPlayerId(), segment.getSegment().getId());
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
		
		synchronized(statement)
		{
			statement.setInt(1, playerId);
			statement.setInt(2, segmentId);

			ResultSet result = statement.executeQuery();
			
			try
			{
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
				result.close();
			}
		}
	}

	@Override
	public boolean containsAll(Collection<?> collection)
	{
		if(collection.size() == 0)
			return true;
		
		int size = 0;
		
		ResultSet result = null;
		try
		{
			result = getWhere(buildWhere((Collection<ViewSegment>)collection));
			
			for(; result.next(); size++)
			{
			}
		}
		catch(SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
		finally
		{
			finallyCloseStatement(result);
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
		return getWhere(segment.getPlayerId(), segment.getSegment().getId(), segment.getSubStartLatitude(), segment.getSubStartLongitude(), segment.getSubEndLatitude(), segment.getSubEndLongitude());
	}

	private String getWhere(int playerId, int segmentId, float startLat, float startLng, float endLat, float endLng)
	{
		String where = 
		"(" +
			"PLAYERID = " + playerId + " " +
			"SEGMENTID = " + segmentId + " ";
		
		if(startLat != Float.NaN && startLng != Float.NaN)
		{
			where +=
				"AND START_LAT = " + startLat + " " +
				"AND START_LNG = " + startLng + " ";
		}
		
		if(endLat != Float.NaN && endLng != Float.NaN)
		{
			where +=
				"AND END_LAT = " + endLat + " " +
				"AND END_LNG = " + endLng;
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
	
	public boolean removeAllSegments(Collection<Segment> segments)
	throws SQLException
	{
		String idList = getIdList(segments);
		
		if(idList == null)
			return false;
		
		return deleteFromTable("SEGMENTID IN (" + idList + ")") > 0;
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

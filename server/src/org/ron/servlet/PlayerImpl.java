package org.ron.servlet;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;

import javax.vecmath.Vector2f;

import org.ron.Collision;
import org.ron.PositionCollision;


public class PlayerImpl
implements Player
{
	private final class PositionWrapper
	implements Position
	{
		private final Player _player;
		
		public PositionWrapper(Player player)
		{
			_player = player;
		}
		
		@Override
		public boolean equals(Object object)
		{
			if(object instanceof Position)
			{
				Position position = (Position)object;
				
				return
					position.getLatitude() == getLatitude()
					&& position.getLongitude() == getLongitude();
			}
			
			return false;
		}

		@Override
		public float getLatitude()
		{
			try
			{
				return _database.getLatitude(_player);
			}
			catch (SQLException e)
			{
				throw wrapInRuntimeException(e);
			}	
		}

		@Override
		public float getLongitude()
		{
			try
			{
				return _database.getLongitude(_player);
			}
			catch (SQLException e)
			{
				throw wrapInRuntimeException(e);
			}
		}
	}
	
	private final Position _position = new PositionWrapper(this);
	
	private int _id;
	private PlayerDatabase _database;
	private String _name;

	public PlayerImpl(int id, PlayerDatabase players)
	{
		_database = players;
		_id = id;
	}
	
	@Override
	public boolean equals(Object object)
	{
		if(!(object instanceof Player))
			return false;
		
		return getId() == ((Player)object).getId();
	}

	public int getId()
	{
		return _id;
	}
	
	public void setId(int value)
	{
		_id = value;
	}
	
	public String getName()
	{
		try
		{
			if(_name == null)
				_name = _database.getPlayerName(this);
			
			return _name;			
		}
		catch(SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
	}

	public void setPosition(Position position)
	throws SQLException
	{
		_database.setPosition(this, position);		
	}
	
	public void setPosition(float lat, float lng)
	{
		try
		{
			_database.setPosition(this, lat, lng);
		}
		catch(SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
	}
	
	public Position getPosition()
	{
		return _position;
	}
	
	public boolean hasLost()
	{
		try
		{
			return _database.hasLost(this);
		}
		catch(SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
	}
	
	public Collection<Segment> getSegments()
	{
		try
		{
			return _database.getNodes().getSegments().getAll(this);
		}
		catch(SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
	}
	
	public Collection<Segment> getSegments(Calendar time)
	{
		try
		{
			return _database.getNodes().getSegments().getAll(this, time);
		}
		catch(SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
	}
	
	public void testCollision(Collection<Segment> segments)
	throws PositionCollision
	{
		Position position = getPosition();
		
		Segment lastSegment;
		try
		{
			lastSegment = _database.getNodes().getSegments().getLastInserted(this);
		}
		catch (SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
		
		if(lastSegment != null)
		{
			if(position.equals(lastSegment.getEnd()))
				return; //player hasn't moved since setting last node
		}
		
		Vector2f posVector = new Vector2f(position.getLatitude(), position.getLongitude());
		
		for(Segment segment : segments)
		{
			Collision.testCollision(posVector, segment.getStart().toVector(), segment.getEnd().toVector());
		}
	}
	
	public void getUpdate(ClientWriter writer, Collection<Segment> segments)
	throws PositionCollision
	{
		try
		{
			_database.getNodes().getSegments().getViews().getUpdate(writer, this, segments);
		}
		catch(SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
	}
	
	private RuntimeException wrapInRuntimeException(SQLException exception)
	{
		return SQLiteConnection.wrapInRuntimeException(exception);
	}
}

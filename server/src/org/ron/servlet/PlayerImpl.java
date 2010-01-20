package org.ron.servlet;

import java.sql.SQLException;
import java.util.Calendar;

import org.ron.PositionCollision;


public class PlayerImpl
implements Player
{
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
		return this;
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

	public float getLatitude()
	{
		
		try
		{
			return _database.getLatitude(this);
		}
		catch (SQLException e)
		{
			throw wrapInRuntimeException(e);
		}
	}

	public float getLongitude()
	{
		try
		{
			return _database.getLongitude(this);
		}
		catch (SQLException e)
		{
			throw wrapInRuntimeException(e);
		}
	}
	
	public Segment[] getSegments(Calendar time)
	{
		try
		{
			return _database.getNodes().getSegments().toArray(this, time);
		}
		catch(SQLException exception)
		{
			throw wrapInRuntimeException(exception);
		}
	}
	
	public void getUpdate(ClientWriter writer, Segment[] segments)
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

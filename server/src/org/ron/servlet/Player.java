package org.ron.servlet;

import java.sql.SQLException;
import java.util.Calendar;

import org.ron.PositionCollision;


public class Player
implements Position
{
	private Integer _id;
	private PlayerDatabase _database;
	private String _name;

	public Player(int id, PlayerDatabase players)
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

	public Integer getId()
	{
		return _id;
	}
	
	public void setId(int value)
	{
		_id = value;
	}
	
	public String getName()
	throws SQLException
	{
		if(_name == null)
			_name = _database.getPlayerName(this);
		
		return _name;			
	}

	public void setPosition(Position position)
	throws SQLException
	{
		_database.setPosition(this, position);		
	}
	
	public void setPosition(float lat, float lng)
	throws SQLException
	{
		_database.setPosition(this, lat, lng);
	}
	
	public Position getPosition()
	{
		return this;
	}
	
	public boolean hasLost()
	throws SQLException
	{
		return _database.hasLost(this);
	}

	public float getLatitude()
	{
		
		try
		{
			return _database.getLatitude(this);
		}
		catch (SQLException e)
		{
			throw new IllegalStateException(e);
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
			throw new IllegalStateException(e);
		}
	}
	
	public Segment[] getSegments(Calendar time)
	throws SQLException
	{
		return _database.getNodes().getSegments().toArray(this, time);
	}
	
	public void getUpdate(ClientWriter writer, Segment[] segments)
	throws SQLException, PositionCollision
	{
		_database.getNodes().getSegments().getViews().getUpdate(writer, this, segments);
	}
}

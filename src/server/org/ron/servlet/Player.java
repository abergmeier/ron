package org.ron.servlet;

import java.sql.SQLException;


public class Player
implements Position
{
	private Integer _id;
	private PlayerDatabase _database;
	private String _name;
	
	public Player(String name, PlayerDatabase database)
	{
		this(database);
		_name = name;		
	}
	
	public Player(int id, PlayerDatabase database)
	{
		this(database);
		_id = id;		
	}
	
	private Player(PlayerDatabase database)
	{
		_database = database;
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

	@Override
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

	@Override
	public float getLongtitude()
	{
		try
		{
			return _database.getLongtitude(this);
		}
		catch (SQLException e)
		{
			throw new IllegalStateException(e);
		}
	}
}

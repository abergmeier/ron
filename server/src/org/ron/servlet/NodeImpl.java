package org.ron.servlet;

import javax.vecmath.Vector2f;

public class NodeImpl
extends Vector2f
implements Node
{
	private static final long serialVersionUID = -9211379559969796712L;
	private Player _player = null;
	private final int _id;

	public NodeImpl(Player player, int id, float lat, float lng)
	{
		super(lat, lng);
		_player = player;
		_id = id;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public Player getPlayer()
	{
		return _player;
	}
	
	public float getLatitude()
	{
		return getX();
	}
	
	public float getLongitude()
	{
		return getY();
	}
	
	public Vector2f toVector()
	{
		return this;
	}
}

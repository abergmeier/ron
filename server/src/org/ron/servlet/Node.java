package org.ron.servlet;

import javax.vecmath.Vector2f;

public class Node
extends Vector2f
implements Position
{
	private static final long serialVersionUID = -9211379559969796712L;
	private Player _player = null;
	private final int _id;

	public Node(Player player, int id, float lat, float lng)
	{
		super(lat, lng);
		_player = player;
		_id = id;
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
}

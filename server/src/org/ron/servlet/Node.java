package org.ron.servlet;

import javax.vecmath.Vector2f;

public class Node
extends Vector2f
implements Position
{
	private static final long serialVersionUID = -9211379559969796712L;
	private Player _player = null; 

	public Node(Player player, float lat, float lng)
	{
		super(lat, lng);
		_player = player;
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

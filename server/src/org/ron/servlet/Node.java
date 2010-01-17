package org.ron.servlet;

import javax.vecmath.Vector2f;

public interface Node
extends Position
{
	public int getId();
		
	public Player getPlayer();
	
	public Vector2f toVector();
}

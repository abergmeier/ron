package org.ron.servlet;

import org.ron.PositionCollision;

public interface Player
extends Position, IntegerIdObject
{
	public String getName();

	public Position getPosition();
	
	public void setPosition(float lat, float lng);
	
	public boolean hasLost();

	public float getLatitude();

	public float getLongitude();
	
	public Segment[] getSegments();
	
	public void testCollision(Segment[] segments)
	throws PositionCollision;
	
	/**
	 * Test for collisions and writes updated info to clients
	 * @param writer
	 * @param segments
	 * @throws PositionCollision
	 */
	public void getUpdate(ClientWriter writer, Segment[] segments)
	throws PositionCollision;
}

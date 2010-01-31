package org.ron.servlet;

import java.util.Collection;

import org.ron.PositionCollision;

public interface Player
extends IntegerIdObject
{
	public String getName();

	public Position getPosition();
	
	public void setPosition(float lat, float lng);
	
	public boolean hasLost();
	
	public Collection<Segment> getSegments();
	
	public void testCollision(Collection<Segment> segments)
	throws PositionCollision;
	
	/**
	 * Test for collisions and writes updated info to clients
	 * @param writer
	 * @param segments
	 * @throws PositionCollision
	 */
	public void getUpdate(ClientWriter writer, Collection<Segment> segments)
	throws PositionCollision;
}

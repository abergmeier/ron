package org.ron.servlet;

import java.util.Calendar;

import org.ron.PositionCollision;

public interface Player
extends Position
{
	public Integer getId();
	
	public String getName();

	public Position getPosition();
	
	public void setPosition(float lat, float lng);
	
	public boolean hasLost();

	public float getLatitude();

	public float getLongitude();
	
	public Segment[] getSegments(Calendar time);
	
	public void getUpdate(ClientWriter writer, Segment[] segments)
	throws PositionCollision;
}

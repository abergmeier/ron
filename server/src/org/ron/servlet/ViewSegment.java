package org.ron.servlet;

public interface ViewSegment
{
	public Segment getSegment();
	
	public int getPlayerId();
	
	public Player getOwner();

	public void set(float subStartLatitude, float subStartLongitude, float subEndLatitude, float subEndLongitude);
	
	public float getSubStartLatitude();
	
	public float getSubStartLongitude();
	
	public float getSubEndLatitude();
	
	public float getSubEndLongitude();
}

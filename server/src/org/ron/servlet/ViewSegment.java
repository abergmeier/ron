package org.ron.servlet;

public class ViewSegment
{
	private Segment _segment;
	private float _subStartLatitude;
	private float _subStartLongitude;
	private float _subEndLatitude;
	private float _subEndLongitude;
	
	private int _playerId;
	
	public ViewSegment(Segment segment, int playerId, float startLatitude, float startLongitude, float endLatitude, float endLongitude)
	{
		_segment = segment;
		_playerId = playerId;
		
		set(startLatitude, startLongitude, endLatitude, endLongitude);
	}
	
	public Segment getSegment()
	{
		return _segment;
	}
	
	public int getPlayerId()
	{
		return _playerId;
	}
	
	public Player getOwner()
	{
		return _segment.getPlayer();
	}

	public void set(float subStartLatitude, float subStartLongitude, float subEndLatitude, float subEndLongitude)
	{
		_subStartLatitude = subStartLatitude;
		_subStartLongitude = subStartLongitude;
		_subEndLatitude = subEndLatitude;
		_subEndLongitude = subEndLongitude;
	}
	
	public float getSubStartLatitude()
	{
		return _subStartLatitude;
	}
	
	public float getSubStartLongitude()
	{
		return _subStartLongitude;
	}
	
	public float getSubEndLatitude()
	{
		return _subEndLatitude;
	}
	
	public float getSubEndLongitude()
	{
		return _subEndLongitude;
	}
}

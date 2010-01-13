package org.ron.servlet;

public class ViewSegment
{
	private int _segmentId;
	private Node _subStart;
	private Node _subEnd;
	
	private int _playerId;
	
	public ViewSegment(int segmentId, int playerId, Node subStart, Node subEnd)
	{
		_segmentId = segmentId;
		_playerId = playerId;
		
		set(subStart, subEnd);
	}
	
	public ViewSegment(Segment segment, int playerId, Node subStart, Node subEnd)
	{
		this(segment.getId(), playerId, subStart, subEnd);
	}
	
	public int getSegmentId()
	{
		return _segmentId;
	}
	
	public int getPlayerId()
	{
		return _playerId;
	}
	
	public Player getOwner()
	{
		return _subStart.getPlayer();
	}

	public void set(Node subStart, Node subEnd)
	{
		_subStart = subStart;
		_subEnd = subEnd;
	}
	
	public Node getSubStart()
	{
		return _subStart;
	}
	
	public Node getSubEnd()
	{
		return _subEnd;
	}
}

package org.ron.servlet;

import javax.vecmath.Vector2f;

public class ClientWriter
{
	private Player _player = null;
	private int _segmentId = Integer.MIN_VALUE;
	private Vector2f _start;
	private Vector2f _end;
	private boolean _isPartial;
	
	private String _output = "";
	
	public ClientWriter()
	{
	}
	
	public void setPlayer(Player player)
	{
		if(player != _player && _segmentId == Integer.MIN_VALUE)
		{
			_output +=
				"<player id=\"" + _player.getId() + "\">\n" +
					"<" + (_isPartial ? "partial" : "segment") + " id=\"" + _segmentId + "\">\n" +
						"<start lat=\"" + _start.getX() + "\" lng=\"" + _start.getY() + "\"/>\n" +
						"<end lat=\"" + _end.getX() + "\" lng=\"" + _end.getY() + "\"/>\n" +
					"</segment>\n" +
				"</player>\n";
		}
		
		_player = player;
	}
	
	public void add(Segment segment)
	{
		_isPartial = false;
		_segmentId = segment.getId();
		_start = segment.getStart();
		_end = segment.getEnd();		
	}
	
	public void add(Segment segment, Vector2f start, Vector2f end)
	{
		_isPartial = true;
		_segmentId = segment.getId();
		_start = start;
		_end = end;
	}
	
	public String close()
	{
		setPlayer(null);
		return _output;
	}
}

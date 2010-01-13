package org.ron.servlet;

import java.util.Calendar;

public class Segment
{
	private Node _start;
	private Node _end;
	private  int _id;
	private Calendar _time;
	
	public Segment(int id, Node start, Node end, Calendar time)
	{
		set(id, start, end, time);
	}
	
	public void set(int id, Node start, Node end, Calendar time)
	{
		if(start.getPlayer().equals(end.getPlayer()))
			throw new IllegalArgumentException("Segment nodes need the belong to the same player");
		
		_id = id;
		_start = start;
		_end = end;
		_time = time;
	}
	
	public Player getPlayer()
	{
		return _start.getPlayer();
	}
	
	public int getId()
	{
		return _id;
	}
	
	public Node getStart()
	{
		return _start;
	}
	
	public Node getEnd()
	{
		return _end;
	}
	
	public Calendar getTime()
	{
		return _time;
	}
}

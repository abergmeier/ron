package org.ron.servlet;

import java.util.Calendar;

public interface Segment
{
	public void set(int id, Node start, Node end, Calendar time);
	
	public Player getPlayer();
	
	public int getId();
	
	public Node getStart();
	
	public Node getEnd();
	
	public Calendar getTime();
}

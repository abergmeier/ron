package org.ron.servlet;

import java.util.Calendar;

public interface Segment
extends IntegerIdObject
{
	public void set(int id, Node start, Node end, Calendar time);
	
	public Player getPlayer();
	
	public Node getStart();
	
	public Node getEnd();
	
	public Calendar getTime();
}

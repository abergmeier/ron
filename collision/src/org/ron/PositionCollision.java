package org.ron;

public class PositionCollision
extends Exception
{
	private static final long serialVersionUID = 3740390595478481096L;
	
	public PositionCollision()
	{
	}
	
	public PositionCollision(String message)
	{
		super(message);
	}
}

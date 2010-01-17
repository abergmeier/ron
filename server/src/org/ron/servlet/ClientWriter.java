package org.ron.servlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Vector2f;

public class ClientWriter
{
	private final Map<Integer,List<Object[]>> _players = new HashMap<Integer, List<Object[]>>();
	
	public ClientWriter()
	{
	}
	
	private List<Object[]> getData(Segment segment)
	{
		List<Object[]> playerData = _players.get(segment.getPlayer().getId());
		
		if(playerData == null)
		{
			playerData = new ArrayList<Object[]>();
			_players.put(segment.getPlayer().getId(), playerData);
		}
		
		return playerData;
	}
	
	public void add(Segment segment)
	{
		getData(segment).add
		(
			new Object[]
			{
				"segment",
				segment.getId(),
				segment.getStart().getLatitude(),
				segment.getStart().getLongitude(),
				segment.getEnd().getLatitude(),
				segment.getEnd().getLongitude()
			}
		);	
	}
	
	public void add(Segment segment, Vector2f start, Vector2f end)
	{
		getData(segment).add
		(
			new Object[]
			{
				"partial",
				segment.getId(),
				start.getX(),
				start.getY(),
				end.getX(),
				end.getY()
			}
		);
	}
	
	public Map<Integer,List<Object[]>> close()
	{
		return _players;
	}
}

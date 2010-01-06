package org.ron.servlet;

import java.util.ArrayList;

public class NodeUpdate
{
	private ArrayList<PlayerNodes> _nodes = new ArrayList<PlayerNodes>();
	int _time;
	
	public NodeUpdate()
	{
	}
	
	public PlayerNodes newPlayer(int playerId)
	{
		PlayerNodes nodes = new PlayerNodes(playerId);
		_nodes.add(nodes);
		return nodes;
	}
	
	public ArrayList<PlayerNodes> getPlayerData()
	{
		return _nodes;
	}
	
	public int getTime()
	{
		return _time;
	}
	
	public void setTime(int time)
	{
		_time = time;
	}
	
	public class PlayerNodes
	{
		private int _playerId;
		private ArrayList<Position> _positions = new ArrayList<Position>();
		
		public PlayerNodes(int playerId)
		{
			_playerId = playerId;
			_positions = new ArrayList<Position>();
		}
		
		public int getPlayerId()
		{
			return _playerId;
		}
		
		public ArrayList<Position> getNodes()
		{
			return _positions;
		}
		
		public void addNode(Position position)
		{
			_positions.add(position);
		}
	}
}


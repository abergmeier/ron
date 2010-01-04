package org.ron.servlet;

import java.sql.SQLException;
import java.util.Calendar;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.vecmath.Vector2f;

import org.apache.xmlrpc.webserver.XmlRpcServlet;

public class RonServlet extends XmlRpcServlet
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -9076867142926702651L;

	//remember the HttpSession of the current thread
	private static ThreadLocal<HttpSession> threadSession = new ThreadLocal<HttpSession>();
	
	//gets the session of the current thread
	private static HttpSession getHttpSession()
	{
		return threadSession.get();
	}
	
	private static void setHttpSession(HttpSession value)
	{
		threadSession.set(value);
	}

	//workaround since the Request object is not accessible in the handlers
	//so remember it beforehand for each Post
	public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException
	{
        	setHttpSession(request.getSession());
	        super.doPost(request, response);
	}
	
	public void addNode(int playerId)
	throws SQLException
	{
		HttpSession session = getHttpSession();
		Player player = new Player(playerId, PlayerDatabase.get(session));
		NodeDatabase.get(session).add(player);
	}
	
	public void removeNode(float lat, float lng)
	throws SQLException
	{
		HttpSession session = getHttpSession();
		NodeDatabase.get(session).delete(new Position(lat, lng));
	}
	
	public String update(int playerId, float lat, float lng, int time)
	throws SQLException
	{
		HttpSession session = getHttpSession();
		PlayerDatabase players = PlayerDatabase.get(session);
		NodeDatabase nodes = NodeDatabase.get(session);
		
		Player player = new Player(playerId, players);
		player.setPosition(new Position(lat, lng));
		
		//check for collision with node lines
		for(Player otherPlayer : players)
		{
			Vector2f[] playerNodes = nodes.narrow(otherPlayer);
			for(int i = 1; i < playerNodes.getCount(); i++)
			{
				org.ron.Collision.check(playerNodes[i - 1], playerNodes[i]);
			}
		}
		
		Calendar updateCalendar = Calendar.getInstance();
		updateCalendar.set(Calendar.SECOND, time);
		
		NodeUpdate update = nodes.getNew(player, updateCalendar);
		
		String output = "<state time=\"" + update.getTime() + "\"/>";
		
		for(NodeUpdate.PlayerNodes nodes : update.getPlayerData())
		{
			if(nodes.getNodes().size() == 0)
				continue;
			
			output += "<player id=\"" + nodes.getPlayerId() + "\">";
				
			for(Position position : nodes.getNodes())
			{
				output +=
					"<node " +
						"lat=\"" + position.getLatitude() + "\" " +
						"lng=\"" + position.getLongtitude() + "\"" +
					"/>";
			}
			
			output += "</player>";
		}
		
		return output;
	}
	
	public int addPlayer(String name)
	throws SQLException
	{
		HttpSession session = getHttpSession();
		Player newPlayer = PlayerDatabase.get(session).create(name);
		return newPlayer.getId();
	}
	
	public void removePlayer(int playerId)
	throws SQLException
	{
		HttpSession session = getHttpSession();
		PlayerDatabase database = PlayerDatabase.get(session); 
		database.delete(new Player(playerId, database));
	}
}



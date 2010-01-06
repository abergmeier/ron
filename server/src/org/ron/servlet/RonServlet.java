package org.ron.servlet;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Calendar;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.vecmath.Vector2f;

import org.apache.xmlrpc.webserver.XmlRpcServlet;

public class RonServlet
extends XmlRpcServlet
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
	throws SQLException, ClassNotFoundException
	{
		HttpSession session = getHttpSession();
		PlayerDatabase players = PlayerDatabase.get(session);
		Player player = players.get(playerId);
		NodeDatabase.get(session).add(player, player.getLatitude(), player.getLongtitude());
	}
	
	public void removeNode(double lat, double lng)
	throws SQLException, ClassNotFoundException
	{
		HttpSession session = getHttpSession();
		NodeDatabase _database = NodeDatabase.get(session);
		_database.remove(_database.get((float)lat, (float)lng));
	}
	
	public String updateState(int playerId, double lat, double lng, int time)
	throws SQLException, ClassNotFoundException
	{
		HttpSession session = getHttpSession();
		PlayerDatabase players = PlayerDatabase.get(session);
		NodeDatabase nodes = NodeDatabase.get(session);
		
		Player player = new Player(playerId, players);
		player.setPosition((float)lat, (float)lng);
		
		Calendar updateCalendar = Calendar.getInstance();
		updateCalendar.set(Calendar.SECOND, time);
		
		NodeUpdate update = nodes.getNew(player, updateCalendar);
		
		String output = "<state time=\"" + update.getTime();
		
		Vector2f[] intersections = null;
		
		Position currentPosition = player.getPosition();
		
		String playerString = "";
		try
		{
			//check for collision with node lines
			for(Player currentPlayer : players)
			{
				playerString += "<player id=\"" + currentPlayer.getId() + "\">";
				
				Node[] playerNodes = nodes.toArray(currentPlayer);
				for(int i = 1; i < playerNodes.length; i++)
				{
					intersections = org.ron.Collision.GetIntersections(currentPosition.getLatitude(), currentPosition.getLongtitude(), playerNodes[i - 1], playerNodes[i]);
					playerString +=
						"<segment " +
							"lat=\"" + intersections[0] + "\" " +
							"lng=\"" + intersections[1] + "\"" +
						"/>";
				}
				playerString += "</player>";
			}
		}
		catch(org.ron.PositionCollision exception)
		{
			output += " lost=\"true\""; 
		}
		
		output += "\"/>" + playerString;
		
		return output;
	}

	public int addPlayer(String name, double latitude, double longitude)
	throws SQLException, ClassNotFoundException
	{
		HttpSession session = getHttpSession();
		Player newPlayer = PlayerDatabase.get(session).add(name, (float)latitude, (float)longitude);
		return newPlayer.getId();
	}
	
	public void removePlayer(int playerId)
	throws SQLException, ClassNotFoundException
	{
		HttpSession session = getHttpSession();
		PlayerDatabase database = PlayerDatabase.get(session);
		
		Savepoint save = null;
		Connection connection = null;
		
		try
		{
			connection = database.getConnection();
			save = connection.setSavepoint();
		
			//first remove all nodes of the player
			NodeDatabase.get(session).clear();
			database.remove(database.get(playerId));
		}
		catch(Exception exception)
		{
			if(save != null)
				connection.rollback(save);
		}
		finally
		{
			connection.releaseSavepoint(save);
		}
	}
}

/*
<methodCall>
<methodName>submitPosition</methodName>
<params>
	<param name="player">4</param>
	<param name="lat">3</param>
	<param name="lng">4</param>
</params>
</methodCall>



<methodCall>
	<methodName>updateState</methodName>
	<params>
		<param name="playerid">6</param>
		<param name="lat">234.343</param>
		<param name="lng">2343.45443</param>
		<param name="lastServerTime">4503404</param>
	</params>
</methodCall>
<methodResponse>
	<state time="34343.34" won="false" lost="false">
	<player id="">
		<node lat="" lng=""/>
		<node lat="" lng=""/>
	</player>
	<player id="">
		<node lat="" lng=""/>
		<node lat="" lng=""/>
	</player>
</methodResponse>



<methodCall>
	<methodName>addNode</methodName>
	<params>
		<param name="playerid">1</param>
	<params>
</methodCall>



<methodCall>
	<methodName>addPlayer</methodName>
	<params>
		<param>Hugo</param>
	</params>
</methodCall>
<methodResponse>
	<id>4</id>
</methodResponse>



<methodCall>
	<methodName>removePlayer</methodName>
	<params>
		<param name="id">4</param>
	<params>
</methodCall>
*/
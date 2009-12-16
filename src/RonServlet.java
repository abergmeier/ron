import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.xmlrpc.webserver.XmlRpcServlet;

public class RonServlet extends XmlRpcServlet
{
	//
	private static final long serialVersionUID = 1L;

	
	public void updatePosition(int playerId, float lat, float lng)
	throws SQLException
	{		
		HttpSession session;
		Player player = new Player(playerId, PlayerDatabase.get(session));
		player.setPosition(new Position(lat, lng));
	}
	
	public void addNode(int playerId)
	throws SQLException
	{
		HttpSession session;
		Player player = new Player(playerId, PlayerDatabase.get(session));
		NodeDatabase.get(session).add(player);
	}
	
	public void removeNode(float lat, float lng)
	throws SQLException
	{
		HttpSession session;
		NodeDatabase.get(session).delete(new Position(lat, lng));
	}
	
	public java.util.List getNodes()
	{

	}
	
	public int addPlayer(String name)
	{
		Player newPlayer = PlayerDatabase.get().create(name);
		return newPlayer.getId();
	}
	
	public void removePlayer(int playerId)
	throws SQLException
	{
		HttpSession session;
		PlayerDatabase database = PlayerDatabase.get(session); 
		database.delete(new Player(playerId, database));
	}
}



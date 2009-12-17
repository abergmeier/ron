import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.xmlrpc.webserver.XmlRpcServlet;

public class RonServlet extends XmlRpcServlet
{
	//
	private static final long serialVersionUID = 1L;

	//remember the HttpSession of the current thread
	private static ThreadLocal threadSession = new ThreadLocal();
	
	//gets the session of the current thread
	private static HttpSession getHttpSession()
	{
		return (HttpSession)threadSession.get();
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
        	setHttpSession(request.getHttpSession());
	        super.doPost(request, response);
	}
	
	
	public void updatePosition(int playerId, float lat, float lng)
	throws SQLException
	{		
		HttpSession session = getHttpSession();
		Player player = new Player(playerId, PlayerDatabase.get(session));
		player.setPosition(new Position(lat, lng));
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
	
	public java.util.List getNodes()
	{
		HttpSession session = getHttpSession();
	}
	
	public int addPlayer(String name)
	{
		HttpSession session = getHttpSession();
		Player newPlayer = PlayerDatabase.get().create(name);
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



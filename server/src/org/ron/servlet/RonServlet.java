package org.ron.servlet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Calendar;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.StackKeyedObjectPoolFactory;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.ron.PositionCollision;

public class RonServlet
extends XmlRpcServlet
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -9076867142926702651L;
	
	private GenericObjectPool _pool = null;
	private PoolableConnectionFactory _factory = null;
	private static final String DRIVERKEY = "SQLITEDRIVER";
	
	private static PlayerDatabase _players = null;
	
	@Override
	public void init(javax.servlet.ServletConfig config)
	throws ServletException
	{
		super.init(config);
		
		//see whether a factory already exists
		_pool = new GenericObjectPool();
					
		_factory = new PoolableConnectionFactory
		(
			new DriverManagerConnectionFactory("jdbc:sqlite:server.db", null),
			_pool,
			new StackKeyedObjectPoolFactory(),
			null, //validationQuery
			false,
			true
		);
			
		PoolingDriver driver = new PoolingDriver();
		driver.registerPool(DRIVERKEY, _pool);
	}
	
	@Override
	public void destroy()
	{	
		try
		{
			_players.getNodes().getSegments().getViews().close();
			_players.getNodes().getSegments().close();
			_players.getNodes().close();
			_players.close();
			
			_pool.close();
		}
		catch(Exception exception)
		{
			exception.printStackTrace();
		}
		
		super.destroy();
	}
/*	
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
*/
	
	protected Connection getConnection()
	throws ClassNotFoundException, SQLException
	{
		Class.forName("org.sqlite.JDBC");
	
		Connection connection = DriverManager.getConnection("jdbc:apache:commons:dbcp:" + DRIVERKEY);
		connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
	
		if(connection == null)
			throw new NullPointerException("Could not create database");
		
		return new SQLiteConnection(connection);
	}

	//workaround since the Request object is not accessible in the handlers
	//so remember it beforehand for each Post
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException
	{
		/*
        	setHttpSession(request.getSession());
        	*/
        	
		
    	Connection connection;
		try
		{
			connection = getConnection();
    	
        	if(_players == null)
	        	_players = new PlayerDatabase(connection);
        	else
        		_players.setConnection(connection);
        	
        	Statement statement = connection.createStatement();
        	statement.execute("PRAGMA foreign_keys = ON;");        	
		}
		catch (SQLException exception)
		{
			throw new ServletException(exception);
		}
		catch (ClassNotFoundException exception)
		{
			throw new ServletException(exception);
		}
		
		super.doPost(request, response);
            
        try
        {
	        //clear connection again
	        _players.setConnection(null);
			connection.close();
		}
        catch (SQLException exception)
        {
        	throw new ServletException(exception);
		}   
	}
	
	public boolean addNode(int playerId)
	throws SQLException, ClassNotFoundException
	{
		Player player = _players.get(playerId);
		_players.getNodes().addForPlayerPosition(player);
		return true;
	}
	
	public boolean removeNode(double lat, double lng)
	throws SQLException, ClassNotFoundException
	{
		NodeDatabase _database = _players.getNodes();
		_database.remove(_database.get((float)lat, (float)lng));
		return true;
	}
	
	public String updateState(int playerId, double lat, double lng)
	throws SQLException, ClassNotFoundException
	{
		return updateState(playerId, lat, lng, 0);
	}
	
	public String updateState(int playerId, double lat, double lng, int time)
	throws SQLException, ClassNotFoundException
	{		
		Player player = _players.get(playerId);
		
		if(player.hasLost())
			return "<state/>"; //user no longer needs updates
		
		//before anything else update position
		player.setPosition((float)lat, (float)lng);
		
		final Calendar updateCalendar = Calendar.getInstance();
		updateCalendar.set(Calendar.SECOND, time);
		
		ClientWriter writer = new ClientWriter();
		
		Calendar newUpdateTime = updateCalendar;
		
		Segment[] segments;
		try
		{
			Boolean allLost = null;
			
			for(Player otherPlayer : _players)
			{
				if(otherPlayer.equals(player))
					continue; //we already have all data of current player on device
			
				if(allLost == null)
					allLost = true;
				
				allLost = allLost && otherPlayer.hasLost();
			
				segments = otherPlayer.getSegments(updateCalendar);
				
				for(Segment segment : segments)
				{
					if(newUpdateTime.after(segment.getTime()))
						continue; //update is already "older"
					
					newUpdateTime = segment.getTime();
				}
				
				player.getUpdate(writer, segments);
			}
			
			if(allLost != null && allLost)
				return "<state won=\"true\"/>";
		}
		catch(PositionCollision exception)
		{
			return "<state lost=\"true\"/>";
		}
		
		return
			"<state time=\"" + newUpdateTime.get(Calendar.SECOND) + "\">\n" +
				writer.close() +
			"</state>";
		
	}

	public int addPlayer(String name, double latitude, double longitude)
	throws SQLException, ClassNotFoundException
	{		
		Player newPlayer = _players.add(name, (float)latitude, (float)longitude);
		return newPlayer.getId();
	}
	
	public boolean removePlayer(int playerId)
	throws SQLException, ClassNotFoundException
	{
		Connection connection = _players.getConnection();
		Savepoint save = connection.setSavepoint();
		
		try
		{
			//first remove all nodes of the player
			_players.getNodes().clear();
			_players.remove(_players.get(playerId));
			return true;
		}
		catch(SQLException exception)
		{
			connection.rollback(save);
			throw exception;
		}
		catch(RuntimeException exception)
		{
			connection.rollback(save);
			throw exception;
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
	<state time="34343" won="false" lost="false">
		<player id="">
			<segment id="1">
				<start lat="343.343" lng="3434.343"/>
				<end lat="434.3434" lng="3434.343"/>
			</segment>
			<partial id="2">
			 	<start lat="4343.343" lng="3434.343"/>
			 	<end lat="4343.343" lng="23343.343"/>
			 </partial>
		</player>
	</state>
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
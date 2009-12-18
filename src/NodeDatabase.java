import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import javax.servlet.http.HttpSession;

public class NodeDatabase
extends AbstractDatabase<Position>
{
	protected NodeDatabase(HttpSession session)
	throws SQLException 
	{
		super(session);
	}
	
	public static NodeDatabase get(HttpSession session)
	throws SQLException
	{	
		final String KEY = "NodeDatabase";
		
		NodeDatabase instance = (NodeDatabase)session.getAttribute(KEY);
		
		if(instance == null)
		{
			instance = new NodeDatabase(session);
			session.setAttribute(KEY, instance);
		}
		
		return instance;
	}
	
	protected String getTableName()
	{
		return "NODE";
	}
	
	protected void createTable()
	throws SQLException
	{
		execute
		(
				"BEGIN;" +
				"CREATE TABLE \"" + getTableName() + "\" " +
				"(" +
					"\"PLAYERID\" INTEGER NOT NULL, " +
					"\"LAT\" REAL NOT NULL, " +
					"\"LNG\" REAL NOT NULL, " +
					"\"TIME\" INTEGER NOT NULL" +
				");" +
				"CREATE UNIQUE INDEX \"position-index\" on node (LAT ASC, LNG ASC);" +
				"COMMIT;"
		);
	}
	
	public NodeUpdate getNew(Player player, Calendar time)
	throws SQLException
	{
		PreparedStatement statement = getConnection().prepareStatement
		(
			"SELECT " +
				"PLAYERID," +
				"LAT," +
				"LNG," +
				"MAX(TIME)" +
			"FROM NODE " +
			"WHERE " +
				"TIME >= ? " +
			 	"AND PLAYERID <> ?" +
			"ORDER BY PLAYERID, TIME"
		);
		
		statement.setInt(0, time.get(Calendar.SECOND));
		statement.setInt(1, player.getId());
		ResultSet result = statement.executeQuery();
		
		NodeUpdate update = new NodeUpdate();
		NodeUpdate.PlayerNodes playerNodes = null;
		
		int lastPlayerId = Integer.MIN_VALUE;
		int playerId;
		
		Position position;
		for(result.first(), update.setTime(result.getInt(3)); result.next();)
		{
			playerId = result.getInt(0);
			
			if(lastPlayerId != playerId)
			{
				lastPlayerId = playerId;
				playerNodes = update.newPlayer(playerId);
			}
			
			position = new Position(result.getFloat(1), result.getFloat(2));
			playerNodes.addNode(position);			
		}
		
		return update;
	}
	
	public void add(Player player)
	throws SQLException
	{		
		PreparedStatement statement = getConnection().prepareStatement("INSERT INTO " + getTableName() + " (PLAYERID, LAT, LNG, TIME) VALUES (?, ?, ? , ?)");
		
		try
		{
			statement.setInt(0, player.getId());
			Position position = player.getPosition();
			statement.setFloat(1, position.getLatitude());
			statement.setFloat(2, position.getLongtitude());
			
			statement.setInt(3, Calendar.getInstance().get(Calendar.SECOND));
			statement.execute();
		}
		finally
		{
			statement.close();
		}
	}
	
	public void delete(Position position)
	throws SQLException
	{
		execute
		(
			"DELETE FROM " + getTableName() + " " +
			"WHERE " +
				"LAT = " + position.getLatitude() + ", " +
				"LNG = " + position.getLongtitude()
		);
	}
	
	public void deleteAll()
	throws SQLException
	{
		execute("DELETE FROM " + getTableName());
	}
}

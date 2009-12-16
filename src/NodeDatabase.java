import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;

import javax.servlet.http.HttpSession;

public class NodeDatabase
extends AbstractDatabase<Position>
{
	protected NodeDatabase()
	throws SQLException 
	{
		super();
	}
	
	public static NodeDatabase get(HttpSession session)
	throws SQLException
	{
		final String KEY = "NodeDatabase";
		NodeDatabase instance = (NodeDatabase)session.getAttribute(KEY);
		
		if(instance == null)
		{
			instance = new NodeDatabase();
			session.setAttribute(KEY, instance);
		}
		
		return instance;
	}
	
	protected String getTableName()
	{
		return "NODE";
	}
	
	protected void createTable(String tableName)
	throws SQLException
	{
		execute
		(
				"CREATE TABLE \"" + getTableName() + "\""
				+ "("
					+ "\"PLAYERID\" INTEGER NOT NULL,"
					+ "\"LAT\" REAL NOT NULL,"
					+ "\"LNG\" REAL NOT NULL,"
					+ "\"TIME\" INTEGER NOT NULL"
				+ ")"
		);
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
			statement.executeQuery();
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
			"DELETE FROM " + getTableName()
			+ " WHERE "
				+ " LAT = " + position.getLatitude() + ","
				+ " LNG = " + position.getLongtitude()
		);
	}
	
	public void removeAll()
	throws SQLException
	{
		execute("DELETE FROM " + getTableName());
	}
}

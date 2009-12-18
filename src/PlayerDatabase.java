import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.http.HttpSession;

public class PlayerDatabase
extends AbstractDatabase<Player>
{
	
	protected PlayerDatabase(HttpSession session)
	throws SQLException
	{
		super(session);
	}
	
	protected String getTableName()
	{
		return "PLAYER";
	}
	
	//creates a new database on the session if necessary 
	public static PlayerDatabase get(HttpSession session)
	throws SQLException
	{
		final String KEY = "PlayerDatabase";
		PlayerDatabase instance = (PlayerDatabase)session.getAttribute(KEY);
		
		if(instance == null)
		{
			instance = new PlayerDatabase(session);
			session.setAttribute(KEY, instance);
		}
	
		return instance;
	}
	
	protected void createTable()
	throws SQLException
	{
		execute
		(
				"CREATE TABLE " + getTableName() + " "
				+ "("
    				+ "ID INTEGER PRIMARY KEY NOT NULL,"
    				+ "LAT REAL NULL,"
    				+ "LNG REAL NULL,"
    				+ "NAME TEXT NOT NULL"
    			+ ")"
		);
	}
	
	public Player get(int playerId)
	throws SQLException
	{
		Statement statement = getConnection().createStatement();
		ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + getTableName() + " WHERE ID = " + playerId);

		result.first();
		if(result.getInt(0) == 0)
			throw new IndexOutOfBoundsException();
		
		return new Player(playerId, this);
	}
	
	public Position getPosition(Player player)
	throws SQLException
	{
		PreparedStatement statement = getConnection().prepareStatement("SELECT LAT, LNG FROM " + getTableName() + " WHERE ID = ?");
		
		try
		{	
			statement.setInt(0, player.getId());
			
			ResultSet result = statement.executeQuery();
			result.first();
			return new Position(result.getFloat(0), result.getFloat(1));
		}
		finally
		{
			statement.close();
		}
	}
	
	public Player create(String playerName)
	throws SQLException
	{
		execute("INSERT INTO " + getTableName() + " (NAME) VALUES ('" + playerName + "')");
		
		Statement statement = getConnection().createStatement();
		
		try
		{
			ResultSet result = statement.executeQuery("SELECT ID FROM PLAYER WHERE NAME = '" + playerName + "'");
			
			result.next();
			return new Player(result.getInt(0), this);	
		}
		finally
		{
			statement.close();
		}
	}
	
	public void delete(Player player)
	throws SQLException
	{
		execute("DELETE FROM PLAYER WHERE ID = " + player.getId());
	}

	public void setPosition(Player player, Position position)
	throws SQLException
	{
		PreparedStatement statement = getConnection().prepareStatement("UPDATE PLAYER SET LAT = ?, LNG = ? WHERE ID = ?");
		
		try
		{
			statement.setFloat(0, position.getLatitude());
			statement.setFloat(1, position.getLongtitude());
			statement.setInt(2, player.getId());
			statement.executeQuery();			
		}
		finally
		{
			statement.close();
		}
	}
}

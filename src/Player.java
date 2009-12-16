import java.sql.SQLException;


public class Player
{
	private int _id;
	private PlayerDatabase _database;
	
	public Player(int id, PlayerDatabase database)
	{
		_id = id;
		_database = database;
	}

	public int getId()
	{
		return _id;
	}
	
	public Position getPosition()
	throws SQLException
	{
		return _database.getPosition(this);
	}

	public void setPosition(Position position)
	throws SQLException
	{
		_database.setPosition(this, position);		
	}
}

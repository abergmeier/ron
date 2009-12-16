
public class Position
{
	private float _latitude;
	private float _longtitude;
	
	public Position(float latitude, float longtitude)
	{
		_latitude = latitude;
		_longtitude = longtitude;
	}
	
	public float getLatitude()
	{
		return _latitude;
	}
	
	public float getLongtitude()
	{
		return _longtitude;
	}
}

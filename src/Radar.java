class Radar
{
	private float _range = 20f;
	public void inRange(Position position1, Position position2)
	{
		float lat = position1.getLatitude() - position2.getLatitude();
		float lng = position1.getLongtitude() - position2.getLongtitude();
		
		return _range <= Math.sqrt(Math.pow(lat, 2) + Math.pow(lng, 2));
	}
	
	private void test()
	{
		dx = x2 - x1;
		dy = y2 - y1;
		
		dr = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
		
		D = x1 * y2 - x2 * y1;
		
		x = D * dy + sgn dy * dx Math.sqrt(Math.pow(r, 2) * Math.pow(dr, 2) - Math.pow(D, 2)) / Math.pow(dr, 2);
	}
}

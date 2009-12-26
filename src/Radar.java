
import javax.vecmath.Vector2f;

class Radar
{
	private static Vector2f[] buffers = new Vector2f[]{new Vector2f(), new Vector2f()};
	
	private static class Result
	{
		private Vector2f[] _result = new Vector2f[2];
		private Vector2f _mid;
		
		public Result(Vector2f mid, Vector2f A, Vector2f B)
		{
			_mid = mid;
			
			_result[0] = new Vector2f(A);
			_result[0].sub(_mid);
			_result[1] = new Vector2f(B);
			_result[1].sub(_mid);
			
			//make sure the order is negative to positive
			if(_result[0].getX() > _result[1].getX())
			{
				//switch
				Vector2f reference = _result[0];
				_result[0] = _result[1];
				_result[1] = reference;
			}
		}
		
		public void add(Vector2f entry)
		{
			entry.sub(_mid);
			
			try
			{
				int index;
				
				if(entry.getX() < 0)
					index = 0;
				else
					index = 1;
				
				if(entry.length() < _result[index].length())
					_result[index].set(entry);
			}
			finally
			{
				//make sure we have the initial state
				entry.add(_mid);
			}
		}
		
		public Vector2f[] getAll()
		{
			_result[0].add(_mid);
			_result[1].add(_mid);
			return _result;
		}
	}
	
	//circle : [C, r]
	// segment [A, B]
	// [P] : Point of collision on segment.
	private static Vector2f[] CircleSegmentIntersect(Vector2f C, float r, Vector2f A, Vector2f B)
	{
		Vector2f P;
		
		{
			Vector2f AC = buffers[0];
			AC.set(C);
			AC.sub(A);
			Vector2f AB = buffers[1];
			AB.set(B);
			AB.sub(A);
			
			float ABlength = AB.length(); 
				
			AB.normalize();
	
			float t = AC.dot(AB);
			//float acab = AC.dot(AB);
			//float t = acab / ab2;
			
			if (t <= 0.0f)
			{
				//the shortest distance on AB to C is A
				// since point does not longer reside on AB
				P = A;
			}
			else if (t >= ABlength)
			{
				//the shortest distance on AB to C is B
				//since point does no longer reside on AB
				P = B;
			}
			else
			{
				AB.scale(t);
				AB.add(A);
								
				P = AB;
			}
		}
		
		float segmentLength;
		
		{ //narrow distance processing
			//use non used vector so we don't have to allocate
			Vector2f distance = buffers[0];

			distance.set(P);
			distance.sub(C);
		
			if(distance.length() > r)
				return null; //not in circle
			else if(distance.length() == r)
				return new Vector2f[]{new Vector2f(P)}; //on circle
		
			//we have to calculate the 2 points within
			//the circle
			segmentLength = (float)Math.sqrt(Math.pow(r, 2) - distance.lengthSquared());
		}
		
		//let the result class calculate which of
		//the points is the ones we want
		Result result = new Result(P, A, B);		
		
		{ //narrow calculation of intersections
			Vector2f intersection = buffers[0];
			intersection.set(B);
			intersection.sub(A);
			intersection.normalize();
			intersection.scale(segmentLength);
			intersection.add(P);
			
			result.add(intersection);
			
			intersection.sub(P);
			intersection.negate();
			intersection.add(P);
			
			result.add(intersection);
		}
		
		return result.getAll();
	}
/*	
	private static float getY(float x, Vector2f A, Vector2f B)
	{
		return B.getY() * (x - A.getX()) / B.getX() + A.getY();
	}
	
	public static Vector2f[] GetIntersections2(Vector2f midCircle, float r, Vector2f A, Vector2f B)
	{
		float dx = B.getX() - A.getX();
		float dy = B.getY() - A.getY();
		
		float dr2 = (float)(Math.pow(dx, 2) + Math.pow(dy, 2));
		float D = A.getX() * B.getY() - B.getX() * A.getY();
			
		double squared = Math.pow(r, 2) * dr2 - Math.pow(D, 2);
		
		if(squared < 0)
			return null; //we have no intersection
		else if(squared == 0)
			return null; //easier to handle this way
		
		float first = D * dy;
		float second = (float)(Math.abs(dy) * dx * Math.sqrt(squared));
		
		float x = (first + second) / dr2;
		
		Vector2f[] vectors = new Vector2f[2];
		vectors[0] = new Vector2f(x, getY(x, A, B));
		
		x = (first - second) / dr2;
		vectors[1] = new Vector2f(x, getY(x, A, B));
		
		return vectors;
	}
*/

	public static Vector2f[] GetIntersections(Vector2f midCircle, float r, Vector2f A, Vector2f B)
	{
		return CircleSegmentIntersect(midCircle, r, A, B);
	}
}

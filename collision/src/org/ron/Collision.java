package org.ron;

import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import javax.vecmath.Vector2f;

public class Collision
{
	private static final float RADIUS = 20;
	private static Vector2f[] buffers = new Vector2f[]{new Vector2f(), new Vector2f()};
	
	private static class Result
	{
		private static class VectorLengthComparator
		implements Comparator<Vector2f>
		{
			 public int compare(Vector2f o1, Vector2f o2)
			 {
				 return (int)(o1.length() - o2.length());
			 }
	         
			 public boolean equals(Object obj)
			 {
				 return obj instanceof VectorLengthComparator;
			 }
		}
		
		private TreeSet<Vector2f> _result = new TreeSet<Vector2f>(new VectorLengthComparator());
		private Vector2f _mid;
		
		public Result(Vector2f mid, Vector2f A, Vector2f B)
		{
			_mid = mid;
			
			Vector2f vector = new Vector2f(A);
			vector.sub(_mid);
			_result.add(vector);

			vector = new Vector2f(B);
			vector.sub(_mid);
			_result.add(vector);
		}
		
		public void add(Vector2f entry)
		{
			entry.sub(_mid);
			
			try
			{
				_result.add(new Vector2f(entry));
			}
			finally
			{
				//make sure we have the initial state
				entry.add(_mid);
			}
		}
		
		public Vector2f getFirst()
		{
			return _result.first();
		}
		
		public Vector2f getSecond()
		{
			Iterator<Vector2f> iterator = _result.iterator();
			iterator.next();
			return iterator.next();
		}
	}
	
	//circle : [C, r]
	// segment [A, B]
	// [P] : Point of collision on segment.
	private static Vector2f[] CircleSegmentIntersect(Vector2f C, float r, Vector2f A, Vector2f B)
	throws PositionCollision
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
		
			if(distance.length() == 0)
				throw new PositionCollision();
			else if(distance.length() > r)
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
		
		return new Vector2f[]{result.getFirst(), result.getSecond()};
	}
	
	public static Vector2f[] GetIntersections(Vector2f midCircle, Vector2f A, Vector2f B)
	{
		return GetIntersections(midCircle, RADIUS, A, B);
	}
	
	public static Vector2f[] GetIntersections(float lat, float lng, Vector2f A, Vector2f B)
	{
		return GetIntersections(new Vector2f(lat, lng), A, B);
	}

	public static Vector2f[] GetIntersections(Vector2f midCircle, float r, Vector2f A, Vector2f B)
	throws PositionCollision
	{
		return CircleSegmentIntersect(midCircle, r, A, B);
	}
}

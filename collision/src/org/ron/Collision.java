package org.ron;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.vecmath.Vector2f;

public class Collision
{
	private static Vector2f[] buffers = new Vector2f[]{new Vector2f(), new Vector2f()};
	private static Result _results = new Result();
	
	private static class Result
	{
		private static final int _capacity = 4;
		private List<Vector2f> _vectors = new ArrayList<Vector2f>(_capacity);
		private Vector2f _mid;
		private boolean _isSorted = false;
		private int _i = 0;
		
		public Result()
		{
			for(int i = 0; i != _capacity; i++)
			{
				_vectors.add(new Vector2f());
			}
		}
		
		public void init(Vector2f mid, Vector2f A, Vector2f B)
		{
			_mid = mid;
			_i = 0;
			
			Vector2f vector = _vectors.get(0);
			vector.set(A);
			vector.sub(_mid);
			
			for(int i = 1; i != _vectors.size(); i++)
			{
				_vectors.get(i).set(Float.MAX_VALUE, Float.MAX_VALUE); 
			}
			
			add(B);
		}
		
		public void add(Vector2f entry)
		{
			_isSorted = false;
			_vectors.get(_i).set(entry);
			_vectors.get(_i).sub(_mid);
			_i++;
		}
		
		private void sort()
		{
			if(_isSorted)
				return;
			
			Collections.sort
			(
				_vectors,
				new Comparator<Vector2f>()
				{
					@Override
					public int compare(Vector2f vector1, Vector2f vector2)
					{
						float delta = vector1.length() - vector2.length();
						
						if(delta < 0f)
							return -1;
						else if(delta > 0f)
							return 1;
						else
							return 0;
					}
					
				}
			);
			
			_isSorted = true;
		}
		
		public void getFirst(Vector2f vector)
		{
			sort();
				
			vector.set(_vectors.get(0));
			vector.add(_mid);
		}
		
		public void getSecond(Vector2f vector)
		{
			sort();
			
			vector.set(_vectors.get(1));
			vector.add(_mid);
		}
	}
	
	//circle : [C, r]
	// segment [A, B]
	// [P] : Point of collision on segment.
	private static boolean CircleSegmentIntersect(Vector2f C, float r, Vector2f A, Vector2f B, Vector2f[] colResult)
	throws PositionCollision
	{
		synchronized(buffers)
		{
	
			final Vector2f P;
			
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
			
				if(distance.length() == 0f)
					throw new PositionCollision();
				
				//we're done here with collision processing
				//only go on when a radius needs to be processed
				if(r == 0f)
					return false; //we're done (did collision testing only)
				else if(r < 0f)
					r = Math.abs(r); //negative is the same as positive radius
				
				if(distance.length() > r)
					return false; //not in circle
				else if(distance.length() == r)
				{
					//right on circle
					colResult[0].set(P);
					colResult[0].add(C);
					colResult[1].set(colResult[0]);					
					return true;
				}
			
				//we have to calculate the 2 points within
				//the circle
				segmentLength = (float)Math.sqrt(Math.pow(r, 2) - distance.lengthSquared());
			}
			
			//let the result class calculate which of
			//the points is the ones we want
			synchronized(_results)
			{
				_results.init(P, A, B);
							
				{ //narrow calculation of intersections
					Vector2f intersection = buffers[0];
					intersection.set(B);
					intersection.sub(A);
					intersection.normalize();
					intersection.scale(segmentLength);
					intersection.add(P);
					
					_results.add(intersection);
					
					intersection.sub(P);
					intersection.negate();
					intersection.add(P);
					
					_results.add(intersection);
				}
				
				_results.getFirst(colResult[0]);
				_results.getSecond(colResult[1]);
				return true;
			}
		}
	}
	
	public static boolean GetIntersections(Vector2f midCircle, float r, Vector2f A, Vector2f B, Vector2f[] colResult)
	throws PositionCollision
	{
		return CircleSegmentIntersect(midCircle, r, A, B, colResult);
	}
	
	public static void testCollision(Vector2f midCircle, Vector2f A, Vector2f B)
	throws PositionCollision
	{
		CircleSegmentIntersect(midCircle, 0, A, B, null);
	}
}

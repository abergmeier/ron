package org.ron;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.vecmath.Vector2d;
import javax.vecmath.Vector2f;

public class Collision
{
	private static Vector2d[] buffers = new Vector2d[]{new Vector2d(), new Vector2d()};
	private static Result _results = new Result();
	
	private static class Result
	{
		private static final int _capacity = 4;
		private List<Vector2d> _vectors = new ArrayList<Vector2d>(_capacity);
		private Vector2d _mid;
		private Vector2f _fMid = new Vector2f();
		private boolean _isSorted = false;
		private int _i = 0;
		
		public Result()
		{
			for(int i = 0; i != _capacity; i++)
			{
				_vectors.add(new Vector2d());
			}
		}
		
		public void init(Vector2d mid, Vector2d A, Vector2d B)
		{
			_mid = mid;
			_fMid.set(_mid);
			_i = 0;
			
			Vector2d vector = _vectors.get(0);
			vector.set(A);
			vector.sub(_mid);
			
			for(int i = 1; i != _vectors.size(); i++)
			{
				_vectors.get(i).set(Double.MAX_VALUE, Double.MAX_VALUE); 
			}
			
			add(B);
		}
		
		public void add(Vector2d entry)
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
				new Comparator<Vector2d>()
				{
					@Override
					public int compare(Vector2d vector1, Vector2d vector2)
					{
						double delta = vector1.length() - vector2.length();
						
						if(delta < 0d)
							return -1;
						else if(delta > 0d)
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
			vector.add(_fMid);
		}
		
		public void getSecond(Vector2f vector)
		{
			sort();
			
			vector.set(_vectors.get(1));
			vector.add(_fMid);
		}
	}
	
	//circle : [C, r]
	// segment [A, B]
	// [P] : Point of collision on segment.
	private static boolean CircleSegmentIntersect(Vector2d C, float r, Vector2d A, Vector2d B, Vector2f[] colResult)
	throws PositionCollision
	{
		synchronized(buffers)
		{
			
			boolean isAIn;
			boolean isBIn;	
			
			final Vector2d P;
			
			{
				{
					Vector2d in = buffers[0];
					in.sub(C, B);				
					isBIn = in.length() < r;

					//TODO: add collision detection								
				}
				
				Vector2d AC = buffers[0];
				AC.sub(C, A);
				isAIn = AC.length() < r;
				
				if(isAIn && isBIn)
				{
					//we don't need any further processing - done
					colResult[0].set(A);
					colResult[1].set(B);
					return true;
				}
				
				Vector2d AB = buffers[1];
				AB.sub(B, A);
				AB.normalize();
				double t = AC.dot(AB);
				
				AB.scale(t);
				
				AB.add(A);
				
				P = AB;
			}
			
			double segmentLength;
			
			{
				double pDistance;
			
				{ //narrow distance processing
					//use non used vector so we don't have to allocate
					Vector2d distanceVector = buffers[0];
					distanceVector.sub(P, C);
					
					pDistance = distanceVector.length();
				}
			
				if(pDistance == 0d)
					throw new PositionCollision();
				
				//we're done here with collision processing
				//only go on when a radius needs to be processed
				if(r == 0d)
					return false; //we're done (did collision testing only)
				
				//make sure radius is positive
				r = Math.abs(r);
				
				if(pDistance > r)
					return false; //not in circle
				else if(pDistance == r)
				{
					//right on circle
					colResult[0].set(P);
					colResult[1].set(colResult[0]);					
					return true;
				}
			
				//we have to calculate the 2 points within
				//the circle
				segmentLength = Math.sqrt(Math.pow(r, 2) - pDistance * pDistance);
			}
			
			//let the result class calculate which of
			//the points is the ones we want
			synchronized(_results)
			{
				_results.init(P, A, B);
							
				{ //narrow calculation of intersections
					Vector2d intersection = buffers[0];
					intersection.sub(B, A);
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
	
	public static boolean GetIntersections(Vector2d midCircle, float r, Vector2d A, Vector2d B, Vector2f[] colResult)
	throws PositionCollision
	{
		return CircleSegmentIntersect(midCircle, r, A, B, colResult);
	}
	
	public static void testCollision(Vector2d midCircle, Vector2d A, Vector2d B)
	throws PositionCollision
	{
		CircleSegmentIntersect(midCircle, 0, A, B, null);
	}
}

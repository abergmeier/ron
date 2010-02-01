package org.ron.test;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import org.ron.Collision;
import org.ron.PositionCollision;

import javax.vecmath.Vector2d;
import javax.vecmath.Vector2f;


public class RadarCanvas
extends Canvas
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1779914013346998246L;
	private Vector2d _circle = new Vector2d();
	private float _r = Float.NaN;
	private ArrayList<Vector2f[]> _nodes = new ArrayList<Vector2f[]>();
	
	public RadarCanvas(Vector2f circle, float r)
	{
		setBackground(Color.WHITE);
		_circle.set(circle);
		_r = r;
	}

	private void drawCircle(Graphics graphic, Vector2f midPoint, float r)
	{
		drawCircle(graphic, (int)(midPoint.getX() - r), (int)(midPoint.getY() - r), r);
	}
	
	private void drawCircle(Graphics graphic, Vector2d midPoint, float r)
	{
		drawCircle(graphic, (int)(midPoint.getX() - r), (int)(midPoint.getY() - r), r);
	}
	
	private void drawCircle(Graphics graphic, int x, int y, float r)
	{
		graphic.drawArc
		(
			x,
			y,
            (int)(r * 2),
            (int)(r * 2),
            0,
            360
		);
	}
	
	private void drawLine(Graphics graphic, Vector2f start, Vector2f end)
	{
		graphic.drawLine
		(
				(int)start.getX(),
				(int)start.getY(),
				(int)end.getX(),
				(int)end.getY()
		);	
	}
	
	public void addPlayer(Vector2f[] nodes)
	{
		_nodes.add(nodes);
		repaint();
	}

	public void paint(Graphics graphic)
	{
		graphic.setColor(Color.RED);
		drawCircle(graphic, _circle, _r);
		
		long[] times = new long[1];
		long calcTime = 0;
		
		float h = 0f;
		float s = 1f;
		float b = 1f;
		Color foregroundColor;
		
		Vector2f[] intersections = new Vector2f[]{new Vector2f(), new Vector2f()};
		
		try {
			Collision.GetIntersections(new Vector2d(2.5, 3), 2f, new Vector2d(4d, 3d), new Vector2d(5.5d, 5), intersections);
		} catch (PositionCollision e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try
		{
			boolean succeeded;
			
			Vector2d start = new Vector2d();
			Vector2d end = new Vector2d();
			
			for(Vector2f[] nodes : _nodes)
			{
				for(int i = 1; i != nodes.length; i++)
				{
					foregroundColor = Color.getHSBColor(h + i / 1000f, s, b);
					graphic.setColor(foregroundColor);
					drawLine(graphic, nodes[i - 1], nodes[i]); 
						
					times[0] = System.nanoTime();
					
					start.set(nodes[i - 1]);
					end.set(nodes[i]);
					
					succeeded = Collision.GetIntersections(_circle, _r, start, end, intersections);
					times[0] = System.nanoTime() - times[0];
					calcTime += times[0]; 
			
					if(!succeeded)
						continue; //found no intersections
							
					for(Vector2f intersection : intersections)
					{
						//drawLine(graphic, _circle, intersection);
						drawCircle(graphic, intersection, 5);
					}
				}
				
				h += 1f / 20f;
			}
		
			System.out.println("Calculation took " + calcTime / 1000000 + " ms");
		}
		catch(PositionCollision exception)
		{
			System.out.println("Boom");
		}
	}
}

package org.ron.test;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import org.ron.Collision;
import javax.vecmath.Vector2f;


public class RadarCanvas
extends Canvas
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1779914013346998246L;
	private Vector2f _circle = null;
	private float _r = Float.NaN;
	private ArrayList<Vector2f[]> _nodes = new ArrayList<Vector2f[]>();
	
	public RadarCanvas(Vector2f circle, float r)
	{
		setBackground(Color.WHITE);
		_circle = circle;
		_r = r;
	}
	
	private void drawCircle(Graphics graphic, Vector2f midPoint, float r)
	{
		graphic.drawArc
		(
			(int)(midPoint.getX() - r),
			(int)(midPoint.getY() - r),
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
		
		for(Vector2f[] nodes : _nodes)
		{
			for(int i = 1; i != nodes.length; i++)
			{
				foregroundColor = Color.getHSBColor(h + i / 1000f, s, b);
				graphic.setColor(foregroundColor);
				drawLine(graphic, nodes[i - 1], nodes[i]); 
					
				times[0] = System.nanoTime();
				boolean succeeded = Collision.GetIntersections(_circle, _r, nodes[i - 1], nodes[i], intersections);
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
}

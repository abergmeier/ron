import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;

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
	private Vector2f[] _nodes = null;
	
	public RadarCanvas(Vector2f circle, float r, Vector2f[] nodes)
	{
		_circle = circle;
		_r = r;
		_nodes = nodes;
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

	public void paint(Graphics graphic)
	{
		graphic.setColor(Color.RED);
		drawCircle(graphic, _circle, _r);
		
		Color lines = Color.YELLOW;
		long calcTime = 0;
		for(int i = 1; i != _nodes.length; i++, lines = lines.darker())
		{
			graphic.setColor(lines);
			drawLine(graphic, _nodes[i - 1], _nodes[i]);
				
			long[] times = new long[1]; 
				
			times[0] = System.nanoTime();
			Vector2f[] intersections = Radar.GetIntersections(_circle, _r, _nodes[i - 1], _nodes[i]);
			times[0] = System.nanoTime() - times[0];
			calcTime += times[0]; 
	
			if(intersections == null)
				continue;
					
			for(Vector2f intersection : intersections)
			{
				//drawLine(graphic, _circle, intersection);
				drawCircle(graphic, intersection, 5);
			}
		}
		
		System.out.println("Calculation took " + calcTime / 1000000 + " ms");
	}
}

package org.ron.test;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Random;

import javax.vecmath.Vector2f;

public class CollisionTest
{	
	public static void main(String[] args)
	{
		Vector2f[] nodes = new Vector2f[]
		{
			new Vector2f(4, 6),
			new Vector2f(100, 10),
			new Vector2f(34, 303),
			new Vector2f(340, 2),
			new Vector2f(10, 400),
			new Vector2f(400, 400),
			new Vector2f(290, 216)
		};
		
		final int XSIZE = 500;
		final int YSIZE = 500;		

		Vector2f circle = new Vector2f(XSIZE / 2f, YSIZE / 2f);
		final float r = 100f;

		Frame window = new Frame("Collisions");
		window.setSize(XSIZE, YSIZE);
		window.addWindowListener
		(
				new WindowAdapter()
				{
					public void windowClosing( WindowEvent e )
			        {
			            System.exit(0);
			        }
				}
		);
		RadarCanvas canvas = new RadarCanvas(circle, r);
		canvas.addPlayer(nodes);
		
		final int NODECOUNT = 20;
		Random randomizer = new Random();
		for(int i = 0; i != 4; i++)
		{
			nodes = new Vector2f[NODECOUNT];
			
			for(int j = 0; j != NODECOUNT; j++)
			{
				nodes[j] = new Vector2f(randomizer.nextFloat() * XSIZE, randomizer.nextFloat() * YSIZE);
			}
			
			canvas.addPlayer(nodes);
		}
		
		window.add(canvas);
		window.setVisible(true);
	}
}

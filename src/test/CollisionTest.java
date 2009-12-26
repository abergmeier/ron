import java.awt.Color;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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

		Vector2f circle = new Vector2f(250f, 250f);
		final float r = 100f;

		Frame window = new Frame("Collisions");
		window.setSize(500, 500);
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
		RadarCanvas canvas = new RadarCanvas(circle, r, nodes);
		canvas.setBackground(Color.WHITE);
		window.add(canvas);
		window.setVisible(true);
	}
}

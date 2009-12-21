import javax.vecmath.Vector2f;

class Radar
{
	private float _range = 20f;
	
	//circle : [C, r]
	// segment [A, B]
	// [P] : Point of collision on segment.
	Vector2f CircleSegmentIntersect(Vector2f C, float r, Vector2f A, Vector2f B)
	{
		Vector2f AC = new Vector2f(C);
		AC.sub(A);
		Vector2f AB = new Vector2f(B);
		AB.sub(A);
		 
		float ab2 = AB.dot(AB);
		float acab = AC.dot(AB);
		float t = acab / ab2;

		Vector2f P;
		
		if (t < 0.0f)
			P = A;
		else if (t > 1.0f)
			P = AB;
		else
		{
			AB.scale(t);
			AB.add(A);
			P = AB;
		}

		Vector2f H = new Vector2f(P);
		H.sub(C);
		
		//float h2 = H.dotProduct(H);
		//float r2 = Math.pow(r, 2);
		
		if(H.length() <= r)
			return P;
		
		return null;
		//return h2 <= r2;
	}
	
}

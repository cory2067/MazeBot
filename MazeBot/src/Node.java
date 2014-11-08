public class Node implements Comparable<Node>
{
	public static final int NONE   = 0,
							OPEN   = 1,
							CLOSED = 2; 
	
	public int x, y, g, h;
	public int belongsTo = NONE;
	public Node parent;
	
	public Node(int x, int y)
	{
		this.x = x;
		this.y = y;
	}
	
	public int compareTo(Node n) 
	{
		int f = g + h;
		int f2 = n.g + n.h;
		
		if(f > f2)
			return 1;
		else if(f < f2)
			return -1;
		return 0;
	}
	
	public void findHeuristic()
	{
		int dx = Math.abs(x - MazeBot.goal.x);
		int dy = Math.abs(y - MazeBot.goal.y);
		

		//h = 10 * (dx + dy) - 6 * Math.min(dx, dy); //diagonal distance
		h = 10 * (dx + dy); //manhattan distance
	}
	
	public int movementCost(Node n)
	{
		if(x != n.x && y != n.y)
			return 14;
		return 10;
	}
	
	public String toString()
	{
		return "(" + x + ", " + y + ")"; 
	}
}

import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.PriorityQueue;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

//uses a* to solve a maze
public class PathFinder 
{
	//public static PriorityQueue<Node> open;
	public static Node goal;
	
	private static final int FPS = 5;
	private static final int RED = 0, GREEN = 1, BLUE = 2;
	private static final double[][] COLORS = {{0, 0, 255}, {0, 255, 0}, {255, 0, 0}};
	
	
	private static JLabel label;
	private static Mat img, display;
	private static JTextArea console;
	
	public static void main(String[] args) throws InterruptedException
	{
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		//VideoCapture cam = new VideoCapture(0);
		//cam.open(0);
		
		//Highgui.imwrite("camera.jpg", capture);
		
		JFrame frame = new JFrame("MazeBot");
		frame.setLayout(new FlowLayout());
		frame.setSize(1000, 600);
		
		label = new JLabel();
		frame.add(label);
		console = new JTextArea(30, 24);
		frame.add(new JScrollPane(console));
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		//print("Starting camera feed");
		Mat map = Highgui.imread("map.jpg");
		
		//while(running)
		//{
			//Thread.sleep(FRAME_TIME);
			
		img = new Mat();
		Mat temp = map.clone();
		//Mat imgLarge = new Mat();
		
		display = new Mat();
		Imgproc.resize(map, display, new Size(320, 240));
		
		//cam.read(temp);	//read raw img (640x480)
		Imgproc.cvtColor(temp, img, Imgproc.COLOR_RGBA2GRAY); //convert to grayscale
		
		//apply adaptive threshold to convert to black and white
		Imgproc.adaptiveThreshold(img, temp, 1000, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 9, 8);
		
		//morph open to improve clarity of maze walls
		Mat kernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(4, 4));
		Imgproc.morphologyEx(temp, img, Imgproc.MORPH_OPEN, kernel);
		
		//make image smaller to improve processing speed
		Imgproc.resize(img, temp, new Size(320, 240));
		Imgproc.threshold(temp, img, 200, 255, Imgproc.THRESH_BINARY);
		
		//expand image for viewing on the GUI
		//Imgproc.resize(img, imgLarge, new Size(), 2, 2, Imgproc.INTER_AREA);
		//Imgproc.threshold(temp, imgLarge, 200, 255, Imgproc.THRESH_BINARY);
				//}
			
		//do a* algorithm stuff
		
		//used for debugging
		//for(int y = 0; y < 240; y++)
		//{
		//	for(int x = 0; x < 320; x++)
		//	{
		//		System.out.print((isOpen(x,y) ? 1 : 0) + "");
		//	}
		//	System.out.print("\n");
		//}
		
		Node[][] nodes = new Node[320][240];
		for(int y = 0; y < 240; y++)
			for(int x = 0; x < 320; x++)
				nodes[x][y] = new Node(x, y);  //initialize nodes
		
		PriorityQueue<Node> open = new PriorityQueue<Node>();
		goal = nodes[319][119];
	
		Node start = nodes[0][119];
		start.findHeuristic();
		start.g = 0;
		open.add(start);
		
		int iters = 0;
		while(open.peek() != goal)
		{
			Node current = open.poll();
			current.belongsTo = Node.CLOSED;
			drawPoint(current, BLUE);
			
			ArrayList<Node> neighbors = new ArrayList<Node>();
			int x = current.x, y = current.y;
			
			for(int a = x-1; a <= x+1; a++) //get neighbors
			{
				for(int b = y-1; b <= y+1; b++)
				{
					if(a == x && b == y)
						continue;
					if(a >= 0 && a < 320 && b >= 0 && b < 240 && isFree(a, b))
						neighbors.add(nodes[a][b]);
				}
			}
			
			for(Node n : neighbors)
			{
				int cost = current.g + n.movementCost(current);
				
				if(n.belongsTo == Node.OPEN && cost < n.g)
				{
					open.remove(n);
					n.belongsTo = Node.NONE;
				}
				if(n.belongsTo == Node.NONE)
				{
					n.g = cost;
					n.findHeuristic();
					open.add(n);
					n.belongsTo = Node.OPEN;
					n.parent = current;
					drawPoint(n, GREEN);
				}
			}
			
			iters++;
			if(iters % 20 == 0)
				updateDisplay();
		}
		
		ArrayList<Node> solution = new ArrayList<Node>();
		
		Node trace = goal;
		while(trace != null)
		{
			solution.add(0, trace);
			drawPoint(trace, RED);
			trace = trace.parent;
			updateDisplay();
		}
		
		print("Solved");
		print("Maze length: " + solution.size());
		
		ArrayList<Node> solvePath = new ArrayList<Node>();
		for(int n = 0; n < solution.size(); n += 8)
			solvePath.add(solution.get(n));
		
		if(solvePath.get(solvePath.size() - 1) != goal)
			solvePath.add(goal);
		
		//print(solvePath.size());
		//Imgproc.resize(map, display, new Size(320, 240));
		//for(Node n : solvePath)
		//	drawPoint(n, RED);
		updateDisplay();
			
	}

	public static boolean isFree(int x, int y)
	{
		return img.get(y, x)[0] != 0;
	}
	
	public static void drawPoint(Node node, int color)
	{
		display.put(node.y, node.x, COLORS[color]);
	}	
	
	public static void updateDisplay()
	{
		Mat displayLarge = new Mat();
		Imgproc.resize(display, displayLarge, new Size(), 2, 2, Imgproc.INTER_AREA);		
		label.setIcon(new ImageIcon(toBufferedImage(displayLarge)));
	}
	
	public static BufferedImage toBufferedImage(Mat m)
	{	
	    int type = BufferedImage.TYPE_BYTE_GRAY;
	    if(m.channels() > 1)
	        type = BufferedImage.TYPE_3BYTE_BGR;
	    
	    byte [] b = new byte[m.channels()*m.cols()*m.rows()];
	    m.get(0,0,b);
	    BufferedImage image = new BufferedImage(m.cols(),m.rows(), type);
	    final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
	    System.arraycopy(b, 0, targetPixels, 0, b.length);  
	    return image;
	}
	
	public static void print(int i)
	{
		print(Integer.toString(i));	
	}
	
	public static void print(String s)
	{
		console.append(s + "\n");
		console.setCaretPosition(console.getDocument().getLength());
	}
}

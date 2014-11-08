import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.PriorityQueue;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;


public class MazeBot 
{
	public static Node goal;
	private static JTextArea console;
	private static JLabel label;
	private static Mat img, display;
	private static final int RED = 0, GREEN = 1, BLUE = 2;
	private static final double[][] COLORS = {{0, 0, 255}, {0, 255, 0}, {255, 0, 0}};
	private static boolean running = true;
	
	public static void main(String[] args) throws InterruptedException
	{
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	
		
		JFrame frame = new JFrame("MazeBot");
		frame.setLayout(new FlowLayout());
		frame.setSize(980, 600);
		
		label = new JLabel();
		frame.add(label);
		console = new JTextArea(30, 24);
		frame.add(new JScrollPane(console));
		JButton startButton = new JButton("Start");
		startButton.setEnabled(false);
		startButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				running = false;
			}
		});
		frame.add(startButton);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		VideoCapture cam = new VideoCapture(0);
		cam.open(0);
		frame.setVisible(true);
		
		if(!cam.isOpened())
		{
			print("No camera detected\nPlug in webcam and restart program");
			running = false;
			Mat nocam = Highgui.imread("nocam.jpg");
			label.setIcon(new ImageIcon(toBufferedImage(nocam)));
			return;
		}
		print("Starting camera feed");
		startButton.setEnabled(true);
		
		Mat map = new Mat();
		while(running)
		{	
			img = new Mat();
			Mat temp = new Mat();
			
			cam.read(map); //read raw img (640x480)
			Imgproc.cvtColor(map, img, Imgproc.COLOR_RGBA2GRAY); //convert to grayscale
		
			
			//apply adaptive threshold to convert to black and white
			Imgproc.adaptiveThreshold(img, temp, 1000, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 9, 8);
			
			//morph open to improve clarity of maze walls
			Mat kernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(2, 2));
			Imgproc.morphologyEx(temp, img, Imgproc.MORPH_OPEN, kernel);
			
			//maybe try erode/dilate if quality is insufficient
			//kernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(1, 1));
			///Imgproc.morphologyEx(img, temp, Imgproc.MORPH_ERODE, kernel);
			//img = temp.clone();
			
			label.setIcon(new ImageIcon(toBufferedImage(img)));
		}
		
		display = new Mat(); //what will be sent to the screen
		display = map.clone();

		
		startButton.setEnabled(false);
		print("Image captured");
		
		print("Preprocessing map");
		//improve preprocessing efficiency in the future
		boolean border = false;
		int yBlock = 0, step = 2;
		for(int i = 0; i < 4; i++)
		{
			if(i == 2)
			{
				yBlock = 478;
				step = -2;
			}
			
			border = false;
			for(;; yBlock+=step)
			{
				try{
					if(!border && (!isFree(320, yBlock) || !isFree(320, yBlock+1)))
						border = true;
					else if(border && isFree(320, yBlock) && isFree(320, yBlock+1))
						break;
				} catch(Exception e) {
					print("Could not recognize a maze\nWill attempt to continue");
					break;
				}
				
				blockPoint(320, yBlock);
				blockPoint(320, yBlock+1);
			}
			border = false;
		}
		
		int startX = 0;
		border = false;
		for(;; startX += 2)
		{
			try{
				if(!border && (!isFree(startX, 240) || !isFree(startX+1, 240)))
					border = true;
				else if(border && isFree(startX, 240) && isFree(startX+1, 240))
					break;
			} catch(Exception e) {
				print("Could not find a starting point\nWill attempt to continue");
				break;
			}
		}
		
		int goalX = 638;
		border = false;
		for(;; goalX -= 2)
		{
			try{
				if(!border && (!isFree(goalX, 240) || !isFree(goalX+1, 240)))
					border = true;
				else if(border && isFree(goalX, 240) && isFree(goalX+1, 240))
					break;
			} catch(Exception e) {
				print("Could not find a goal to reach\nWill attempt to continue");
				break;
			}
		}
		
		print("Running A* algorithm");
		Node[][] nodes = new Node[640][480];
		for(int y = 0; y < 480; y++)
			for(int x = 0; x < 640; x++)
				nodes[x][y] = new Node(x, y);  //initialize nodes
		
		PriorityQueue<Node> open = new PriorityQueue<Node>();
		goal = nodes[goalX][239];
	
		Node start = nodes[startX][239];
		start.findHeuristic();
		start.g = 0;
		open.add(start);
		
		int iters = 0;
		boolean failure = false;
		while(true) //A* algorithm
		{
			Node current = open.poll();
			
			if(current == null) //if there is no solution
			{
				failure = true;
				break;
			}
			if(current == goal) //if goal is found
				break;
			
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
					if(a >= 0 && a < 640 && b >= 0 && b < 480 && isFree(a, b))
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
			if(iters % 80 == 0)
				updateDisplay();
		}
		
		if(failure)
		{
			print("Cannot solve maze");
			return;
		}
		
		print("Reached goal, retracing back to start");
		ArrayList<Node> solution = new ArrayList<Node>();
		
		Node trace = goal;
		int sIters = 0;
		while(trace != null) //draw solution
		{
			solution.add(0, trace);
			drawPoint(trace, RED);
			trace = trace.parent;		
			
			sIters++;
			if(sIters % 2 == 0)
				updateDisplay();
		}
		
		//redraw solution in bold, directly on map (somewhat inefficient)
		display = map.clone();
		for(Node n : solution)
			drawBoldPoint(n, RED);
		updateDisplay();
		
		print("Solved maze in " + iters + " iterations of A*");
		print("Maze length: " + solution.size() + " pixels");
		
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
	
	public static void blockPoint(int x, int y)
	{
		img.put(y, x, 0);
	}
	
	public static boolean isFree(int x, int y)
	{
		return img.get(y, x)[0] != 0;
	}
	
	public static void drawPoint(Node node, int color)
	{
		display.put(node.y, node.x, COLORS[color]);
	}	
	
	public static void drawBoldPoint(Node node, int color)
	{
		display.put(node.y, node.x, COLORS[color]);
		display.put(node.y, node.x+1, COLORS[color]);
		display.put(node.y+1, node.x, COLORS[color]);
		display.put(node.y+1, node.x+1, COLORS[color]);
	}
	
	public static void updateDisplay()
	{
		label.setIcon(new ImageIcon(toBufferedImage(display)));
	}
	
	public static int get(Mat m, int x, int y)
	{
		return (int) m.get(y, x)[0];
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

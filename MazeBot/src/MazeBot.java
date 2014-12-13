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
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;


public class MazeBot 
{
	public static Node goal;
	private static JTextArea console;
	private static JLabel label;
	private static JButton startButton;
	private static Mat img, display, circle;
	private static VideoCapture cam;
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
		Mat black = Mat.zeros(480, 640, 0);
		label.setIcon(new ImageIcon(toBufferedImage(black)));
		console = new JTextArea(30, 24);
		frame.add(new JScrollPane(console));
		startButton = new JButton("Start");
		startButton.setEnabled(false);
		startButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				running = false;
			}
		});
		frame.add(startButton);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		cam = new VideoCapture(0);
		cam.open(0);
		frame.setVisible(true);
		
		if(!cam.isOpened())
		{
			print("No camera detected\nPlug in webcam and restart program");
			running = false;
			label.setIcon(new ImageIcon(toBufferedImage(black)));
			return;
		}
		print("Starting camera feed");
		startButton.setEnabled(true);
		
		circle = Highgui.imread("circle.png");
		Imgproc.cvtColor(circle, circle, Imgproc.COLOR_RGBA2GRAY);
		
		//questionable infinite loop
		while(true)
		{
			running = true;
			run();
		}
	}
	
	public static void run() throws InterruptedException
	{
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
			
			Mat loc = new Mat();
			Imgproc.matchTemplate(img, circle, loc, Imgproc.TM_CCOEFF_NORMED);
			Point max = Core.minMaxLoc(loc).maxLoc;
			max.x += 18; max.y += 21;
			Core.circle(img, max, 12, new Scalar(0));
			label.setIcon(new ImageIcon(toBufferedImage(img)));
		}
		
		display = new Mat(); //what will be sent to the screen
		display = map.clone();

		
		startButton.setEnabled(false);
		print("Image captured");
		
		print("Preprocessing map");
		//improve preprocessing efficiency in the future
		
		//start experimental code
		//does not work at all
		int[] amt = new int[640];
		for(int x = 20; x < 620; x++)
		{
			for(int y = 0; y < 480; y++)
			{
				if(!isFree(x, y))
					amt[x] += (Math.pow(Math.abs(240 - y), 3)/100.0);
			}
		}
		
		int borders[] = new int[4];
		for(int i = 0; i < 4; i++)
		{
			int max = -1, maxval = -1;
			for(int a = 0; a < 640; a++)
			{
				if(amt[a] > maxval)
				{
					max = a;
					maxval = amt[a];
				}
			}
			
			if(max < 320)
			{
				for(int a = 0; a < max + 8; a++)
					amt[a] = 0;
				max += (i < 2) ? -4 : 4;
			}
			else
			{
				for(int a = 639; a > max - 8; a--)
					amt[a] = 0;
				max += (i < 2) ? 4 : -4;
			}
			
			borders[i] = max;
			for(int y = 0; y < 480; y++)
				drawPoint(new Node(max, y), RED);
			updateDisplay();
			Thread.sleep(1000);
		}
		
		//end experimental code
		
		/*boolean border = false;
		int xBlock = 0, step = 2;
		for(int i = 0; i < 4; i++)
		{
			if(i == 2)
			{
				xBlock = 638;
				step = -2;
			}
			
			border = false;
			for(;; xBlock+=step)
			{
				try{
					if(!border && (!isFree(xBlock, 240) || !isFree(xBlock+1, 240)))
						border = true;
					else if(border && isFree(xBlock, 240) && isFree(xBlock+1, 240))
						break;
				} catch(Exception e) {
					print("Could not recognize a maze\nWill attempt to continue");
					break;
				}
				
				blockPoint(xBlock, 240);
				blockPoint(xBlock+1, 240);
			}
			border = false;
		}	*/
		
		boolean border = false;
		int startY = 0;
		for(;; startY += 2)
		{
			try{
				if(!border && (!isFree(320, startY) || !isFree(320, startY+1)))
					border = true;
				else if(border && isFree(320, startY) && isFree(320, startY+1))
					break;
			} catch(Exception e) {
				print("Could not find a starting point\nWill attempt to continue");
				break;
			}
		}
		
		int goalY = 478;
		border = false;
		for(;; goalY -= 2)
		{
			try{
				if(!border && (!isFree(320, goalY) || !isFree(320, goalY+1)))
					border = true;
				else if(border && isFree(320, goalY) && isFree(320, goalY+1))
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
		//goal = nodes[320][goalY];
		goal = nodes[320][479];
	
		Node start = nodes[230][0];
		//Node start = nodes[320][startY];
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
			//DO SOMETHING HERE
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
		startButton.setEnabled(true);
		startButton.setText("Restart");
		running = true;
		while(running) 
			Thread.sleep(20);
		print("Restarting");
		startButton.setText("Start");
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

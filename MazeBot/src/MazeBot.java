import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
	//200 micrometers per step
	public static Node goal;
	public static Serial serial;
	private static JTextArea console;
	private static JLabel label;
	private static JButton startButton;
	private static JSlider erode;
	private static Mat img, display, circle, circleTop;
	private static VideoCapture cam;
	private static int erodeVal = 1;
	private static final int RED = 0, GREEN = 1, BLUE = 2;
	private static final double[][] COLORS = {{0, 0, 255}, {0, 255, 0}, {255, 0, 0}};
	private static Point markL, markR;
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
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		/*JFrame settings = new JFrame("MazeBot");
		settings.setLayout(new FlowLayout());
		settings.setSize(300, 150);*/
		erode = new JSlider(1, 24, 1);
		erode.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				erodeVal = erode.getValue();
			}
		});
		frame.add(erode);
		startButton = new JButton("Next");
		startButton.setEnabled(false);
		startButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				running = false;
			}
		});
		frame.add(startButton);
		/*settings.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		settings.setVisible(true);*/
		
		try{
			serial = new Serial();
		} catch(Exception e) {
			MazeBot.print(e.toString());
		}
		
		print("Connecting to camera...");
		cam = new VideoCapture(0);
		cam.open(0);
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
		circleTop = Highgui.imread("circleTop.png");
		Imgproc.cvtColor(circleTop, circleTop, Imgproc.COLOR_RGBA2GRAY);
		
		//questionable infinite loop
		while(true)
		{
			startButton.setText("Next");
			running = true;
			run();
		}
	}
	
	public static void run() throws InterruptedException
	{
		print("Ready to begin");
		Point penStart = null;
		Mat map = new Mat();
		int pushCount = 0;
		while(pushCount < 2)
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
			kernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(erodeVal, erodeVal));
			Imgproc.morphologyEx(img, temp, Imgproc.MORPH_ERODE, kernel);
			img = temp.clone();
			
			Mat loc = new Mat();
			
			if(pushCount == 0)
			{
				Mat top = img.rowRange(0, 240).colRange(400, 639);
				Imgproc.matchTemplate(top, circleTop, loc, Imgproc.TM_CCOEFF_NORMED);
				Point max = Core.minMaxLoc(loc).maxLoc;
				max.x += 418; max.y += 21;
				penStart = max.clone();
				Core.circle(img, max, 12, new Scalar(0,0,0));
				
				Mat left = img.rowRange(240, 479).colRange(0, 320);
				Imgproc.matchTemplate(left, circle, loc, Imgproc.TM_CCOEFF_NORMED);
				max = Core.minMaxLoc(loc).maxLoc;
				max.x += 17; max.y += 257;
				markL = max.clone();
				Core.circle(img, max, 12, new Scalar(0));
				
				Mat right = img.rowRange(240, 479).colRange(320, 639);
				Imgproc.matchTemplate(right, circle, loc, Imgproc.TM_CCOEFF_NORMED);
				max = Core.minMaxLoc(loc).maxLoc;
				max.x += 337; max.y += 257;
				markR = max.clone();
				Core.circle(img, max, 12, new Scalar(0));
			}
			
			//print(Arrays.toString(getBelts(getGlobal(new Node((int)penStart.x, (int)penStart.y)))));
			
			label.setIcon(new ImageIcon(toBufferedImage(img)));
			
			if(!running)
			{
				running = true;
				pushCount++;
				startButton.setText("Start");
			}
		}
		
		Point penCm = getGlobal(new Node((int)penStart.x, (int)penStart.y));
		penCm.x += 1.7; penCm.y += 2.4; //PEN CONSTANTS
		
		display = new Mat(); //what will be sent to the screen
		display = map.clone();

		
		startButton.setEnabled(false);
		print("Image captured");
		
		print("Preprocessing map");
		//improve preprocessing efficiency in the future
		
		boolean border = false;
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
					if(!border && (!isFree(xBlock, 240) && !isFree(xBlock+1, 240)))
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
		}
		
		border = false;
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
	
		Node start = nodes[320][0];
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
			restart();
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
		
		/*running = true;
		startButton.setEnabled(true);
		startButton.setText("Continue?");	*/		
		
		ArrayList<int[]> belts = new ArrayList<int[]>(); //steps belt len
		//double[] dist = new double[]{39.5 - penCm.x, 18 - penCm.y};
		double[] goal = new double[]{43, 18};
		
		belts.add(toSteps(getBelts(penCm)));
		//print(Arrays.toString(belts.get(0)));
		
		//print(penCm.x + " " + goal[0]);
		for(double i = penCm.x; i < goal[0]; i+=0.02)
		{
			double[] belt = getBelts(new Point(i, penCm.y));
			belts.add(toSteps(belt));
			
			/*int left = st[0] - belts.get(q)[0];
			int right = st[1] - belts.get(q)[1];	
			if(Math.abs(left) > 4 || Math.abs(right) > 4)
				print("ERROR: Step instruction exceeds 4");
			left += 4; right += 4;
			steps.add((byte) (left * 10 + right));
			
			q++;*/
		}
		//print(Arrays.toString(belts.get(1)));
		//print( penCm.y + " " + goal[1]);
		for(double i = penCm.y; i > goal[1]; i-=0.02)
		{
			double[] belt = getBelts(new Point(goal[0], i));
			belts.add(toSteps(belt));
			
			/*int left = st[0] - belts.get(q)[0];
			int right = st[1] - belts.get(q)[1];
			if(Math.abs(left) > 4 || Math.abs(right) > 4)
				print("ERROR: Step instruction exceeds 4");
			left += 4; right += 4;
			steps.add((byte) (left * 10 + right));
			q++;*/
			//print(Arrays.toString(toSteps(belt)));
		}
		
		//for(int b[] : belts)
		//	print(Arrays.toString(b));
		
		Point startPoint = getGlobal(solution.get(0));
		for(double i = goal[0]; i > startPoint.x; i-=0.02)
		{
			double[] belt = getBelts(new Point(i, goal[1]));
			belts.add(toSteps(belt));
		}
		for(double i = goal[1]; i < startPoint.y; i+=0.02)
		{
			double[] belt = getBelts(new Point(startPoint.x, i));
			belts.add(toSteps(belt));
		}
		//print(Arrays.toString(toSteps(getBelts(startPoint))));
		//print(Arrays.toString(belts.get(belts.size()-1)));
		
		ArrayList<Byte> steps = new ArrayList<Byte>(); //steps to send out
		for(int n = 0; n < solution.size(); n++)
		{
			belts.add(toSteps(getBelts(getGlobal(solution.get(n)))));
			//print(Arrays.toString(belts.get(belts.size()-1)));
		}
		
		int lower = belts.size() - solution.size();
		int lift = belts.size();
		
		Point endPoint = getGlobal(solution.get(solution.size() - 1));
		System.out.println(endPoint.y);
		for(double i = endPoint.y; i < 62; i+=0.02)
		{
			double[] belt = getBelts(new Point(endPoint.x, i));
			belts.add(toSteps(belt));
		}
		for(double i = endPoint.x; i < penCm.x; i+=0.02)
		{
			double[] belt = getBelts(new Point(i, 62));
			belts.add(toSteps(belt));
		}
		for(double i = 62; i > penCm.y; i-=0.02)
		{
			double[] belt = getBelts(new Point(penCm.x, i));
			belts.add(toSteps(belt));
		}
	
		steps.add((byte) 44);
		for(int n = 1; n < belts.size(); n++)
		{
			int left = belts.get(n)[0] - belts.get(n-1)[0];
			int right = belts.get(n)[1] - belts.get(n-1)[1];	
			if(Math.abs(left) > 4 || Math.abs(right) > 4)
			{
				print("ERROR: Step instruction exceeds 4");
				print("at " + n + "-- " + "left: " + left + "; right: " + right);
			}
			left += 4; right += 4;
			steps.add((byte) (left * 10 + right));
			//print(steps.get(n));
		}
		steps.add(0, (byte) 91);
		steps.add(lower, (byte) 90);
		steps.add(lift, (byte) 91);
		
		print("Total operations to send: " + steps.size());
		
		//if(!serial.arduinoConnected())
		//	return;
		
		//90 for pen down, 91 for pen up
		int i = 0;
		while(i < steps.size())
		{
			byte[] batch = new byte[256];
			Arrays.fill(batch, (byte) 44);
			
			for(int a = 0; a < 256; a++)
			{
				batch[a] = steps.get(i);
				i++;
				if(i >= steps.size())
					break;
			}
			
			while(!serial.ready)
				Thread.sleep(1);
			serial.sendSteps(batch);
			//print("sending");
			
			
			//print("Drawing: " + (i-256)*100/solution.size() + "%");
			//maybe add progress meter	
		}
		
		while(!serial.ready)
			Thread.sleep(1);
		//print("Drawing: 100%");
		//print(solvePath.size());
		//Imgproc.resize(map, display, new Size(320, 240));
		//for(Node n : solvePath)
		//	drawPoint(n, RED);
		updateDisplay();
		restart();
	}
	
	public static void restart() throws InterruptedException
	{
		startButton.setEnabled(true);
		startButton.setText("Restart");
		running = true;
		while(running) 
			Thread.sleep(20);
		print("Restarting");
	}
	
	//old version -- redundant trigonometry; less optimized
	/*public static Point getLocal(Node n)
	{			
		Point i = new Point(n.x, n.y);
		double distL = Math.sqrt(Math.pow(i.x - markL.x, 2) + Math.pow(i.y - markL.y, 2));
		double distR = Math.sqrt(Math.pow(i.x - markR.x, 2) + Math.pow(i.y - markR.y, 2));
		double d = Math.sqrt(Math.pow(markL.x - markR.x, 2) + Math.pow(markL.y - markR.y, 2));
		double c = Math.acos((distR*distR + d*d - distL*distL)/(2*distR*d));
		double y = distR*Math.sin(c);
		double x = d-distR*Math.cos(c);
		return new Point(x, y);
	}*/
	
	public static Point getLocal(Node n)
	{
		Point i = new Point(n.x, n.y);
		double distL = Math.sqrt(Math.pow(i.x - markL.x, 2) + Math.pow(i.y - markL.y, 2));
		double distR = Math.sqrt(Math.pow(i.x - markR.x, 2) + Math.pow(i.y - markR.y, 2));
		double d = Math.sqrt(Math.pow(markL.x - markR.x, 2) + Math.pow(markL.y - markR.y, 2));
		double cosc = (distR*distR + d*d - distL*distL)/(2*distR*d);
		double y = distR*Math.sqrt(1-cosc*cosc);
		double x = d-distR*cosc;
		if(n.y > (markR.y - markL.y)/(markR.x - markL.x)*(n.x - markL.x) + markL.y)
			y *= -1;
		return new Point(x, y);
	}
	
	//returns coordinates in cm
	public static Point getGlobal(Node n) //takes raw input
	{
		Point global = new Point();
		double d = Math.sqrt(Math.pow(markL.x - markR.x, 2) + Math.pow(markL.y - markR.y, 2));
		final double scale = 25.7/d; //25.7 / d; //constant: cm between markers, to make a scaling constant from px -> cm
		Point local = getLocal(n);
		global.x = local.x * scale + 12.8;//11.3;//13;//12;//constant: cm from edge to L
		global.y = /*47.6*/50 - local.y * scale; //constant: cm from top to marker (actually 48)
		return global;
	}
	
	public static int[] toSteps(double[] cm)
	{
		int[] steps = new int[2];
		steps[0] = (int) (cm[0] * 50 + 0.5);
		steps[1] = (int) (cm[1] * 50 + 0.5);
		return steps;
	}
	
	public static double[] getBelts(Point p) //takes a global point
	{
		final double alpha = 1.0, beta = 7.8, gamma = 51;//49.4;//50.5;//49.5;
		
		double[] belts = new double[2];
		belts[0] = Math.sqrt(Math.pow(p.x - alpha, 2) + Math.pow(p.y - beta, 2));
		belts[1] = Math.sqrt(Math.pow(gamma - p.x - alpha, 2) + Math.pow(p.y - beta, 2));
		return belts;
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

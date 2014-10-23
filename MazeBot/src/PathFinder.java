import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.xml.transform.Source;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

//uses a* to solve a maze
public class PathFinder 
{
	private static final int FPS = 5;
	private static final int RED = 0, GREEN = 1;
	private static final double[] RED_RGB = {0, 0, 255},
								  GREEN_RGB = {0, 255, 0};
	
	private static JLabel label;
	private static Mat img, display;
	private static JTextArea console;
	private static boolean running = true;
	
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
		final int FRAME_TIME = 1000/FPS;
		Mat map = Highgui.imread("map.jpg");
		
		//while(running)
		//{
			//Thread.sleep(FRAME_TIME);
			
		img = new Mat();
		Mat temp = map;
		Mat imgLarge = new Mat();
		
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
		Imgproc.resize(img, imgLarge, new Size(), 2, 2, Imgproc.INTER_AREA);
		//Imgproc.threshold(temp, imgLarge, 200, 255, Imgproc.THRESH_BINARY);
		
		label.setIcon(new ImageIcon(toBufferedImage(imgLarge)));
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
		
		for(int z = 0; z < 320; z++)
		{
			int x = (int) (Math.random() * 320);
			int y = (int) (Math.random() * 240);
			
			drawPoint(z, 50, Math.random() > 0.5 ? GREEN : RED);
		}
	}

	public static boolean isOpen(int x, int y)
	{
		return img.get(y, x)[0] != 0;
	}
	
	public static void drawPoint(int x, int y, int color)
	{
		display.put(y, x, color == RED ? RED_RGB : GREEN_RGB);
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

import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import javax.swing.ImageIcon;
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
	private static final int FPS = 5;
	
	private static JTextArea console;
	private static boolean running = true;
	
	public static void main(String[] args) throws InterruptedException
	{
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		VideoCapture cam = new VideoCapture(0);
		cam.open(0);
		
		//Highgui.imwrite("camera.jpg", capture);
		
		JFrame frame = new JFrame("MazeBot");
		frame.setLayout(new FlowLayout());
		frame.setSize(1000, 600);
		
		JLabel label = new JLabel();
		frame.add(label);
		console = new JTextArea(30, 24);
		frame.add(new JScrollPane(console));
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		print("Starting camera feed");
		final int FRAME_TIME = 1000/FPS;
		
		int iters = 0;
		while(running)
		{
			Thread.sleep(FRAME_TIME);
			
			Mat img = new Mat();
			Mat temp = new Mat();
			Mat imgLarge = new Mat();
			
			cam.read(temp);	//read raw img (640x480)
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
			
			iters++;
			if(iters == 50)
			{
				print("Took screenshot, saved to camera.jpg");
				Highgui.imwrite("camera.jpg", img);
			}
		}
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
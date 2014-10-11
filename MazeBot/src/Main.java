import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;


public class Main 
{
	public static void main(String[] args) throws InterruptedException
	{
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		VideoCapture cam = new VideoCapture(0);
		Thread.sleep(1000);
		cam.open(0);
		System.out.println("Camera opened: " + cam.isOpened());
		Mat frame = new Mat();
		
		cam.read(frame);
		
		Highgui.imwrite("camera.jpg", frame);
	}
}
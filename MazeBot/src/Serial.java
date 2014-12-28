import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;


public class Serial implements SerialPortEventListener 
{
	public boolean initiallized = false;
	public SerialPort serialPort;
        /** The port we're normally going to use. */
	private static final String PORT_NAMES[] = { 
			"/dev/ttyUSB0", // Linux
			"COM3", // Windows
			"COM4", // Windows, alternative
	};
	/**
	* A BufferedReader which will be fed by a InputStreamReader 
	* converting the bytes into characters 
	* making the displayed results codepage independent
	*/
	private BufferedReader input;
	/** The output stream to the port */
	private OutputStream output;
	/** Milliseconds to block while waiting for port open */
	private static final int TIME_OUT = 2000;
	/** Default bits per second for COM port. */
	private static final int DATA_RATE = 115200;

	public Serial() throws Exception
	{
                // the next line is for Raspberry Pi and 
                // gets us into the while loop and was suggested here was suggested http://www.raspberrypi.org/phpBB3/viewtopic.php?f=81&t=32186
                //System.setProperty("gnu.io.rxtx.SerialPorts", "/dev/ttyACM0");
		initiallized = true;
		CommPortIdentifier portId = null;
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

		//First, Find an instance of serial port as set in PORT_NAMES.
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
			for (String portName : PORT_NAMES) {
				if (currPortId.getName().equals(portName)) {
					portId = currPortId;
					break;
				}
			}
		}
		if (portId == null) {
			MazeBot.print("Could not find Arduino");
			return;
		}
		MazeBot.print("Connecting to Arduino on " + portId.getName() + "...");

	try {
			// open serial port, and use class name for the appName.
			serialPort = (SerialPort) portId.open(this.getClass().getName(),
					TIME_OUT);

			// set port parameters
			serialPort.setSerialPortParams(DATA_RATE,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
			
			Thread.sleep(1500);

			// open the streams
			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
			output = serialPort.getOutputStream();

			// add event listeners
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
		} catch (Exception e) {
			MazeBot.print(e.toString());
			return;
		}
	
	MazeBot.print("Connection established");
	}

	/**
	 * This should be called when you stop using the port.
	 * This will prevent port locking on platforms like Linux.
	 */
	public synchronized void close() {
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
	}

	/**
	 * Handle an event on the serial port. Read the data and print it.
	 */
	long startTime, endTime;
	public synchronized void serialEvent(SerialPortEvent oEvent) {
		if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {
				if(!input.ready())
					return;
				String inputLine=input.readLine();
				System.out.println("Received: " + inputLine); 
				if(inputLine.equals("data"))
				{
					System.out.println("Sending data");
					startTime = System.currentTimeMillis();
					byte[] a = new byte[128];
					for(int i = 0; i < 128; i++)
						a[i] = pairToByte(1, 4);
					output.write(a);
				}
				if(inputLine.equals("done"))
				{
					endTime = System.currentTimeMillis();
					System.out.println("Done in " + (endTime-startTime) + " ms");
				}
			} catch (Exception e) {
				MazeBot.print(e.toString());
			}
		}
		// Ignore all the other eventTypes, but you should consider the other ones.
	}

	public byte pairToByte(int l, int r)
	{
		return (byte) (l*10 + r);
	}
	
	public static byte[] toBytes(int value)
	{
		return new byte[] {
				(byte) (value >>> 8),
				(byte) (value)
		};
	}
	
	/*public static void main(String[] args) throws Exception {
		Serial main = new Serial();
		main.initialize();
		Thread t=new Thread() {
			public void run() {
				//the following line will keep this app alive for 1000 seconds,
				//waiting for events to occur and responding to them (printing incoming messages to console).
				try {Thread.sleep(1000000);} catch (InterruptedException ie) {}
			}
		};
		t.start();
		System.out.println("Started");
	}*/
}
package in.ac.iiitd.androidsyncapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

/**
 * Sends the configuration to slave and keeps Pinging every 2-3s for status
 * if it receives ACK it means the file is being downloaded by slave
 * if it Receives NAK then it changes the status of
 * o_isDownloading[deviceID] = o_isRunning[id] = false, so the part is added back to o_pool_config
 * if No ACK or NAK from slave it is assumed it is dead and above process is repeated
 * @author Suresh
 *
 */
public class BluetoothComm{

	private static final String TAG = "AndroidSync BluetoothComm";

	/**
	 * Configuration which contains 
	 * deviceID -- (int) task Scheduled on, 
	 * id -- (int) Part id, 
	 * start -- start of part block,
	 * end -- (int) end of part block, 
	 * isDone -- (String) message if download was successful/ Failed, 
	 * url -- (String) url from which to download, 
	 * size -- (int) content length, 
	 * path -- (String) directory where the part was stored in sd-card, 
	 * name -- (String) name of the file
	 * crash -- (int) crash count for the respective part
	 * isDone -- (String) Message regarding the part file success/failure.
	 */
	private Bundle o_config;

	private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
	private static final String NAME = "AndroidSync";

	private final static String EOI = "O_LAST", BUNDLE = "O_CONFIG", FILE = "O_FILE", N_FILE = "N_FILE", PART = "O_PART";

	/**
	 * States of the connection
	 */
	private static final int STATE_IDLE = 0xf000, STATE_CONNECTING = 0xf001, STATE_CONNECTED = 0xf002;

	//private static final int semaphore;

	/**
	 * True if Connection was successful
	 */
	private boolean o_conn_success;

	// Member fields
	private final BluetoothAdapter o_Adapter;
	private final Handler oh_Handler;
	private AcceptThread o_AcceptThread;
	private ConnectThread o_ConnectThread;
	private ConnectedThread o_ConnectedThread;

	// To Manage Different Connections
	private SparseArray<ConnectedThread> o_Connections;

	/** Service to execute Jobs in Sequential Order*/
	private static final ExecutorService execGlobal = null;//Executors.newSingleThreadExecutor();
	private static final ExecutorService execSendReceiveData = Executors.newSingleThreadExecutor();

	/**Local Sequential thread executor*/
	private ExecutorService execLocal;

	/**To Store the data type of config */
	public static final HashSet<String> _conString = 
			new HashSet<String>(Arrays.asList("url", "isDone", "path", "name", "ext"));

	/**
	 * Constructor
	 * @param h Handler for the UI Thread
	 * @param es ExecutorService to run tasks on
	 */
	public BluetoothComm(Handler h, ExecutorService es){
		oh_Handler = h;
		o_Adapter = BluetoothAdapter.getDefaultAdapter();
		o_Adapter.enable();
		o_Connections = new SparseArray<BluetoothComm.ConnectedThread>();
		execLocal = es;
	}

	/**
	 * Set ExecutorService to run tasks
	 * @param es new one to run on
	 */
	public synchronized void setExec(ExecutorService es){
		execLocal = es;
	}

	/**
	 * Closes all the Communication channel and Sockets
	 */
	public void reset(){
		if(o_AcceptThread != null){
			o_AcceptThread.cancel();
			o_AcceptThread = null;
		}
		if(o_ConnectedThread != null){
			o_ConnectedThread.cancel();
			o_ConnectedThread = null;
		}
		if(o_ConnectThread != null){
			o_ConnectThread.cancel();
			o_ConnectThread = null;		
		}

		for(int i=0; i<o_Connections.size(); i++){
			ConnectedThread cnt = o_Connections.get(o_Connections.keyAt(i));
			cnt.cancel();
		}
	}

	/**
	 * Starts the Listening phase of the device
	 */
	public void start(){
		o_AcceptThread = new AcceptThread();
		if(execLocal == null){
			//execGlobal.execute(o_AcceptThread);
			o_AcceptThread.start();
		}
		else{
			execLocal.execute(o_AcceptThread);
		}
	}

	/**
	 * To connect to a Bluetooth Device
	 * @param device to connect to 
	 * @param type can be HAND_SHAKE
	 */
	public synchronized void connect(BluetoothDevice device, int type){
		Log.v(TAG, "Connecting to " + device.getName());
		o_ConnectThread = new ConnectThread(device, type);

		if(execLocal == null){
			execGlobal.execute(o_ConnectThread);
		}
		else{
			execLocal.execute(o_ConnectThread);
		}
	}

	/**
	 * To Handle Connection from a {@link AcceptThread} successful event
	 * @param socket socket of remote device
	 * @param type MESSAGE_TYPE
	 */
	private void  mmSocketFromAccept(BluetoothSocket socket, int MESSAGE_TYPE){

		o_ConnectedThread = new ConnectedThread(socket);
		if(MESSAGE_TYPE == Helper.HANDSHAKE){
			if(execLocal == null){
				//execGlobal.execute(o_ConnectedThread);
				o_ConnectedThread.start();
			}
			else{
				execLocal.execute(o_ConnectedThread);
			}
		}
		o_Connections.put(0, o_ConnectedThread);
		SlaveServer.master = socket.getRemoteDevice();
	}

	/**
	 * To Handle Connection from a {@link ConnectThread} successful event
	 * @param socket socket of remote device
	 * @param type MESSAGE_TYPE
	 */
	private void mmSocketFromConnect(BluetoothSocket socket, int type) {
		// TODO Auto-generated method stub

		if(type == Helper.HANDSHAKE){
			o_ConnectedThread = new ConnectedThread(socket);
			//execGlobal.execute(o_ConnectedThread);
			o_ConnectedThread.start();

			o_Connections.put(Helper.o_no_devices++, o_ConnectedThread);
			Log.v(TAG, "Devices Incremented to " + Helper.o_no_devices);
		}
	}

	/**
	 * Send an arbitrary message using current {@link ConnectedThread}
	 * @param msg message to send
	 * @param i deviceID to which needs to send
	 */
	public boolean sendMessage(String msg){
		if(o_ConnectedThread == null) return false;
		o_ConnectedThread.write(msg);
		return false;
	}

	/**
	 * Send an arbitrary message
	 * @param msg message to send
	 * @param i deviceID to which needs to send
	 */
	public void sendMessage(String msg, int i){
		Log.v(TAG, "Sending Message to " + i);

		synchronized (this) {
			o_ConnectedThread = o_Connections.get(i);
		}		
		o_ConnectedThread.write(msg);
	}

	/**
	 * To send the file to the Master
	 * @param deviceID assigned by master
	 * @param partID
	 * @param deviceID 
	 */	
	public void sendFile(final String path, final int partID, final int deviceID) {

		Runnable r = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				final File file = new File(path);
				Log.v(TAG, "Sending Only Part File");
				o_ConnectedThread.write(N_FILE);
				o_ConnectedThread.writeInt(partID);
				o_ConnectedThread.writeInt(deviceID);
				o_ConnectedThread.writeFile(file);
			}
		};

		execSendReceiveData.execute(r);
	}

	/**
	 * To send the bundle and the file both to the Master
	 * @param obj {@link Bundle} to send which contains path to file "path"
	 * @param type Message Type
	 */
	public void sendFile(final Bundle config,final int type) {
		// TODO Auto-generated method stub

		Runnable r = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Log.v(TAG, "Sending Part File");
				o_ConnectedThread.write(FILE);
				for(String key:config.keySet()){
					Object obj = config.get(key);
					o_ConnectedThread.write((key + " " + obj));
					Log.v(TAG, "Sending " + key + " " + obj);
				}

				// To signal end of input
				o_ConnectedThread.write(EOI);

				o_ConnectedThread.writeFile(new File(config.getString("path")));
			}
		};

		execSendReceiveData.execute(r);
	}

	/**
	 * Sends a Bundle to the destination device, adds the task to an {@link ExecutorService}
	 * @param config {@link Bundle} to send
	 * @param deviceID device id to which to send
	 * @param type of message {@link Helper} like TYPE_BUNDLE, TYPE_PART
	 */
	public synchronized void sendBundle(final Bundle config, final int deviceID,final int type){

		Runnable r = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Log.v(TAG, "Sending Bundle to " + deviceID);
				final ConnectedThread ConnectedThread = o_Connections.get(deviceID);
				Log.v(TAG, "Broadcasting TYPE");
				
				if(type == Helper.TYPE_BUNDLE) ConnectedThread.write(BUNDLE);
				else if(type == Helper.TYPE_DOWNLOAD_PART_REQUEST) ConnectedThread.write(PART);
				for(String key:config.keySet()){
					Object obj = config.get(key);
					ConnectedThread.write((key + " " + obj));
					Log.v(TAG, "Sending " + key + " " + obj);
				}

				// To signal end of input
				ConnectedThread.write(EOI);
			}
		};

		execSendReceiveData.execute(r);
	}

	public void startExecGlobal(){

	}

	/**
	 * To ShutDown the local {@link ExecutorService} and wait until all scheduled tasks are completed.
	 * @return true if {@link ExecutorService} shutdown normally else false;
	 */
	public boolean isFinished(){
		execLocal.shutdown();
		boolean shutdown = false;
		try{
			shutdown = execLocal.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		}catch(InterruptedException ie){
			Log.v(TAG, "In isFinished " + ie.toString());
		}
		return shutdown;
	}

	private class AcceptThread extends Thread {
		private final BluetoothServerSocket mmServerSocket;

		public AcceptThread() {
			// Use a temporary object that is later assigned to mmServerSocket,
			// because mmServerSocket is final
			BluetoothServerSocket tmp = null;
			try {
				// MY_UUID is the app's UUID string, also used by the client code
				tmp = o_Adapter.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID);
			} catch (IOException e) { }
			mmServerSocket = tmp;
		}

		public void run() {
			BluetoothSocket socket = null;
			// Keep listening until a socket is returned
			while (true) {
				try {
					Log.v(TAG, "Server Listning");

					socket = mmServerSocket.accept();
					Log.v(TAG, "Connection Success");                
				} catch (IOException e) {
					Log.v(TAG, "Server Crashed");
					break;
				}
				// If a connection was accepted
				if (socket != null) {
					// Do work to manage the connection (in a separate thread)
					mmSocketFromAccept(socket, Helper.HANDSHAKE);
					try {
						Log.v(TAG, "Server Socket CLosed");
						mmServerSocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
			}
		}

		/** Will cancel the listening socket, and cause the thread to finish */
		public void cancel() {
			try {
				mmServerSocket.close();
			} catch (IOException e) { }
		}
	}

	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
		final int TYPE;
		private int trial;

		public ConnectThread(BluetoothDevice device, int type) {
			// Use a temporary object that is later assigned to mmSocket,
			// because mmSocket is final
			BluetoothSocket tmp = null;
			mmDevice = device;
			TYPE = type;
			trial = 0;
			Method m = null;
			// Get a BluetoothSocket to connect with the given BluetoothDevice
			try {
				// MY_UUID is the app's UUID string, also used by the server code
				try {
					m = mmDevice.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
				} catch (NoSuchMethodException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				tmp = (BluetoothSocket) m.invoke(device, MY_UUID);
			} catch (Exception e) { }
			mmSocket = tmp;
		}

		public void run() {
			// Cancel discovery because it will slow down the connection
			o_Adapter.cancelDiscovery(); 

			try {
				trial++;
				Log.v(TAG, "Trying to Connect to " + mmSocket.getRemoteDevice().getName());

				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception
				mmSocket.connect();
			} catch (IOException connectException) {
				Log.v(TAG, "Failed to Connect to " + mmSocket.getRemoteDevice().getName());
				// Unable to connect; close the socket and get out
				try {
					mmSocket.close();
				} catch (IOException closeException) { }
				if(trial > 2){
					return;
				}
				Log.v(TAG, "Retrying to connect");
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				run();
				return;
			}
			Log.v(TAG, "Success Connected to " + mmSocket.getRemoteDevice().getName());
			// Do work to manage the connection (in a separate thread)

			mmSocketFromConnect(mmSocket, TYPE);
		}

		/** Will cancel an in-progress connection, and close the socket */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) { }
		}
	}

	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) { }

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			// Keep listening to the InputStream until an exception occurs
			while (true) {
				try {
					// Read from the InputStream
					String s = ByteStream.toString(mmInStream);

					if(s.equals(BUNDLE)){
						oh_Handler.obtainMessage(Helper.TYPE_BUNDLE, -1, -1, getBundle()).sendToTarget();
						Log.v(TAG, "Bundle Transfered");
					}
					else if(s.equals(PART)){
						oh_Handler.obtainMessage(Helper.TYPE_DOWNLOAD_PART_REQUEST, -1, -1, getBundle()).sendToTarget();
					}
					else if(s.equals(FILE)){
						Log.v(TAG, "Receiving File");
						Bundle b;
						oh_Handler.obtainMessage(Helper.TYPE_PART_FROM_SLAVE, -1, -1, b = getBundle()).sendToTarget();
						Log.v(TAG, "Bundle Received");
						receiveFile(b.getString("path"));
					}
					else if(s.equals(N_FILE)){
						Log.v(TAG, "Receiving File Only");
						oh_Handler.obtainMessage(Helper.TYPE_ONLY_PART_SLAVE, ByteStream.toInt(mmInStream), ByteStream.toInt(mmInStream), null);
						receiveFile(ByteStream.toString(mmInStream));
					}
					else{
						oh_Handler.obtainMessage(Helper.TYPE_STRING, -1, -1, s).sendToTarget();
						Log.v(TAG, s);
					}


				} catch (IOException e) {
					Log.v(TAG, "Connected Thread" +e.toString());
					e.printStackTrace();
					break;
				}
			}
		}

		/**
		 * Receive file from the slave with the given {@link Bundle}
		 * @param b conigaration {@link Bundle}
		 */
		private void receiveFile(String path) {
			// TODO Auto-generated method stub
			File file = new File(path);
			try {
				Log.v(TAG, "Receiving File");
				ByteStream.toFile(mmInStream, file);
				Log.v(TAG, "File Received");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.v(TAG, e.toString());
				e.printStackTrace();
			}
		}

		/**
		 * Reads a Bundle from the Bluetooth Socket and parses it.
		 * @return Parsed Bundle which contains all the necessary iniformation.
		 * @throws IOException
		 */
		private synchronized Bundle getBundle() {
			// TODO Auto-generated method stub

			Bundle b = new Bundle();
			try{
				Log.v(TAG, "Parsing Bundle");

				String read;
				while(!(read = ByteStream.toString(mmInStream)).equals(EOI)){
					Log.v(TAG, "getB  : " + read);
					String[] part = read.split(" ");
					if(_conString.contains(part[0])){
						b.putString(part[0], part[1]);
					}
					else{
						b.putInt(part[0], Integer.parseInt(part[1]));
					}
				}

				/*
				for(String key:b.keySet()){
					Log.v(TAG, key + " " + b.get(key));
				}
				//*/

			}catch(Exception e){
				Log.v(TAG, "In getBundle" + e.toString());
			}
			return b;
		}

		/** */
		public void writeInt(final int i){
			try {
				ByteStream.toStream(mmOutStream, i);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.v(TAG, "WriteInt" + e.toString());
				e.printStackTrace();
			}
		}

		/** To write the whole file on to stream*/
		public void writeFile(final File file){
			Log.v(TAG, "Writing File...");
			try {
				ByteStream.toStream(mmOutStream, file);
			} catch (Exception e) {
				Log.v(TAG, "In WriteFile " + e.toString());
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			Log.v(TAG, "Written File...");
		}

		/** Call this from the main activity to send data to the remote device */
		public void write(String s) {
			try {
				ByteStream.toStream(mmOutStream, s);
			} catch (IOException e) { }
		}

		/** Call this from the main activity to shutdown the connection */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) { }
		}
	}
}

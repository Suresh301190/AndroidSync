package in.ac.iiitd.androidsyncapp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import android.os.Message;
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

	private static final int FROM_MASTER = 0xf000, FROM_SLAVE = 0xf001;
	
	/**
	 * States of the connection
	 */
	private static final int STATE_IDLE = 0xf000, STATE_CONNECTING = 0xf001, STATE_CONNECTED = 0xf002;
		
	/**
	 * True if Connection was successful
	 */
	private boolean o_conn_success;
	
	// Message Types
	private static final int MESSAGE_BROADCAST = 0xe000;
	private static final int MESSAGE_UNICAST = 0xe000;
	private static final int HANDSHAKE = 0xe001;
	
	// Member fields
    private final BluetoothAdapter o_Adapter;
    private final Handler oh_Handler;
    private AcceptThread o_AcceptThread;
    private ConnectThread o_ConnectThread;
    private ConnectedThread o_ConnectedThread;
    
    // To Manage Different Connections
    private SparseArray<ConnectedThread> o_Connections;
    
    /** Service to execute Jobs in Sequential Order*/
    private static final ExecutorService execGlobal = Executors.newSingleThreadExecutor();
    private static final ExecutorService execSendData = Executors.newSingleThreadExecutor();
    
    private ExecutorService execLocal;
	
	public BluetoothComm(Bundle b, Handler h, ExecutorService es){
		oh_Handler = h;
		o_config = b;
		o_Adapter = BluetoothAdapter.getDefaultAdapter();
		o_Adapter.enable();
		o_Connections = new SparseArray<BluetoothComm.ConnectedThread>();
		execLocal = es;
	}
	
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
	}
	
	/**
	 * Starts the Listening phase of the device
	 */
	public void start(){
		Log.v(TAG, "Connecting");
		o_AcceptThread = new AcceptThread();
		if(execLocal == null){
			execGlobal.execute(o_AcceptThread);
		}
		else{
			execLocal.execute(o_AcceptThread);
		}
	}
	
	public void start_s(){
		Log.v(TAG, "Connecting");
		o_AcceptThread = new AcceptThread();
		o_AcceptThread.run();
	}
	
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
	
	public boolean connect_s(BluetoothDevice device, int type){
		o_conn_success = false;
		Log.v(TAG, "Connecting to " + device.getName());
		o_ConnectThread = new ConnectThread(device, type);
		o_ConnectThread.run();
		return o_conn_success;
	}
	
	private void  mmSocketFromAccept(BluetoothSocket socket, int MESSAGE_TYPE){
		Log.v(TAG, "Connection Sucess");
		o_conn_success = true;
		
		o_ConnectedThread = new ConnectedThread(socket);
		if(MESSAGE_TYPE == FROM_SLAVE){
			if(execLocal == null){
				execGlobal.execute(o_ConnectedThread);
			}
			else{
				execLocal.execute(o_ConnectedThread);
			}
		}
	}
	
	private void mmSocketFromConnect(BluetoothSocket socket, int type) {
		// TODO Auto-generated method stub
		
		if(type == HANDSHAKE){
			o_ConnectedThread = new ConnectedThread(socket);
			execGlobal.execute(o_ConnectedThread);
			
			o_Connections.put(Helper.o_no_devices++, o_ConnectedThread);
		}
	}
	
	public boolean sendMessage(String msg){
		if(o_ConnectedThread == null) return false;
		o_ConnectedThread.write(msg);
		return false;
	}
	
	public void sendMessage(String msg, int i){
		Log.v(TAG, "Sending Message to " + i);
		o_ConnectedThread = o_Connections.get(i);
		o_ConnectedThread.write(msg);
	}
	
	public synchronized boolean sendBundle(final Bundle config, final int i){
		
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Log.v(TAG, "Sending Bundle to " + i);
				final ConnectedThread ConnectedThread = o_Connections.get(i);
				for(String key:config.keySet()){
					Object obj = config.get(key);
					ConnectedThread.write("" + obj);
					if(obj instanceof String){
						Log.v(TAG, "String " + obj);
					}
					else{
						Log.v(TAG, "Integer " + obj);
					}
				}
				
			}
		};
		
		execSendData.execute(r);
		
		return false;
	}
	
	public void startExecGlobal(){
		
	}
	
	public boolean isFinished(){
		execLocal.shutdown();
		try{
			execLocal.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		}catch(InterruptedException ie){
			
		}
		return true;
	}
	
	private class AcceptThread extends Thread {
	    private final BluetoothServerSocket mmServerSocket;
	 
	    public AcceptThread() {
	        // Use a temporary object that is later assigned to mmServerSocket,
	        // because mmServerSocket is final
	        BluetoothServerSocket tmp = null;
	        try {
	            // MY_UUID is the app's UUID string, also used by the client code
	            tmp = o_Adapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
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
	            } catch (IOException e) {
	            	break;
	            }
	            // If a connection was accepted
	            if (socket != null) {
	                // Do work to manage the connection (in a separate thread)
	                mmSocketFromAccept(socket, FROM_SLAVE);
	                try {
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
	 
	    public ConnectThread(BluetoothDevice device, int type) {
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        BluetoothSocket tmp = null;
	        mmDevice = device;
	        TYPE = type;
	 
	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	        	// MY_UUID is the app's UUID string, also used by the server code
	            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
	        } catch (IOException e) { }
	        mmSocket = tmp;
	    }
	 
	    public void run() {
	        // Cancel discovery because it will slow down the connection
	        o_Adapter.cancelDiscovery();
	 
	        try {
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
	    private final DataInputStream mmInStream;
	    private final DataOutputStream mmOutStream;
	    private StringBuffer sb;
	 
	    public ConnectedThread(BluetoothSocket socket) {
	    	sb = new StringBuffer("");
	        mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }
	 
	        mmInStream = new DataInputStream(tmpIn);
	        mmOutStream = new DataOutputStream(tmpOut);
	    }
	 
	    public void run() {
	 
	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	            try {
	                // Read from the InputStream
	                sb.append(mmInStream.readUTF());
	                
	                // Send the obtained bytes to the UI activity
	                Message msg = oh_Handler.obtainMessage();
	                msg.obj = sb.toString();
	                oh_Handler.sendMessage(msg);
	                sb.setLength(0);
	                
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void write(String s) {
	        try {
	            mmOutStream.writeUTF(s);
	        } catch (IOException e) { }
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
}

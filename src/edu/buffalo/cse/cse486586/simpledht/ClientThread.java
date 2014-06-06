
package edu.buffalo.cse.cse486586.simpledht;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Thread used to send messages from the AVD to another AVD's server.
 * 
 * @author Vivek Bhalla
 */
public class ClientThread implements Runnable {
    NodeMessage msg;

    /*
     * Constructor to accept the NodeMessage object.
     */
    public ClientThread(NodeMessage msg) {
        this.msg = msg;
    }

    @Override
    public void run() {
        try {
            // Create Socket
            Socket socket = new Socket("10.0.2.2", Integer.parseInt(msg.sendPort));

            // Initialize Output Stream
            ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(
                    socket.getOutputStream()));

            // Write the NodeMessage Object
            output.writeObject(msg);
            output.flush();
            socket.close();

        } catch (UnknownHostException e) {
            Log.e("ClientThread", "UnknownHostException");
        } catch (IOException e) {
            Log.e("ClientThread", "Socket IOException");
        }
    }
}


package edu.buffalo.cse.cse486586.simpledht;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.TreeMap;

/**
 * Thread used to receive messages from another AVD's client thread. This thread
 * is always running in the background and is listening on its socket for an
 * incoming request and keeps blocking using the accept method until it receives
 * any data. The message types join and joinreply for node joining and ring
 * forming are handled here
 * 
 * @author Vivek Bhalla
 */
public class ServerThread implements Runnable {
    static final int SERVER_PORT = 10000; // Port at which the server listens

    @SuppressWarnings("resource")
    @Override
    public void run() {

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        NodeMessage msg = new NodeMessage();
        while (true) {
            try {
                // Accept Connection and Initialize Input Stream
                Socket socket = new Socket();
                socket = serverSocket.accept();
                ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(
                        socket.getInputStream()));

                // Get Message (As NodeMessage Object)
                msg = (NodeMessage) input.readObject();

                if (msg.type.equals("join")) {

                    SimpleDhtProvider.local = false; // Set local flag to false,
                                                     // since there is a valid
                                                     // request for forming a
                                                     // ring
                    nodeJoin(msg.myPort);

                } else if (msg.type.equals("joinreply")) {

                    SimpleDhtProvider.local = false; // Set local flag to false,
                                                     // since there is a valid
                                                     // request for forming a
                                                     // ring
                    updateinfo(msg);

                } else {
                    SimpleDhtProvider.ident.process(msg);
                }

            } catch (ClassNotFoundException e) {
                Log.e("ServerThread", "ClassNotFoundException");
            } catch (IOException e) {
                Log.e("ServerThread", "IOException");
            }

        }
    }

    /*
     * Method to perform the node join operation and create the ring as per
     * Chord DHT Protocol/Algorithm
     */
    private void nodeJoin(String joinPort) {

        SimpleDhtProvider.ring.put(joinPort,
                genHash(String.valueOf(Integer.parseInt(joinPort) / 2)));

        // Initialize an ArrayList to store all the message that have to be sent
        // across the network for the ring to be formed
        ArrayList<NodeMessage> messages = new ArrayList<NodeMessage>();

        // TreeMap with its comparator overridden for getting the proper
        // sequence of ports
        TreeMap<String, String> temp = SimpleDhtProvider.ring;

        String large = SimpleDhtProvider.largestID; // Will contain the node
                                                    // with the largest ID
        String small = SimpleDhtProvider.smallestID; // Will contain the node
                                                     // with the smallest ID

        // To get largest and smallest node
        for (String port : temp.keySet()) {
            String id = temp.get(port);
            if (id.compareTo(large) > 0) {
                large = id;
            } else if (id.compareTo(small) < 0) {
                small = id;
            }
        }

        // To decide successor and predeccessor node for each node
        int count = 1;
        int last = SimpleDhtProvider.ring.size();
        for (String outport : SimpleDhtProvider.ring.keySet()) {
            String sucPort = null;
            String prePort = null;
            int incount = 1;
            for (String inport : temp.keySet()) {

                if ((count == 1) && (last == count + 1)) {
                    if (incount == count + 1) {
                        sucPort = prePort = inport;
                    }
                } else if ((count == 2) && (last == count)) {
                    if (incount == count - 1) {
                        sucPort = prePort = inport;
                    }
                } else if (incount == count + 1) {
                    sucPort = inport;
                } else if (incount == count - 1) {
                    prePort = inport;
                } else if (count == last) {
                    if (incount == 1) {
                        sucPort = inport;
                    }
                } else if (count == 1) {
                    if (incount == last) {
                        prePort = inport;
                    }
                }

                incount++;
            }
            count++;

            if (!outport.equals(SimpleDhtProvider.myPort)) {

                // Create new message to send joinreply with all the information
                // updated
                NodeMessage msg = new NodeMessage();
                msg.type = "joinreply";
                msg.myPort = SimpleDhtProvider.myPort;
                msg.sendPort = outport;
                msg.sucPort = sucPort;
                msg.prePort = prePort;
                msg.largestID = large;
                msg.smallestID = small;
                messages.add(msg);

            } else {

                SimpleDhtProvider.sucPort = sucPort;
                SimpleDhtProvider.prePort = prePort;
                SimpleDhtProvider.preID = genHash(String.valueOf(Integer.parseInt(prePort) / 2));
                SimpleDhtProvider.sucID = genHash(String.valueOf(Integer.parseInt(sucPort) / 2));
                SimpleDhtProvider.largestID = large;
                SimpleDhtProvider.smallestID = small;

            }
        }

        // Send the messages one by one from the ArrayList by creating new
        // threads for each message
        for (NodeMessage m : messages) {
            ClientThread reply = new ClientThread(m);
            Thread t = new Thread(reply);
            t.start();
        }

    }

    /*
     * Method to update the AVD's info about successor, predecessor, smallest
     * and largest node
     */
    private void updateinfo(NodeMessage msg) {
        SimpleDhtProvider.prePort = msg.prePort;
        SimpleDhtProvider.sucPort = msg.sucPort;
        SimpleDhtProvider.preID = genHash(String.valueOf(Integer.parseInt(msg.prePort) / 2));
        SimpleDhtProvider.sucID = genHash(String.valueOf(Integer.parseInt(msg.sucPort) / 2));
        SimpleDhtProvider.smallestID = msg.smallestID;
        SimpleDhtProvider.largestID = msg.largestID;
    }

    /*
     * Method to generate the hash value of a particular node or key. It uses
     * SHA-1 algorithm to generate the hash value.
     */
    @SuppressWarnings({
            "resource"
    })
    private String genHash(String input) {

        MessageDigest sha1 = null;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}

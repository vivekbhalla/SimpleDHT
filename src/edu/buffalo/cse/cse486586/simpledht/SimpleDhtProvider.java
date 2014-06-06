
package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.TreeMap;

public class SimpleDhtProvider extends ContentProvider {

    public static boolean local = true; // Flag to indicate whether operation is
                                        // local or a ring is formed
    public static String myPort; // AVD's port
    public static String prePort; // Predecessor port for this AVD
    public static String sucPort; // Successor port for this AVD
    public static String queryPort; // Used to set the port which sent the
                                    // query/delete request
    public static String myID; // Hashed ID for this AVD
    public static String preID; // Hashed ID for predecessor node of this node
    public static String sucID; // Hashed ID for successor node of this node
    public static String smallestID; // Hashed smallest node in the ring
    public static String largestID; // Hashed largest node in the ring
    Context context; // Context for the content provider
    public static Identifier ident; // Identifier class object, used to identify
                                    // the type of query and process it
    public static boolean waitOver = false; // Flag in-order to busy wait the
                                            // main thread, till a reply is
                                            // received in the server thread
    public static MatrixCursor cursor; // MatrixCusor object used to store the cursor returned by query
    public static HashMap<String, String> loaclCursor = new HashMap<String, String>(); // Used to retrieve response of local query 
                                                                                       // and pass it on to the messages hashmap
    public static boolean queryFlag = false; // Flag to indicate that a query type message has been received from another 
                                             //  node and a queryreply type of message has to be created
    private static final String COLUMN_1 = "key";
    private static final String COLUMN_2 = "value";
    public static TreeMap<String, String> ring; // TreeMap used for ordering of ring nodes

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        
        String[] columnNames = {
                COLUMN_1, COLUMN_2
        };
        cursor = new MatrixCursor(columnNames);
        
        if (!local) {
        
            if (selection.equals("*")) {
            
                return delGlobal(uri);

            } else if (selection.equals("@")) {

                return delLocal(uri);
            
            } else {
            
                String key = null;
                key = genHash(selection);
                int compC = key.compareTo(myID);
                int compP = key.compareTo(preID);
                int compFirst = myID.compareTo(preID);

                if ((compP > 0) && (compC <= 0) && (compFirst > 0)) {

                    return localDelete(columnNames, selection);
                
                } else if (myID.equals(smallestID) && (key.compareTo(largestID) > 0)) {

                    return localDelete(columnNames, selection);
                
                } else if (compC > 0) {

                    sendDelete(uri, selection, sucPort);
                
                } else if (myID.equals(smallestID) && (compC <= 0)) {

                    return localDelete(columnNames, selection);
                
                } else if ((compC < 0) && (compP <= 0)) {

                    sendDelete(uri, selection, prePort);
                
                }
            }
        
        } else {
        
            if (selection.equals("*") || selection.equals("@")) {

                return delLocal(uri);
            
            } else {

                return localDelete(columnNames, selection);
            
            }
        }
        return 0;
    }

    private int delLocal(Uri uri) {
        
        String selection = null;
        int count = 0;
        try {
            File file = new File(context.getFilesDir().getAbsolutePath());
            for (File f : file.listFiles()) {
                selection = f.getName();
                Log.v("delLocal", selection);
                if (f.delete()) {
                    count++;
                }

            }
        } catch (Exception e) {
            Log.e("delLocal", "file read failed");
        }
        return count;
    }

    private int delGlobal(Uri uri) {
        String selection = null;
        int count = 0;
        try {
            File file = new File(context.getFilesDir().getAbsolutePath());
            for (File f : file.listFiles()) {
                selection = f.getName();
                Log.v("delGlobal", selection);
                if (f.delete()) {
                    count++;
                }

            }
        } catch (Exception e) {
            Log.e("delGlobal", "file read failed");
        }

        // Create Global Delete message to delete all <key,value> pairs from all nodes
        NodeMessage globalDelete = new NodeMessage();
        globalDelete.type = "globaldelete";
        globalDelete.myPort = myPort;
        globalDelete.sendPort = sucPort;
        globalDelete.queryPort = queryPort;
        globalDelete.uri = uri.toString();
        
        ClientThread delete = new ClientThread(globalDelete);
        Thread t = new Thread(delete);
        t.start();
        return count;
    }

    private void sendDelete(Uri uri, String selection, String port) {
        
        NodeMessage deletemsg = new NodeMessage();
        deletemsg.type = "delete";
        deletemsg.myPort = myPort;
        deletemsg.sendPort = port;
        deletemsg.uri = uri.toString();
        deletemsg.selection = selection;

        ClientThread delete = new ClientThread(deletemsg);
        Thread t = new Thread(delete);
        t.start();
        
        Log.v("Send delete to " + port, selection);
    }

    private int localDelete(String[] columnNames, String selection) {
        
        try {

            File file = new File(context.getFilesDir().getAbsolutePath() + "/" + selection);
            if (file.delete()) {
                return 1;
            }
            Log.v("Local Delete", selection);

        } catch (Exception e) {
            Log.e("localDelete", "file delete failed");
        }
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        
        String filename = values.getAsString(COLUMN_1);
        
        if (!local) {
        
            String key = null;
            key = genHash(filename);
            int compC = key.compareTo(myID);
            int compFirst = myID.compareTo(preID);
            int compP = key.compareTo(preID);

            if ((compP > 0) && (compC <= 0) && (compFirst > 0)) {

                filewrite(values, filename);
            
            } else if (myID.equals(smallestID) && (key.compareTo(largestID) > 0)) {

                filewrite(values, filename);
            
            } else if (compC > 0) {

                sendMsg(values, uri, sucPort);
            
            } else if (myID.equals(smallestID) && (compC <= 0)) {

                filewrite(values, filename);
            
            } else if ((compC < 0) && (compP <= 0)) {

                sendMsg(values, uri, prePort); // NOTE!!
            
            }
        
        } else {

            filewrite(values, filename);
        
        }
        return uri;
    }

    public void filewrite(ContentValues values, String filename) {
        
        try {

            File file = new File(context.getFilesDir().getAbsolutePath(), filename);
            FileWriter fileWrite = new FileWriter(file);
            fileWrite.write(values.getAsString(COLUMN_2));
            fileWrite.close();
        
            Log.v("Local Insert", values.toString());
            
        } catch (Exception e) {
            Log.e("filewrite", "file write failed");
        }
    }

    public void sendMsg(ContentValues values, Uri uri, String port) {
        
        NodeMessage insertMsg = new NodeMessage();
        insertMsg.type = "insert";
        insertMsg.myPort = myPort;
        insertMsg.sendPort = port;
        insertMsg.uri = uri.toString();
        String[] temp = {
                values.getAsString(COLUMN_1), values.getAsString(COLUMN_2)
        };
        insertMsg.cv = temp;
        
        ClientThread insert = new ClientThread(insertMsg);
        Thread t = new Thread(insert);
        t.start();
        
        Log.v("Send insert to " + port, values.toString());
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
       
        String[] columnNames = {
                COLUMN_1, COLUMN_2
        };
        cursor = new MatrixCursor(columnNames);
        
        if (!local) {
           
            if (selection.equals("*")) {

                getGlobal(uri);
            
            } else if (selection.equals("@")) {

                getLocal(uri);
            
            } else {
             
                String key = null;
                key = genHash(selection);
                int compC = key.compareTo(myID);
                int compP = key.compareTo(preID);
                int compFirst = myID.compareTo(preID);
              
                if ((compP > 0) && (compC <= 0) && (compFirst > 0)) {

                    return localQuery(columnNames, selection);
                
                } else if (myID.equals(smallestID) && (key.compareTo(largestID) > 0)) {

                    return localQuery(columnNames, selection);
                
                } else if (compC > 0) {

                    sendQuery(uri, selection, sucPort);
                
                } else if (myID.equals(smallestID) && (compC <= 0)) {

                    return localQuery(columnNames, selection);
               
                } else if ((compC < 0) && (compP <= 0)) {

                    sendQuery(uri, selection, prePort);
               
                }
            }
        
        } else {
           
            if (selection.equals("*") || selection.equals("@")) {

                getLocal(uri);
            
            } else {

                return localQuery(columnNames, selection);
           
            }
        }
        return cursor;

    }

    private void sendQuery(Uri uri, String selection, String port) {
        
        NodeMessage queryMsg = new NodeMessage();
        queryMsg.type = "query";
        queryMsg.myPort = myPort;
        queryMsg.sendPort = port;
        queryMsg.uri = uri.toString();
        queryMsg.selection = selection;
        
        if (!queryFlag) {
            queryMsg.queryPort = myPort;
        } else {
            queryMsg.queryPort = queryPort;
        }
        
        ClientThread query = new ClientThread(queryMsg);
        Thread t = new Thread(query);
        t.start();
        
        if (!queryFlag) {
            while (!waitOver) {

            }
            waitOver = false;
        }

        queryFlag = false;
        queryPort = null;
    }

    private Cursor localQuery(String[] columnNames, String selection) {
       
        MatrixCursor temp = new MatrixCursor(columnNames);
        HashMap<String, String> tempMap = new HashMap<String, String>();
        BufferedReader reader;
        
        try {

            File file = new File(context.getFilesDir().getAbsolutePath());
            reader = new BufferedReader(new FileReader(file + "/" + selection));
            String value = reader.readLine();
            String[] row = {
                    selection, value
            };

            tempMap.put(selection, value);
            cursor.addRow(row);
            temp.addRow(row);
            
            reader.close();
           
            Log.v("Local Query", selection);

        } catch (Exception e) {
            Log.e("localQuery", "file read failed");
        }

        if (queryFlag) {
           
            NodeMessage queryReply = new NodeMessage();
            queryReply.type = "queryreply";
            queryReply.myPort = myPort;
            queryReply.sendPort = queryPort;
            queryReply.globalCursor = tempMap;
            
            ClientThread queryR = new ClientThread(queryReply);
            Thread t = new Thread(queryR);
            t.start();
        }

        queryFlag = false;
        queryPort = null;
        return temp;
    }

    private void getGlobal(Uri uri) {
        
        HashMap<String, String> globalCursor = new HashMap<String, String>();
        BufferedReader reader = null;
        String selection = null;
        
        try {
            File file = new File(context.getFilesDir().getAbsolutePath());
            for (File f : file.listFiles()) {
                selection = f.getName();
                reader = new BufferedReader(new FileReader(file + "/" + selection));
                String value = reader.readLine();
                String[] row = {
                        selection, value
                };

                globalCursor.put(selection, value);
                cursor.addRow(row);
            }

            reader.close();
        
        } catch (Exception e) {
            Log.e("getGlobal", "file read failed");
        }

        NodeMessage globalQuery = new NodeMessage();
        globalQuery.type = "globalquery";
        globalQuery.globalCursor = globalCursor;
        globalQuery.myPort = myPort;
        globalQuery.sendPort = sucPort;
        globalQuery.queryPort = myPort;
        globalQuery.uri = uri.toString();
       
        ClientThread global = new ClientThread(globalQuery);
        Thread t = new Thread(global);
        t.start();

        while (!waitOver) {

        }
        waitOver = false;
   
    }

    private void getLocal(Uri uri) {
        
        BufferedReader reader = null;
        String selection = null;
        
        try {
            File file = new File(context.getFilesDir().getAbsolutePath());
            for (File f : file.listFiles()) {
                selection = f.getName();
                Log.v("getLocal", selection);
                reader = new BufferedReader(new FileReader(file + "/" + selection));
                String value = reader.readLine();
                String[] row = {
                        selection, value
                };

                loaclCursor.put(selection, value);
                cursor.addRow(row);
            
            }
            
            reader.close();

        } catch (Exception e) {
            Log.e("getLocal", "file read failed");
        }

        if (queryFlag) {
           
            NodeMessage globalQuery = new NodeMessage();
            globalQuery.type = "globalquery";
            globalQuery.globalCursor = loaclCursor;
            globalQuery.myPort = myPort;
            globalQuery.sendPort = sucPort;
            globalQuery.queryPort = queryPort;
            globalQuery.uri = uri.toString();
            
            ClientThread global = new ClientThread(globalQuery);
            Thread t = new Thread(global);
            t.start();
        }
        
        queryFlag = false;
        queryPort = null;
    
    }

    @Override
    public boolean onCreate() {

        ServerThread server = new ServerThread();
        Thread serverThread = new Thread(server);
        serverThread.start();

        context = getContext();
        ident = new Identifier(context);

        myPort = getMyPort();
        myID = genHash(String.valueOf(Integer.parseInt(myPort) / 2));
        sucPort = prePort = myPort;
        sucID = preID = smallestID = largestID = myID;

        if (!myPort.equals("11108")) {
            
            NodeMessage msg = new NodeMessage();
            msg.type = "join";
            msg.myPort = myPort;
            msg.sendPort = "11108";
            
            ClientThread join = new ClientThread(msg);
            Thread t = new Thread(join);
            t.start();
            
        } else {

            ring = new TreeMap<String, String>(new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    o1 = genHash(String.valueOf(Integer.parseInt(o1) / 2));
                    o2 = genHash(String.valueOf(Integer.parseInt(o2) / 2));
                    return o1.compareTo(o2);
                }
            });
            
            ring.put(myPort, genHash(String.valueOf(Integer.parseInt(myPort) / 2)));
        }

        return true;
    }

    /*
     * Method to generate the hash value of a particular node or key. It uses
     * SHA-1 algorithm to generate the hash value.
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

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

    /*
     * Calculate the port number that this AVD listens on.
     * 
     */
    public String getMyPort() {
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(
                Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        return myPort;
    }
}

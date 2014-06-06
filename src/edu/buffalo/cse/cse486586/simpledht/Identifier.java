
package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentValues;
import android.content.Context;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import java.util.HashMap;

/**
 * Class to Identify the type of message in the NodeMessage object and perform
 * actions accordingly. The various types of messages are - insert, query,
 * queryreply, globalquery, delete, globaldelete. The message types - join and
 * joinreply are handled in the server thread itself.
 * 
 * @author Vivek Bhalla
 */
public class Identifier {
    Context context;
    private static final String COLUMN_1 = "key";
    private static final String COLUMN_2 = "value";
    private MatrixCursor cursor;

    /*
     * Constructor accepts the context for the ContentProvider passed from the
     * Server Thread.
     */
    public Identifier(Context context) {
        this.context = context;
    }

    /*
     * Method to actually process the message type request.
     */
    public void process(NodeMessage msg) {
        if (msg.type.equals("insert")) {

            ContentValues cv = new ContentValues();
            cv.put(COLUMN_1, msg.cv[0]);
            cv.put(COLUMN_2, msg.cv[1]);
            Uri uri = Uri.parse(msg.uri);
            context.getContentResolver().insert(uri, cv);

        } else if (msg.type.equals("query")) {

            SimpleDhtProvider.queryFlag = true;
            SimpleDhtProvider.queryPort = msg.queryPort;
            String[] columnNames = {
                    COLUMN_1, COLUMN_2
            };
            cursor = new MatrixCursor(columnNames);
            Uri uri = Uri.parse(msg.uri);
            context.getContentResolver().query(uri,
                    null, msg.selection, null, null);

        } else if (msg.type.equals("queryreply")) {

            String[] columnNames = {
                    COLUMN_1, COLUMN_2
            };
            cursor = new MatrixCursor(columnNames);
            for (String key : msg.globalCursor.keySet()) {
                String[] row = {
                        key, msg.globalCursor.get(key)
                };
                cursor.addRow(row);
            }
            SimpleDhtProvider.cursor = cursor;
            SimpleDhtProvider.waitOver = true;

        } else if (msg.type.equals("globalquery")) {

            String[] columnNames = {
                    COLUMN_1, COLUMN_2
            };
            cursor = new MatrixCursor(columnNames);
            if (!msg.queryPort.equals(SimpleDhtProvider.myPort)) {

                SimpleDhtProvider.queryFlag = true;
                SimpleDhtProvider.queryPort = msg.queryPort;
                Uri uri = Uri.parse(msg.uri);
                SimpleDhtProvider.loaclCursor = msg.globalCursor;
                context.getContentResolver().query(uri, null, "@", null,
                        null);

            } else {

                cursor = new MatrixCursor(columnNames);
                HashMap<String, String> globalMap = msg.globalCursor;
                for (String key : globalMap.keySet()) {
                    String[] row = {
                            key, globalMap.get(key)
                    };
                    cursor.addRow(row);
                }
                SimpleDhtProvider.cursor = cursor;
                SimpleDhtProvider.waitOver = true;

            }

        } else if (msg.type.equals("delete")) {

            Uri uri = Uri.parse(msg.uri);
            context.getContentResolver().delete(uri, msg.selection, null);

        } else if (msg.type.equals("globaldelete")) {

            if (!msg.queryPort.equals(SimpleDhtProvider.myPort)) {
                SimpleDhtProvider.queryPort = msg.queryPort;
                Uri uri = Uri.parse(msg.uri);
                context.getContentResolver().delete(uri, "@", null);
            }

        } else {
            Log.e("Identifier", "Invalid Message");
        }
    }
}

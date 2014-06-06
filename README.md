SimpleDHT
=========

Android based Distributed Hash Table

This application is a simple DHT based on Chord.
Although the design is based on Chord, it is a simplified version of Chord.
There is no support of finger tables and finger-based routing. Also node leaves/failures are not handled. 

Therefore, there are three things which have been implemented: 

  1. ID space partitioning/re-partitioning
  2. Ring-based routing
  3. Node joins

The content provider implements all DHT functionalities and supports insert query and delete operations.
Thus, if you multiple instances of the app are run, all content provider instances form a Chord ring and
serve insert/query requests in a distributed fashion according to the Chord protocol.

SHA-1 is used as the hash function to generate keys.
The following code snippet takes a string and generates a SHA-1 hash as a hexadecimal string.
Given two keys, you can use the standard lexicographical string comparison to determine which one is greater in order to determine its position in the ring.

    import java.security.MessageDigest;
    import java.security.NoSuchAlgorithmException;
    import java.util.Formatter;                                                                                                                                                             
    private String genHash(String input) throws NoSuchAlgorithmException {
      
      MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
      byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
      
      for (byte b : sha1Hash) {
        formatter.format("%02x", b);
      }
      
      return formatter.toString();
      
    }

Use the python scripts to create, run and set the ports of the AVD’s, by using the following commands 

    python create_avd.py 5
    python run_avd.py 5
    python set_redir.py 10000

The redirection ports for the AVD’s will be-
  
    emulator-5554: “5554” - 11108
    emulator-5556: “5556” - 11112
    emulator-5558: “5558” - 11116
    emulator-5560: “5560” - 11120
    emulator-5562: “5562” - 11124

There is support for two special strings for the selection parameter for query() and delete()

  1.	If “*” (a string with a single character *) is given as the selection parameter to query(), the all <key, value> pairs stored in the entire DHT are returned.
  2.	Similarly, if “*” is given as the selection parameter to delete(), then all <key, value> pairs stored in the entire DHT are deleted.
  3.	If “@” (a string with a single character @) is given as the selection parameter to query() on an AVD, then all <key, value> pairs stored in the local partition of the node, i.e., all <key, value> pairs stored locally in the AVD on which query() is run are retured.
  4.	Similarly, if “@” is given as the selection parameter to delete() on an AVD, then all <key, value> pairs stored in the local partition of the node, i.e., all <key, value> pairs stored locally in the AVD on which delete() is run are deleted

The content provider implements ring-based routing. Following the design of Chord, the content provider maintains predecessor and successor pointers and forwards each request to its successor until the request arrives at the correct node. Once the correct node receives the request, it processes it and returns the result (directly or recursively) to the original content provider instance that first received the request.

  1.  The port numbers (11108, 11112, 11116, 11120, & 11124) have been fixed and act as your successor and predecessor pointers.
  2.  The content provider should handles new node joins. For this, we need to have the first emulator instance (i.e., emulator-5554) receive all new node join requests. Upon completing a new node join request, affected nodes will have updated their predecessor and successor pointers correctly.
  3.	There is no support to handle concurrent node joins. A node join will only happen once the system completely processes the previous join.
  4.	There is no support to handle insert/query requests while a node is joining. All insert/query requests are issued only with a stable system.
  5.	There is no support to handle node leaves/failures.

There is a tester for checking if everything is working properly.


**Note:** The python scripts and tester are provided by [Prof. Steve Ko](http://www.cse.buffalo.edu/people/?u=stevko) from the University at Buffalo.

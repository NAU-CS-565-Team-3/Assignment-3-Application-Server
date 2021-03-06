package appserver.client;

import appserver.comm.Message;
import appserver.comm.MessageTypes;
import static appserver.comm.MessageTypes.JOB_REQUEST;
import appserver.job.Job;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Properties;

import utils.PropertyHandler;

/**
 * Class [FibonacciClient] A primitive Fibonacci client that uses the Fibonacci tool
 * 
 * @author Jacob Kaufman
 */
public class FibonacciClient extends Thread implements MessageTypes{
    
    String host = null;
    int port;
    int sequenceNumber;

    Properties properties;
    /** 
     * Initializer
     * @param serverPropertiesFile - server config file
     * @param currentNumber - current Fibonacci sequence number 
     */
    public FibonacciClient(String serverPropertiesFile, int currentNumber) {
        try {
            properties = new PropertyHandler(serverPropertiesFile);
            host = properties.getProperty("HOST");
            System.out.println("[PlusOneClient.PlusOneClient] Host: " + host);
            port = Integer.parseInt(properties.getProperty("PORT"));
            System.out.println("[PlusOneClient.PlusOneClient] Port: " + port);
            this.sequenceNumber = currentNumber;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Runs FibonacciClient for each sequence number. Connects to the applications server and creates the job to obtain
     * each result.
     */
    public void run() {
        try { 
            // connect to application server
            Socket server = new Socket(host, port);
            
            // hard-coded string of class, aka tool name ... plus one argument
            String classString = "appserver.job.impl.Fibonacci";
            
            // create job and job request message
            Job job = new Job(classString, sequenceNumber);
            Message message = new Message(JOB_REQUEST, job);
            
            // sending job out to the application server in a message
            ObjectOutputStream writeToNet = new ObjectOutputStream(server.getOutputStream());
            writeToNet.writeObject(message);
            
            // reading result back in from application server
            // for simplicity, the result is not encapsulated in a message
            ObjectInputStream readFromNet = new ObjectInputStream(server.getInputStream());
            long result = (long) readFromNet.readObject();
            System.out.println("Fibonacci of " + sequenceNumber +  ": " + result);
        } catch (Exception ex) {
            System.err.println("[FibonacciClient.run] Error occurred");
            ex.printStackTrace();
        }
    }

    /**
     * Run the fibonacci Client for sequence numbers 0 - 47
     * @param args - command line arguments
     */
    public static void main(String[] args) {
        for(int i = 0; i < 48; i++ ){
            (new FibonacciClient("../../config/Server.properties",i)).start();
        }
    }  
}

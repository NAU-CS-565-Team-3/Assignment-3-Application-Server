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
    
    public void run() {
        try { 
            // connect to application server
            Socket server = new Socket(host, port);
            
            // hard-coded string of class, aka tool name ... plus one argument
            String classString = "appserver.job.impl.Fibonnaci";
            
            // create job and job request message
            Job job = new Job(classString, sequenceNumber);
            Message message = new Message(JOB_REQUEST, job);
            
            // sending job out to the application server in a message
            ObjectOutputStream writeToNet = new ObjectOutputStream(server.getOutputStream());
            writeToNet.writeObject(message);
            
            // reading result back in from application server
            // for simplicity, the result is not encapsulated in a message
            ObjectInputStream readFromNet = new ObjectInputStream(server.getInputStream());
            Integer result = (Integer) readFromNet.readObject();
            System.out.println("Fibonacci of " + sequenceNumber +  ": " + result);
        } catch (Exception ex) {
            System.err.println("[FibonacciClient.run] Error occurred");
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        for(int i = 0; i < 48; i++ ){
            (new FibonacciClient("../../config/Server.properties",i)).start();
        }
    }  
}

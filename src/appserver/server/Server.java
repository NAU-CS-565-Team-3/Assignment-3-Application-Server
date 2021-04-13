package appserver.server;

import appserver.comm.Message;
import static appserver.comm.MessageTypes.JOB_REQUEST;
import static appserver.comm.MessageTypes.REGISTER_SATELLITE;
import appserver.comm.ConnectivityInfo;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.PropertyHandler;

/**
 *
 * @author Dr.-Ing. Wolf-Dieter Otte
 */
public class Server {

    // Singleton objects - there is only one of them. For simplicity, this is not enforced though ...
    static SatelliteManager satelliteManager = null;
    static LoadManager loadManager = null;
    static ServerSocket serverSocket = null;

    public Server(String serverPropertiesFile) {

        // create satellite manager and load manager
        satelliteManager = new SatelliteManager();
        loadManager = new LoadManager();
        
        // read server properties and create server socket
        try
        {
            PropertyHandler serverProperties = new PropertyHandler(serverPropertiesFile);
            int port = Integer.parseInt(serverProperties.getProperty("PORT"));
            serverSocket = new ServerSocket(port);
            
        } catch(IOException e)
        {
            System.err.println(e);
            System.exit(1);
        }
    }

    public void run() {
        // serve clients in server loop ...
        // when a request comes in, a ServerThread object is spawned
        while(true)
        {
            try{
                (new ServerThread(serverSocket.accept())).start();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    
    }

    // objects of this helper class communicate with satellites or clients
    private class ServerThread extends Thread {

        Socket client = null;
        ObjectInputStream readFromNet = null;
        ObjectOutputStream writeToNet = null;
        Message message = null;

        private ServerThread(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            // set up object streams and read message
            try {
                readFromNet = new ObjectInputStream(client.getInputStream());
                writeToNet = new ObjectOutputStream(client.getOutputStream());
                
                message = (Message) readFromNet.readObject();
                
            } catch (IOException | ClassNotFoundException e){
                System.err.println(e);
            } 
       
            
            // process message
            switch (message.getType()) {
                case REGISTER_SATELLITE:
                    // read satellite info
                    ConnectivityInfo newSattelite = (ConnectivityInfo) message.getContent();
                    
                    // register satellite
                    synchronized (Server.satelliteManager)
                    {
                        Server.satelliteManager.registerSatellite(newSattelite);
                    }
                    
                    // add satellite to loadManager
                    synchronized (Server.loadManager)
                    {
                        Server.loadManager.satelliteAdded(newSattelite.getName());
                    }
                    break;

                case JOB_REQUEST:
                    System.err.println("\n[ServerThread.run] Received job request");
                    ConnectivityInfo currentSatellite;

                    String satelliteName = null;
                    synchronized (Server.loadManager) {
                        try {
                            // get next satellite from load manager
                            satelliteName = Server.loadManager.nextSatellite();
                            System.out.println("[ServerThread.run] Grabbing Server " + satelliteName);
                        } catch (Exception e) {
                            System.err.println(e);
                        }
                            
                        // get connectivity info for next satellite from satellite manager
                        currentSatellite = Server.satelliteManager.getSatelliteForName(satelliteName);
                    }
                    
                    try {   
                        // connect to satellite
                        Socket satellite = new Socket(currentSatellite.getHost(), currentSatellite.getPort());
                        System.out.println("[ServerThread.run] Connection Created to " + satelliteName);
                        
                        // open object streams,
                        ObjectOutputStream toSatellite = new ObjectOutputStream(satellite.getOutputStream());
                        
                        // forward message (as is) to satellite,
                        toSatellite.writeObject(message);
                        
                        // receive result from satellite and
                        ObjectInputStream fromSatellite = new ObjectInputStream(satellite.getInputStream());
                        Object result = fromSatellite.readObject();
                        // write result back to client
                        writeToNet.writeObject(result);
                        
                    } catch (IOException | ClassNotFoundException e) {
                        System.err.println(e);
                    }
                    
                    break;

                default:
                    System.err.println("[ServerThread.run] Warning: Message type not implemented");
            }
        }
    }

    // main()
    public static void main(String[] args) {
        // start the application server
        Server server = null;
        if(args.length == 1) {
            server = new Server(args[0]);
        } else {
            server = new Server("../../config/Server.properties");
        }
        server.run();
    }
}

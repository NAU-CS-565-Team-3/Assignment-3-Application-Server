package appserver.satellite;

import appserver.job.Job;
import appserver.comm.ConnectivityInfo;
import appserver.job.UnknownToolException;
import appserver.comm.Message;
import static appserver.comm.MessageTypes.JOB_REQUEST;
import static appserver.comm.MessageTypes.REGISTER_SATELLITE;
import appserver.job.Tool;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.PropertyHandler;

/**
 * Class [Satellite] Instances of this class represent computing nodes that execute jobs by
 * calling the callback method of tool a implementation, loading the tool's code dynamically over a network
 * or locally from the cache, if a tool got executed before.
 *
 * @author Dr.-Ing. Wolf-Dieter Otte
 */
public class Satellite extends Thread {

    private ConnectivityInfo satelliteInfo = new ConnectivityInfo();
    private ConnectivityInfo serverInfo = new ConnectivityInfo();
    private HTTPClassLoader classLoader = null;
    private Hashtable toolsCache = null;

    public Satellite(String satellitePropertiesFile, String classLoaderPropertiesFile, String serverPropertiesFile) {

        // read this satellite's properties and populate satelliteInfo object,
        // which later on will be sent to the server
        // --------------------------
        try {
            PropertyHandler satelliteConfig = new PropertyHandler(satellitePropertiesFile);
            satelliteInfo.setName(satelliteConfig.getProperty("NAME"));
            satelliteInfo.setPort(Integer.parseInt(satelliteConfig.getProperty("PORT")));
            satelliteInfo.setHost("127.0.0.1");
        } catch (IOException e) {
            System.err.println(e);
            System.exit(1);
        }

        // read properties of the application server and populate serverInfo object
        // other than satellites, the as doesn't have a human-readable name, so leave it out
        // --------------------------------
        try {
            PropertyHandler serverConfig = new PropertyHandler(serverPropertiesFile);
            serverInfo.setPort(Integer.parseInt(serverConfig.getProperty("PORT")));
            serverInfo.setHost(serverConfig.getProperty("HOST"));
        } catch (IOException e) {
            System.err.println(e);
            System.exit(1);
        }

        // read properties of the code server and create class loader
        // -------------------
        try {
            PropertyHandler classLoaderConfig = new PropertyHandler(classLoaderPropertiesFile);
            classLoader = new HTTPClassLoader(classLoaderConfig.getProperty("HOST"), 
                    Integer.parseInt(classLoaderConfig.getProperty("PORT")));
        } catch (IOException e) {
            System.err.println(e);
            System.exit(1);
        }

        // create tools cache
        // -------------------
        toolsCache = new Hashtable();

        
    }

    @Override
    public void run() {

        // register this satellite with the SatelliteManager on the server
        // ---------------------------------------------------------------
        try {
            Socket server = new Socket(serverInfo.getHost(), serverInfo.getPort());
            Message registerMsg = new Message(REGISTER_SATELLITE, satelliteInfo);
            ObjectOutputStream toNetwork = new ObjectOutputStream(server.getOutputStream());
            System.out.println("[Satellite.run] Register Satellite: " + satelliteInfo.getName());
            toNetwork.writeObject(registerMsg);
        } catch (IOException e) {
            System.err.println(e);
        }
        // create server socket
        // ---------------------------------------------------------------
        try {
            ServerSocket socket = new ServerSocket(satelliteInfo.getPort());
            System.out.println("[Satellite.run] Socket Created on Port :" + satelliteInfo.getPort());

            // start taking job requests in a server loop
            // ---------------------------------------------------------------
            while (true) {
                new SatelliteThread(socket.accept(), this).run();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    // inner helper class that is instanciated in above server loop and processes single job requests
private class SatelliteThread extends Thread {

        Satellite satellite = null;
        Socket jobRequest = null;
        ObjectInputStream readFromNet = null;
        ObjectOutputStream writeToNet = null;
        Message message = null;

        SatelliteThread(Socket jobRequest, Satellite satellite) {
            this.jobRequest = jobRequest;
            this.satellite = satellite;
        }

        @Override
        public void run() {
            try {
                // setting up object streams
                readFromNet = new ObjectInputStream(jobRequest.getInputStream());
                writeToNet = new ObjectOutputStream(jobRequest.getOutputStream());

                // reading message
                message = (Message) readFromNet.readObject();
            
                switch (message.getType()) {
                    case JOB_REQUEST:
                        System.out.println("[SatelliteThread.run] Received New Job Request.");
                        try {
                            Job requestedJob = (Job) message.getContent();
                            String toolString = requestedJob.getToolName();
                            Tool tool = getToolObject(toolString);
                            Object result = tool.go(requestedJob.getParameters());
                            writeToNet.writeObject(result);
                            System.out.println("[SatelliteThread.run] COMPLETED JOB REQUEST #: " + (int) result);
                        } catch (UnknownToolException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                            System.err.println(e);
                        }
                        break;

                    default:
                        System.err.println("[SatelliteThread.run] Warning: Message type not implemented");
                }
            } catch (IOException | ClassNotFoundException e){
                System.out.println(e);
            }
        }

        /**
     * Aux method to get a tool object, given the fully qualified class string
     * If the tool has been used before, it is returned immediately out of the cache,
     * otherwise it is loaded dynamically
     */
        public Tool getToolObject(String toolClassString) throws UnknownToolException, ClassNotFoundException, InstantiationException, IllegalAccessException {

        Tool toolObject = (Tool) toolsCache.get(toolClassString);

        if (toolObject == null) {
            System.out.println("\nTool's Class: " + toolClassString);
            if (toolClassString == null) {
                throw new UnknownToolException();
            }
            Class<?> toolClass = classLoader.loadClass(toolClassString);
            toolObject = (Tool) toolClass.newInstance();
            toolsCache.put(toolClassString, toolObject);
        } else {
            System.out.println("Tool Class: " + toolClassString + " already in Cache.");
        }


            return toolObject;
        }

    }

    public static void main(String[] args) {
        // start the satellite
        Satellite satellite = new Satellite(args[0], args[1], args[2]);
        satellite.run();
    }

}

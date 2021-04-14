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

    // ConnectivityInfo stores host and port number
    private ConnectivityInfo satelliteInfo = new ConnectivityInfo();
    private ConnectivityInfo serverInfo = new ConnectivityInfo();
    private HTTPClassLoader classLoader = null; // use to load class files from remote servers
    private Hashtable toolsCache = null; // use to store and quickly access Tool objects

    public Satellite(String satellitePropertiesFile, String classLoaderPropertiesFile, String serverPropertiesFile) {

        // Read this satellite's properties and populate satelliteInfo object,
        // which later on will be sent to the server
        try {
            PropertyHandler satelliteConfig = new PropertyHandler(satellitePropertiesFile);
            satelliteInfo.setName(satelliteConfig.getProperty("NAME"));
            satelliteInfo.setPort(Integer.parseInt(satelliteConfig.getProperty("PORT")));
            satelliteInfo.setHost("127.0.0.1");
        } catch (IOException e) {
            System.err.println(e);
            System.exit(1);
        }

        // Read properties of the application server (AS) and populate serverInfo object
        //
        // Other than satellites, the AS doesn't have a human-readable name, so leave it out
        try {
            PropertyHandler serverConfig = new PropertyHandler(serverPropertiesFile);
            serverInfo.setPort(Integer.parseInt(serverConfig.getProperty("PORT")));
            serverInfo.setHost(serverConfig.getProperty("HOST"));
        } catch (IOException e) {
            System.err.println(e);
            System.exit(1);
        }

        // Read properties of the code server and create class loader
        try {
            PropertyHandler classLoaderConfig = new PropertyHandler(classLoaderPropertiesFile);
            classLoader = new HTTPClassLoader(classLoaderConfig.getProperty("HOST"),
                    Integer.parseInt(classLoaderConfig.getProperty("PORT")));
        } catch (IOException e) {
            System.err.println(e);
            System.exit(1);
        }

        // Create tools cache
        toolsCache = new Hashtable();

    }

    @Override
    public void run() {

        // Register this satellite with the SatelliteManager on the server
        try {
            Socket server = new Socket(serverInfo.getHost(), serverInfo.getPort());
            // Create message with type REGISTER_SATELLITE and object to send satelliteInfo
            Message registerMsg = new Message(REGISTER_SATELLITE, satelliteInfo);
            ObjectOutputStream toNetwork = new ObjectOutputStream(server.getOutputStream());
            System.out.println("[Satellite.run] Register Satellite: " + satelliteInfo.getName());
            // Send message to application server
            toNetwork.writeObject(registerMsg);
            // Could wait for response here in case of message failure
        } catch (IOException e) {
            System.err.println(e);
        }

        // Create server socket
        try {
            ServerSocket socket = new ServerSocket(satelliteInfo.getPort());
            System.out.println("[Satellite.run] Socket Created on Port :" + satelliteInfo.getPort());

            // Start taking job requests in a server loop
            while (true) {
                new SatelliteThread(socket.accept(), this).run();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * Class [SatelliteThread] An instance of this class represent a thread to process
     * single job requests received in the satellite server loop.
     */
    private class SatelliteThread extends Thread {

        Socket jobRequest = null; // socket for communicating with client
        ObjectInputStream readFromNet = null;
        ObjectOutputStream writeToNet = null;
        Message message = null;

        SatelliteThread(Socket jobRequest, Satellite satellite) {
            this.jobRequest = jobRequest;
        }

        @Override
        public void run() {
            try {
                // Create object streams
                readFromNet = new ObjectInputStream(jobRequest.getInputStream());
                writeToNet = new ObjectOutputStream(jobRequest.getOutputStream());

                // Read message
                message = (Message) readFromNet.readObject();

                // This thread should only handle messages of type JOB_REQUEST
                switch (message.getType()) {
                    case JOB_REQUEST:
                        System.out.println("[SatelliteThread.run] Received New Job Request.");
                        try {
                            // Cast message content to a Job object
                            Job requestedJob = (Job) message.getContent();
                            // Get tool
                            String toolString = requestedJob.getToolName();
                            Tool tool = getToolObject(toolString);
                            // Use tool to process job request
                            Object result = tool.go(requestedJob.getParameters());
                            // Notify the client of job request's result
                            writeToNet.writeObject(result);
                            System.out.println("[SatelliteThread.run] COMPLETED JOB REQUEST #: " + (long) result);
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
         * Aux method to get a tool object, given the fully qualified class string.
         * If the tool has been used before, it is returned immediately out of the cache,
         * otherwise it is loaded dynamically.
         */
        public Tool getToolObject(String toolClassString) throws UnknownToolException, ClassNotFoundException, InstantiationException, IllegalAccessException {

            // Check cache for tool
            Tool toolObject = (Tool) toolsCache.get(toolClassString);

            // Otherwise, load dynamically
            if (toolObject == null) {
                System.out.println("\nTool's Class: " + toolClassString);
                if (toolClassString == null) {
                    throw new UnknownToolException();
                }
                // Use class loader to get appropriate tool's class
                Class<?> toolClass = classLoader.loadClass(toolClassString);
                toolObject = (Tool) toolClass.newInstance();
                // Store tool in cache for quick access in future
                toolsCache.put(toolClassString, toolObject);
            } else {
                System.out.println("Tool Class: " + toolClassString + " already in Cache.");
            }

            return toolObject;
        }

    }

    public static void main(String[] args) {
        // Start the satellite
        Satellite satellite = new Satellite(args[0], args[1], args[2]);
        satellite.run();
    }

}

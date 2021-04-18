package appserver.server;

import java.util.ArrayList;

/**
 *
 * @author Dr.-Ing. Wolf-Dieter Otte
 */
public class LoadManager {

    static ArrayList satellites = null;
    static int nextSatelliteIndex = -1; // The next satellite server to be assigned a job

    /**
     * Initializes LoadManager
     */
    public LoadManager() {
        satellites = new ArrayList<String>();
    }

    /**
     * Adds a satellite to the arraylist satellites if not already added
     * @param satelliteName - satellite to add
     */
    public void satelliteAdded(String satelliteName) {
        // add satellite 
        
        if(!satellites.contains(satelliteName))
        {
            satellites.add(satelliteName);
            
            if (nextSatelliteIndex == -1){
                nextSatelliteIndex = 0;
            }
            
            System.out.println("[LoadManager.satelliteAdded] " + satelliteName + " has been added");
        } else {
            System.out.println("[LoadManager.satelliteAdded] " + satelliteName + " has already been added");
        }
    }

    /**
     * Grabs the next satellite based on the nextSatelliteIndex
     * @return the next satellite
     */
    public String nextSatellite() throws Exception {
        
        int numberSatellites = satellites.size();
        String nextSatelliteName = null;
        
        synchronized (satellites) {
            // Return a satelite name according to a round robin methodology
            if (numberSatellites > 0)
            {
                // Return the next satellite based on the index
                nextSatelliteName = (String)satellites.get( nextSatelliteIndex );
                
                // Iterate the index so the next satellite will be used
                nextSatelliteIndex++;
                // If we have reached the end of the list of satellites, wrap around
                if( nextSatelliteIndex >= numberSatellites ) {
                    nextSatelliteIndex = 0;
                }
            }
            else {
                System.out.println("No Satellites Registered");
                throw new Exception();
            }
        }

        return nextSatelliteName;// ... name of satellite who is supposed to take job
     
    }
}

package appserver.server;

import java.util.ArrayList;

/**
 *
 * @author Dr.-Ing. Wolf-Dieter Otte
 */
public class LoadManager {

    static ArrayList satellites = null;
    static int lastSatelliteIndex = -1;

    public LoadManager() {
        satellites = new ArrayList<String>();
    }

    public void satelliteAdded(String satelliteName) {
        // add satellite
        // ...
        
        if(!satellites.contains(satelliteName))
        {
            satellites.add(satelliteName);
            
            if (lastSatelliteIndex == -1){
                lastSatelliteIndex = 0;
            }
            
            System.out.println("[LoadManager.satelliteAdded] " + satelliteName + " has been added");
        } else {
            System.out.println("[LoadManager.satelliteAdded] " + satelliteName + " has already been added");
        }
    }


    public String nextSatellite() throws Exception {
        
        int numberSatellites = satellites.size();
        String nextSatelliteName = null;
        
        synchronized (satellites) {
            // implement policy that returns the satellite name according to a round robin methodology
            // ...
            
            if (numberSatellites > 0)
            {
                // TODO implement round robin
            }
            else {
                System.out.println("No Satellites Registered");
                throw new Exception();
            }
        }

        return nextSatelliteName// ... name of satellite who is supposed to take job
        ;
    }
}

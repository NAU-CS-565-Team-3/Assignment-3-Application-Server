package appserver.server;

import appserver.comm.ConnectivityInfo;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * SatelliteManager manages the satellite servers
 * @author Dr.-Ing. Wolf-Dieter Otte
 */
public class SatelliteManager {

    // (the one) hash table that contains the connectivity information of all satellite servers
    static private Hashtable<String, ConnectivityInfo> satellites = null;

    public SatelliteManager() {
        satellites = new Hashtable();
    }

    /**
     * Registers Satellite Server by adding it into the hashtable satellites 
     * @param satelliteInfo - satellite information to add
     */
    public void registerSatellite(ConnectivityInfo satelliteInfo) {
        // ...
        String satelliteName = satelliteInfo.getName();
        
        if(satellites.get(satelliteName) == null)
        {
            satellites.put(satelliteName, satelliteInfo);
            System.out.println("[SatelliteManager.registerSatellite] " + satelliteName + " is registered");
        }
        else{
            System.out.println("[SatelliteManager.registerSatellite] " + satelliteName + " is already registered");
        }
    }
    /**
     * Returns the Satellite information 
     * @param satelliteName - name of satellite to get information for
     * @return
     */
    public ConnectivityInfo getSatelliteForName(String satelliteName) {
        // ..
        return satellites.get(satelliteName);
    }
}

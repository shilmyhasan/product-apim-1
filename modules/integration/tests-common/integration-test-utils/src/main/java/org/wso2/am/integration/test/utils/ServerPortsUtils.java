package org.wso2.am.integration.test.utils;

import java.io.IOException;
import java.net.Socket;

public class ServerPortsUtils {

    public static String LOCALHOST = "localhost";
    public static final int httpPortLowerRange = 8080;
    public static final int httpPortUpperRange = 8099;
    public static final int httpsPortLowerRange = 9950;
    public static final int httpsPortUpperRange = 9999;

    /**
     * Check whether give port is available
     *
     * @param port Port Number
     * @return status
     */
    private static boolean isPortFree(int port, String host) {

        Socket s = null;
        try {
            s = new Socket(host, port);
            // something is using the port and has responded.
            return false;
        } catch (IOException e) {
            //port available
            return true;
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to close connection ", e);
                }
            }
        }
    }

    public static int getAvailableHttpPort(String host) {
        return getAvailablePort(httpPortLowerRange, httpPortUpperRange, host);
    }

    public static int getAvailableHttpsPort(String host) {
        return getAvailablePort(httpsPortLowerRange, httpsPortUpperRange, host);
    }

    /**
     * Find a free port to start backend WebSocket server in given port range
     *
     * @param lowerPortLimit from port number
     * @param upperPortLimit to port number
     * @return Available Port Number
     */
    private static int getAvailablePort(int lowerPortLimit, int upperPortLimit, String host) {
        while (lowerPortLimit < upperPortLimit) {
            if (ServerPortsUtils.isPortFree(lowerPortLimit, host)) {
                return lowerPortLimit;
            }
            lowerPortLimit += 1;
        }
        return -1;
    }

}

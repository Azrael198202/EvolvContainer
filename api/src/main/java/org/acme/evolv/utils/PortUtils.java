package org.acme.evolv.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class PortUtils {
    /*
     * create a stable port number in range 9000-16999 based on name's hash
     */
    public static int pickPort(String name) {
        int base = 9000;
        int span = 8000;
        int h = Math.abs(name == null ? 0 : name.hashCode());
        return base + (h % span);
    }

    /*
     * check if a TCP port is available
     */
    public static boolean isTcpPortAvailable(int port) {
        try (ServerSocket s = new ServerSocket()) {
            s.setReuseAddress(false);
            s.bind(new InetSocketAddress("0.0.0.0", port), 1);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /*
     * find a free TCP port in range [start, end]
     */
    public static int findFreePort(int start, int end) {
        for (int p = start; p <= end; p++) {
            if (isTcpPortAvailable(p)) return p;
        }
        throw new RuntimeException("No free port in range: " + start + "-" + end);
    }
}

package org.pipservices3.rpc.services;

import jakarta.ws.rs.container.ContainerRequestContext;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Helper class that retrieves parameters from HTTP requests.
 */
public class HttpRequestDetector {
    /**
     * Detects the platform (using "user-agent") from which the given HTTP request was made.
     *
     * @param req an HTTP request to process.
     * @return the detected platform and version. Detectable platforms: "mobile", "iphone",
     * "ipad",  "macosx", "android",  "webos", "mac", "windows". Otherwise - "unknown" will
     * be returned.
     */
    public static String detectPlatform(ContainerRequestContext req) {
        var ua = req.getHeaderString("user-agent");
        String version;

        if (ua.contains("mobile"))
            return "mobile";
        if (ua.contains("like Mac OS X")) {
            var resMatch = match("CPU( iPhone)? OS ([0-9\\._]+) like Mac OS X", ua);

            version = resMatch.get(2).replace('_', '.');

            if (ua.contains("iPhone"))
                return "iphone " + version;

            if (ua.contains("iPad"))
                return "ipad " + version;

            return "macosx " + version;
        }

        if (ua.contains("Android")) {
            var resMatch = match("Android ([0-9\\.]+)[\\);]", ua);
            version = resMatch.get(1);
            return "android " + version;
        }

        if (ua.contains("webOS")) {
            var resMatch = match("webOS\\/([0-9\\.]+)[\\);]", ua);
            version = resMatch.get(1);
            return "webos " + version;
        }

        if (ua.contains("Intel Mac OS X") || ua.contains("PPC Mac OS X")) {
            var resMatch = match("(Intel|PPC) Mac OS X ?([0-9\\._]*)[\\)\\;]", ua);
            version = resMatch.get(2).replace("_", ".");
            return "mac " + version;
        }


        if (ua.contains("Windows NT")) {
            try {
                var resMatch = match("Windows NT ([0-9\\._]+)[\\);]", ua);
                version = resMatch.get(1);
                return "windows " + version;
            } catch (Exception ex) {
                return "unknown";
            }
        }


        return "unknown";
    }

    /**
     * Detects the browser (using "user-agent") from which the given HTTP request was made.
     *
     * @param req   an HTTP request to process.
     * @return the detected browser. Detectable browsers: "chrome", "msie", "firefox",
     *          "safari". Otherwise - "unknown" will be returned.
     */
    public static String detectBrowser(ContainerRequestContext req) {
        var ua = req.getHeaderString("user-agent");

        if (ua.contains("chrome"))
            return "chrome";
        if (ua.contains("msie"))
            return "msie";
        if (ua.contains("firefox"))
            return "firefox";
        if (ua.contains("safari"))
            return "safari";

        return ua.isEmpty() ? "unknown" : ua;
    }

    /**
     * Detects the IP address from which the given HTTP request was received.
     *
     * @param req   an HTTP request to process.
     * @returns the detected IP address (without a port). If no IP is detected -
     * <code>null</code> will be returned.
     */
    public static String detectAddress(ContainerRequestContext req) {
        String ip = null;

        if (!req.getHeaderString("x-forwarded-for").isEmpty())
            ip = req.getHeaderString("x-forwarded-for").split(",")[0];

//
//        if (ip == null && req.ip) {
//            ip = req.ip;
//        }
//
//        if (ip == null && req.connection) {
//            ip = req.connection.remoteAddress;
//            if (!ip && req.connection.socket) {
//                ip = req.connection.socket.remoteAddress;
//            }
//        }
//
//        if (ip == null && req.socket) {
//            ip = req.socket.remoteAddress;
//        }

        // Remove port
        if (ip != null) {
            ip = ip.toString();
            var index = ip.indexOf(':');
            if (index > 0) {
                ip = ip.substring(0, index);
            }
        }

        return ip;
    }

    private static List<String> match(String pattern, String string) {
        Pattern stringPattern = Pattern.compile(pattern);
        var matcher = stringPattern.matcher(string);
        var resMatch = new ArrayList<String>();
        while (matcher.find())
            resMatch.add(matcher.group());
        return resMatch;
    }

    /**
     * Detects the host name of the request's destination server.
     *
     * @param req   an HTTP request to process.
     * @return the destination server's host name.
     */
    public static String detectServerHost(ContainerRequestContext req) throws UnknownHostException {
        return "" + InetAddress.getLocalHost().getHostName();
    }

    /**
     * Detects the request's destination port number.
     *
     * @param req   an HTTP request to process.
     * @return the detected port number or <code>80</code> (if none are detected).
     */
    public static int detectServerPort(ContainerRequestContext req){
        return 0; // TODO: req haven't server address properties
    }

}

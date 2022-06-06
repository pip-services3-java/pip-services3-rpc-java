package org.pipservices3.rpc.services;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.pipservices3.commons.errors.ConfigException;
import org.pipservices3.commons.refer.Descriptor;
import org.pipservices3.commons.refer.IReferences;
import org.pipservices3.commons.refer.ReferenceException;
import org.pipservices3.components.info.ContextInfo;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AboutOperations extends RestOperations {
    private ContextInfo _contextInfo;

    @Override
    public void setReferences(IReferences references) throws ReferenceException, ConfigException {
        super.setReferences(references);

        this._contextInfo = references.getOneOptional(ContextInfo.class,
                new Descriptor("pip-services", "context-info", "*", "*", "*")
        );
    }

    public Function<ContainerRequestContext, Response> getAboutOperation() {
        return (req) -> {
            try {
                return this.about(req);
            } catch (SocketException | UnknownHostException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private List<String> getNetworkAddresses() throws SocketException {
        var interfaces = NetworkInterface.getNetworkInterfaces();
        List<String> ipAddresses = new ArrayList<>();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            // filters out 127.0.0.1 and inactive interfaces
            if (iface.isLoopback() || !iface.isUp())
                continue;

            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();

                if (addr instanceof Inet6Address)
                    continue;

                ipAddresses.add(addr.getHostAddress());
            }
        }
        return ipAddresses;
    }

    public Response about(ContainerRequestContext req) throws SocketException, UnknownHostException {
        var about = Map.of(
                "server", Map.of(
                        "name", this._contextInfo != null ? this._contextInfo.getName() : "unknown",
                        "description", this._contextInfo != null ? this._contextInfo.getDescription() : "null",
                        "properties", this._contextInfo != null ? this._contextInfo.getProperties() : "null",
                        "uptime", this._contextInfo != null ? this._contextInfo.getUptime() : "null",

                        "current_time", ZonedDateTime.now().toOffsetDateTime().toString(),
                        "protocol", req.getUriInfo().getBaseUri().getScheme(),
                        "host", HttpRequestDetector.detectServerHost(req),
                        "addresses", this.getNetworkAddresses(),
                        "port", HttpRequestDetector.detectServerPort(req),
                        "url", req.getUriInfo().getBaseUri().toString()
                ),
                "client", Map.of(
                        "address", HttpRequestDetector.detectAddress(req),
                        "client", HttpRequestDetector.detectBrowser(req),
                        "platform", HttpRequestDetector.detectPlatform(req),
                        "user", req.getProperty("user")
                )
        );

        about.get("server").put("start_time", this._contextInfo != null ? this._contextInfo.getStartTime().toOffsetDateTime().toString() : null);

        return HttpResponseSender.sendResult(about);
    }
}

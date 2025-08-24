package me.vibing.tcpshield;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TCPShield implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("TCPShield");
    public static final List<String> ALLOWED_IP_RANGES = new ArrayList<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing TCPShield mod...");
        
        // Add Tailscale IP ranges (private networks)
        addTailscaleRanges();
        
        List<String> ipRanges = fetchIpRanges("https://tcpshield.com/v4/");
        ipRanges.addAll(fetchIpRanges("https://tcpshield.com/v4-cf/"));

        ALLOWED_IP_RANGES.addAll(ipRanges);

        if (ALLOWED_IP_RANGES.isEmpty()) {
            LOGGER.warn("No IP ranges were fetched. TCPShield protection may not work properly.");
            LOGGER.warn("This could be due to network issues or the TCPShield servers being unavailable.");
            LOGGER.warn("The mod will continue to run but will not provide protection until IP ranges are loaded.");
        } else {
            LOGGER.info("Successfully loaded {} TCPShield IP ranges for protection.", ALLOWED_IP_RANGES.size());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Loaded IP ranges: {}", String.join(", ", ALLOWED_IP_RANGES));
            }
        }
    }

    private List<String> fetchIpRanges(String urlString) {
        List<String> ipRanges = new ArrayList<>();
        try {
            URL url = new URI(urlString).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000); // 10 seconds
            connection.setReadTimeout(10000); // 10 seconds
            connection.setRequestMethod("GET");
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                ipRanges = reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .filter(this::isValidCidr)
                    .collect(Collectors.toList());
                
                LOGGER.info("Fetched {} valid IP ranges from {}", ipRanges.size(), urlString);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to fetch IP ranges from {}: {}", urlString, e.getMessage());
        }
        return ipRanges;
    }
    
    private boolean isValidCidr(String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) return false;
            
            String ip = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);
            
            if (prefixLength < 0 || prefixLength > 32) return false;
            
            String[] ipParts = ip.split("\\.");
            if (ipParts.length != 4) return false;
            
            for (String part : ipParts) {
                int octet = Integer.parseInt(part);
                if (octet < 0 || octet > 255) return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Add Tailscale IP ranges to the allowed list.
     * Tailscale uses specific private IP ranges for their networks.
     */
    private void addTailscaleRanges() {
        List<String> tailscaleRanges = List.of(
            "100.64.0.0/10",      // Tailscale's main private network range
            "fd7a:115c:a1e0::/48" // Tailscale's IPv6 range
        );
        
        ALLOWED_IP_RANGES.addAll(tailscaleRanges);
        LOGGER.info("Added {} Tailscale IP ranges for private network access", tailscaleRanges.size());
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Tailscale ranges: {}", String.join(", ", tailscaleRanges));
        }
    }
    
    /**
     * Manually refresh the IP ranges from TCPShield servers.
     * This can be called by server administrators to update the protection list.
     */
    public static void refreshIpRanges() {
        TCPShield instance = new TCPShield();
        instance.onInitialize();
    }
}
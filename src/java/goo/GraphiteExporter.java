package goo;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Copied from Prometheus under Apache License 2.0
 * See: https://github.com/prometheus/client_java/blob/master/simpleclient_graphite_bridge/src/main/java/io/prometheus/client/bridge/Graphite.java
 *
 * Export metrics in the GraphiteExporter plaintext format.
 */
public class GraphiteExporter {
    private static final Pattern INVALID_GRAPHITE_CHARS = Pattern.compile("[^a-zA-Z0-9_-]");

    /**
     * Push samples from the given registry to GraphiteExporter.
     */
    public static void push(String host, int port, CollectorRegistry registry) throws IOException {
        Socket s = new Socket(host, port);
        BufferedWriter writer = new BufferedWriter(new PrintWriter(s.getOutputStream()));
        Matcher m = INVALID_GRAPHITE_CHARS.matcher("");
        long now = System.currentTimeMillis() / 1000;
        for (Collector.MetricFamilySamples metricFamilySamples : Collections.list(registry.metricFamilySamples())) {
            for (Collector.MetricFamilySamples.Sample sample : metricFamilySamples.samples) {
                m.reset(sample.name);
                writer.write(m.replaceAll("_"));
                for (int i = 0; i < sample.labelNames.size(); ++i) {
                    m.reset(sample.labelValues.get(i));
                    writer.write("." + sample.labelNames.get(i) + "." + m.replaceAll("_"));
                }
                writer.write(" " + sample.value + " " + now + "\n");
            }
        }
        writer.close();
        s.close();
    }
}
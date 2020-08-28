package dk.aau.netsec.hostage.protocols.AMQP;

import org.apache.qpid.server.SystemLauncher;
import org.apache.qpid.server.configuration.IllegalConfigurationException;
import org.apache.qpid.server.model.SystemConfig;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EmbeddedBroker {
    private static final String INITIAL_CONFIGURATION = "qpid_embedded_inmemory_configuration.json";
    final SystemLauncher systemLauncher = new SystemLauncher();


    public void start () throws Exception {
        systemLauncher.startup(createSystemConfig());
    }

    private Map<String, Object> createSystemConfig () {
        Map<String, Object> attributes = new HashMap<>();
        URL initialConfig = EmbeddedBroker.class.getClassLoader().getResource(INITIAL_CONFIGURATION);
        //System.out.println(initialConfig.toExternalForm());
        attributes.put("type", "Memory");
        attributes.put(SystemConfig.INITIAL_CONFIGURATION_LOCATION, initialConfig.toExternalForm());
        attributes.put(SystemConfig.INITIAL_SYSTEM_PROPERTIES_LOCATION, createSystemPropertyFile());
        attributes.put("startupLoggedToSystemOut", true);
        return attributes;
    }

    public void stopBroker(){
        systemLauncher.shutdown();
    }


    private static String createSystemPropertyFile() {
        File file;
        try {
            final Path path = Files.createTempFile("system", ".properties");
            file = path.toFile();
            Files.write(path, Collections.singletonList(SystemConfig.PROPERTY_QPID_WORK + "=" + "target"), StandardCharsets.UTF_8);
            file.deleteOnExit();
        } catch (IOException e) {
            throw new IllegalConfigurationException("System property file not created", e);
        }
        return file.getAbsolutePath();
    }

}

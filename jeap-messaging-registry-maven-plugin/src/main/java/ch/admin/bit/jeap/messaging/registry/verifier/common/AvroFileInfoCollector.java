package ch.admin.bit.jeap.messaging.registry.verifier.common;

import ch.admin.bit.jeap.messaging.avro.plugin.compiler.ImportClassLoader;
import ch.admin.bit.jeap.messaging.registry.verifier.AvroIdlTokenizer;
import lombok.RequiredArgsConstructor;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class AvroFileInfoCollector {

    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("^@namespace\\(\"([^\"]+)\"\\)", Pattern.MULTILINE);
    private static final Pattern TYPE_PATTERN = Pattern.compile("\\b(record|enum|fixed)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\b");

    private final Log log;

    Set<AvroFileInfo> collectAvroFileInfos(Set<String> files, ImportClassLoader importClassLoader) {
        Set<AvroFileInfo> avroFileInfos = new HashSet<>();
        for (String file : files) {
            URL resource = importClassLoader.findResource(file);
            if (resource == null) {
                log.error("Resource not found: " + file + " in classloader: " + importClassLoader);
                throw new IllegalStateException("Resource not found: " + file);
            }
            log.debug("Found resource: " + resource + " for file: " + file + " with path " + resource.getPath() + " in classloader: " + importClassLoader);
            AvroFileInfo avroFileInfo = new AvroFileInfo(file);
            try (InputStream inputStream = getInputStream(resource)) {
                Set<AvroTypeInfo> avroTypeInfos = collectAvroTypeInfos(file, inputStream);
                avroTypeInfos.forEach(avroFileInfo::addDefinedType);
            } catch (IOException e) {
                throw new IllegalStateException("Error parsing imported file: " + file, e);
            }
            avroFileInfos.add(avroFileInfo);
        }
        return avroFileInfos;
    }

    private static InputStream getInputStream(URL resource) {
        try {
            return resource.openStream();
        } catch (IOException e) {
            throw new IllegalStateException("Could not open stream for " + resource, e);
        }
    }

    private Set<AvroTypeInfo> collectAvroTypeInfos(String filename, InputStream fileInputStream) throws IOException {
        String content = AvroIdlTokenizer.stripComments(
                new String(fileInputStream.readAllBytes(), StandardCharsets.UTF_8));

        // Declarations may span several lines (e.g. a comment between 'enum' and its name), so both patterns
        // are matched against the whole content. Namespaces are tracked by position, as they apply to the
        // declarations that follow them.
        NavigableMap<Integer, String> namespaces = new TreeMap<>();
        Matcher namespaceMatcher = NAMESPACE_PATTERN.matcher(content);
        while (namespaceMatcher.find()) {
            namespaces.put(namespaceMatcher.start(), namespaceMatcher.group(1));
        }

        Set<AvroTypeInfo> types = new HashSet<>();
        Matcher typeMatcher = TYPE_PATTERN.matcher(content);
        while (typeMatcher.find()) {
            Map.Entry<Integer, String> namespace = namespaces.floorEntry(typeMatcher.start());
            AvroTypeInfo avroTypeInfo = new AvroTypeInfo(
                    namespace == null ? "" : namespace.getValue(), typeMatcher.group(2));
            log.debug("Found type " + avroTypeInfo + " in " + filename);
            types.add(avroTypeInfo);
        }
        return types;
    }
}
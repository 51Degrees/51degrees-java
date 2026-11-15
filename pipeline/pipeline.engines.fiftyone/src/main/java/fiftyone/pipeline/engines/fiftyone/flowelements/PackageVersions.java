/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2026 51 Degrees Mobile Experts Limited, Davidson House,
 * Forbury Square, Reading, Berkshire, United Kingdom RG1 3EU.
 *
 * This Original Work is licensed under the European Union Public Licence
 * (EUPL) v.1.2 and is subject to its terms as set out below.
 *
 * If a copy of the EUPL was not distributed with this file, You can obtain
 * one at https://opensource.org/licenses/EUPL-1.2.
 *
 * The 'Compatible Licences' set out in the Appendix to the EUPL (as may be
 * amended by the European Commission) shall be deemed incompatible for
 * the purposes of the Work and the provisions of the compatibility
 * clause in Article 5 of the EUPL shall not apply.
 *
 * If using the Work as, or as part of, a network application, by
 * including the attribution notice(s) required under Article 5 of the EUPL
 * in the end user terms of the application under an appropriate heading,
 * such notice(s) shall fulfill the requirements of that article.
 * ********************************************************************* */

package fiftyone.pipeline.engines.fiftyone.flowelements;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Finds the packages a Pipeline uses and the version of each, for the
 * Package elements of the usage sharing record. A package's jar holds its
 * Maven descriptor, being pom.properties, which gives the version, and
 * pom.xml, which names the packages it depends on. A class that was not
 * loaded from a jar, such as one compiled in the same project, gives no
 * package.
 */
final class PackageVersions {

    /**
     * The Maven group every 51Degrees package is published under.
     */
    static final String GROUP_ID = "com.51degrees";

    private PackageVersions() {
    }

    /**
     * The packages of the group that hold the classes, and every package of
     * the group they depend on, each named "group:artifact" and given its
     * version, in name order.
     * @param classes the classes in use
     * @param groupId the Maven group of the packages to find
     * @return the packages and their versions
     */
    static SortedMap<String, String> of(Collection<Class<?>> classes,
                                        String groupId) {
        SortedMap<String, String> packages = new TreeMap<>();
        for (Class<?> type : classes) {
            ClassLoader loader = type.getClassLoader();
            if (loader == null) {
                continue;
            }
            for (String artifact : artifactsHolding(type, groupId)) {
                add(artifact, groupId, loader, packages);
            }
        }
        return packages;
    }

    /**
     * The artifacts of the group whose descriptor is in the jar the class
     * was loaded from. A jar that bundles several packages gives each of
     * them.
     */
    static List<String> artifactsHolding(Class<?> type, String groupId) {
        String path = type.getName().replace('.', '/') + ".class";
        URL url = type.getClassLoader().getResource(path);
        if (url == null || "jar".equals(url.getProtocol()) == false) {
            return Collections.emptyList();
        }
        String folder = "META-INF/maven/" + groupId + "/";
        List<String> artifacts = new ArrayList<>();
        try {
            // A connection to the class's entry gives the whole jar.
            URLConnection connection = url.openConnection();
            if (connection instanceof JarURLConnection == false) {
                return artifacts;
            }
            // Without the cache the jar is opened for this read alone, so it
            // is closed here and never held open for the application.
            connection.setUseCaches(false);
            try (JarFile jar = ((JarURLConnection) connection).getJarFile()) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (name.startsWith(folder) &&
                        name.endsWith("/pom.properties")) {
                        String artifact = name.substring(folder.length(),
                            name.length() - "/pom.properties".length());
                        if (artifact.indexOf('/') < 0) {
                            artifacts.add(artifact);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // A jar that cannot be read gives no package.
        }
        return artifacts;
    }

    private static void add(String artifact,
                            String groupId,
                            ClassLoader loader,
                            SortedMap<String, String> packages) {
        String name = groupId + ":" + artifact;
        if (packages.containsKey(name)) {
            return;
        }
        String folder = "META-INF/maven/" + groupId + "/" + artifact + "/";
        String version = version(loader.getResource(folder + "pom.properties"));
        if (version == null) {
            return;
        }
        packages.put(name, version);
        for (String dependency : dependencies(
            loader.getResource(folder + "pom.xml"), groupId)) {
            add(dependency, groupId, loader, packages);
        }
    }

    private static String version(URL properties) {
        if (properties == null) {
            return null;
        }
        Properties values = new Properties();
        try (InputStream input = open(properties)) {
            values.load(input);
        } catch (Exception e) {
            return null;
        }
        String version = values.getProperty("version");
        return version == null || version.trim().isEmpty() ?
            null : version.trim();
    }

    /**
     * The artifacts of the group that the pom names as dependencies, other
     * than those only its tests use. A dependency in the pom's own group is
     * often named with ${project.groupId}.
     */
    static List<String> dependencies(URL pom, String groupId) {
        List<String> artifacts = new ArrayList<>();
        if (pom == null) {
            return artifacts;
        }
        try (InputStream input = open(pom)) {
            DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
            factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            factory.setXIncludeAware(false);
            Document document = factory.newDocumentBuilder().parse(input);
            for (Element list : children(document.getDocumentElement(),
                "dependencies")) {
                for (Element dependency : children(list, "dependency")) {
                    String group = text(dependency, "groupId");
                    String artifact = text(dependency, "artifactId");
                    if (artifact != null &&
                        "test".equals(text(dependency, "scope")) == false &&
                        (groupId.equals(group) ||
                            "${project.groupId}".equals(group) ||
                            "${groupId}".equals(group))) {
                        artifacts.add(artifact);
                    }
                }
            }
        } catch (Exception e) {
            // A pom that cannot be read names no dependency.
        }
        return artifacts;
    }

    /**
     * Reads a resource without the JDK's cache of jars, so that the jar is
     * closed with the stream.
     */
    private static InputStream open(URL resource) throws IOException {
        URLConnection connection = resource.openConnection();
        connection.setUseCaches(false);
        return connection.getInputStream();
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> found = new ArrayList<>();
        for (Node node = parent.getFirstChild();
             node != null;
             node = node.getNextSibling()) {
            if (node instanceof Element &&
                name.equals(node.getNodeName())) {
                found.add((Element) node);
            }
        }
        return found;
    }

    private static String text(Element parent, String name) {
        List<Element> found = children(parent, name);
        return found.isEmpty() ? null : found.get(0).getTextContent().trim();
    }
}

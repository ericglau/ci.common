/**
 * (C) Copyright IBM Corporation 2018.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.wasdev.wlp.common.plugins.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public abstract class InstallFeatureUtil {
    
    public abstract void debug(String msg);

    public abstract void debug(String msg, Throwable e);

    public abstract void debug(Throwable e);

    public abstract void warn(String msg);

    public abstract void info(String msg);

    public abstract boolean isDebugEnabled();

    public abstract File downloadArtifact(String groupId, String artifactId, String type, String version)
            throws PluginExecutionException;
    
    @SafeVarargs
    public static Set<String> combineToSet(Collection<String>... collections) {
        Set<String> result = new HashSet<String>();
        for (Collection<String> collection : collections) {
            result.addAll(collection);
        }
        return result;
    }
    
    public Set<String> getServerFeatures(File serverDirectory, boolean noFeaturesConfigurationElements) {
        // parse server.xml features only if there are no configured features in the pom
        Set<String> result = new HashSet<String>();
        if (noFeaturesConfigurationElements) {
            debug("No features configuration elements were specified. Using server.xml.");
            result.addAll(getConfigDropinFeatures(serverDirectory, "overrides"));
            result.addAll(getServerXmlFeatures(serverDirectory, "server.xml", null));
            result.addAll(getConfigDropinFeatures(serverDirectory, "defaults"));
        } else {
            debug("Features configuration elements were specified. Skipping server.xml.");
        }
        return result;
    }
    
    private Set<String> getConfigDropinFeatures(File serverDirectory, String folderName){
        Set<String> result = new HashSet<String>();
        String configDropin = serverDirectory.getAbsolutePath() + "/configDropins/";
        File configDropinFolder = new File(configDropin+folderName);
        String[] overrideFileList = configDropinFolder.list();
        if (overrideFileList == null) {
            debug(folderName + " configDropins folder does not exist.");
            return result;
        }
        List<String> configDropinXmls = new ArrayList<String>();
        for (String xml : overrideFileList){
            if (xml.endsWith(".xml")) {
                configDropinXmls.add(xml);
            }
        }
        if (configDropinXmls.isEmpty()) {
            debug(folderName + " configDropins folder is empty.");
            return result;
        }
        for (String filename : configDropinXmls) {
            result.addAll(getServerXmlFeatures(configDropinFolder, filename, null));
        }
        return result;
    }

    public Set<String> getServerXmlFeatures(File fileContext, String serverFile, List<String> parsedXmls) {
        Set<String> result = new HashSet<String>();
        File serverXml = new File(fileContext, serverFile);
        List<String> updatedParsedXmls = new ArrayList<String>();
        updatedParsedXmls.add(serverFile);
        if (parsedXmls != null){
            updatedParsedXmls.addAll(parsedXmls);
        }
        if (serverXml.exists()) {
            try {
                Document doc = new XmlDocument() {
                    public Document getDocument(File file) throws IOException, ParserConfigurationException, SAXException {
                        createDocument(file);
                        return doc;
                    }
                }.getDocument(serverXml);
                Element root = doc.getDocumentElement();
                NodeList nodes = root.getChildNodes();
                for (int i = 0; i < nodes.getLength(); i++) {
                    if (nodes.item(i) instanceof Element) {
                        Element child = (Element) nodes.item(i);
                        if ("featureManager".equals(child.getNodeName())) {
                            result.addAll(parseFeatureManagerNode(child));
                        }
                        if ("include".equals(child.getNodeName())){
                            result = parseIncludeNode(result, child, fileContext, updatedParsedXmls);
                        }
                    }
                }
            } catch (IOException | ParserConfigurationException | SAXException e) {
                warn("Failed to parse the xml file " + serverXml + ": " + e.getLocalizedMessage());
                debug(e);
            }
        }
        return result;
    }
    
    private Set<String> parseIncludeNode(Set<String> origResult, Element node, File fileContext,
            List<String> updatedParsedXmls) {
        Set<String> result = origResult;
        String includeFileName = node.getAttribute("location");
        if (!updatedParsedXmls.contains(includeFileName)){
            String onConflict = node.getAttribute("onConflict");
            boolean onConflictMerge = Pattern.compile(Pattern.quote("MERGE"), Pattern.CASE_INSENSITIVE).matcher(onConflict).find();
            boolean onConflictReplace = Pattern.compile(Pattern.quote("REPLACE"), Pattern.CASE_INSENSITIVE).matcher(onConflict).find();
            boolean onConflictIgnore = Pattern.compile(Pattern.quote("IGNORE"), Pattern.CASE_INSENSITIVE).matcher(onConflict).find();
            if (!(onConflictMerge || onConflictReplace || onConflictIgnore)){
                onConflictMerge = true;
            }
            if (!onConflictIgnore){
                if (onConflictMerge){
                    result.addAll(getServerXmlFeatures(fileContext, includeFileName, updatedParsedXmls));
                } else if (onConflictReplace){
                    result = getServerXmlFeatures(fileContext, includeFileName, updatedParsedXmls);
                }
            }
        }
        return result;
    }

    private List<String> parseFeatureManagerNode(Element node) {
        List<String> result = new ArrayList<String>();
        NodeList features = node.getElementsByTagName("feature");
        if (features != null) {
            for (int j = 0; j < features.getLength(); j++) {
                result.add(features.item(j).getTextContent());
            }
        }
        return result;
    }
    
    public Set<File> downloadProductJsons(File installDirectory) throws PluginExecutionException {
        // get productId and version for all properties
        File versionsDir = new File(installDirectory, "lib/versions");
        List<ProductProperties> propertiesList = loadProperties(versionsDir);

        if (propertiesList.isEmpty()) {
            throw new PluginExecutionException("Could not find any properties file in the " + versionsDir
                    + " directory. Ensure the directory " + installDirectory + " contains a Liberty installation.");
        }
        
        // download JSONs
        Set<File> downloadedJsons = new HashSet<File>();
        for (ProductProperties properties : propertiesList) {
            File json = downloadJsons(properties.getId(), properties.getVersion());
            if (json != null) {
                downloadedJsons.add(json);
            }
        }
        return downloadedJsons;
    }
    
    /**
     * Download the JSON file for the given product.
     * 
     * @param productId The product ID from the runtime's properties file
     * @param productVersion The product version from the runtime's properties file
     * @return The JSON file, or null if not found
     */
    private File downloadJsons(String productId, String productVersion) {
        String jsonGroupId = productId + ".features";        
        try {
            return downloadArtifact(jsonGroupId, "features", "json", productVersion);
        } catch (PluginExecutionException e) {
            debug("Cannot find json for productId " + productId + ", productVersion " + productVersion, e);
            return null;
        }
    }
    
    private List<ProductProperties> loadProperties(File dir) throws PluginExecutionException {
        List<ProductProperties> list = new ArrayList<ProductProperties>();

        File[] propertiesFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".properties");
            }
        });

        if (propertiesFiles != null) {
            for (File propertiesFile : propertiesFiles) {
                Properties properties = new Properties();
                InputStream input = null;
                try {
                    input = new FileInputStream(propertiesFile);
                    properties.load(input);
                    String productId = properties.getProperty("com.ibm.websphere.productId");
                    String productVersion = properties.getProperty("com.ibm.websphere.productVersion");
                    if (productId == null) {
                        throw new PluginExecutionException(
                                "Cannot find the \"com.ibm.websphere.productId\" property in the file "
                                        + propertiesFile.getAbsolutePath()
                                        + ". Ensure the file is valid properties file for the Liberty product or extension.");
                    }
                    if (productVersion == null) {
                        throw new PluginExecutionException(
                                "Cannot find the \"com.ibm.websphere.productVersion\" property in the file "
                                        + propertiesFile.getAbsolutePath()
                                        + ". Ensure the file is valid properties file for the Liberty product or extension.");
                    }
                    list.add(new ProductProperties(productId, productVersion));
                } catch (IOException e) {
                    throw new PluginExecutionException("Cannot read the product properties file " + propertiesFile.getAbsolutePath(), e);
                } finally {
                    if (input != null) {
                        try {
                            input.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        }

        return list;
    }
    
    public class ProductProperties {
        private String id;
        private String version;
        
        public ProductProperties(String id, String version) {
            this.id = id;
            this.version = version;
        }
        
        public String getId() {
            return id;
        }

        public String getVersion() {
            return version;
        }
    }
    
    /**
     * Returns true if this scenario is not supported for installing from Maven
     * repository, which is one of the following conditions: "from" parameter is
     * specified (don't need Maven repositories), "to" parameter is specified
     * and not usr/core (only usr/core are supported with Maven for now), or esa
     * files are specified in the configuration (not supported with Maven for
     * now)
     * 
     * @param from the "from" parameter specified in the plugin
     * @param to the "to" parameter specified in the plugin
     * @param hasEsaConfigurationElements whether there are ESA files specified in the plugin configuration
     * @return true if the fallback scenario occurred, false otherwise
     */
    public boolean hasUnsupportedParameters(String from, String to, boolean hasEsaConfigurationElements) {
        boolean hasFrom = from != null;
        boolean hasTo = !"usr".equals(to) && !"core".equals(to);
        debug("hasFrom: " + hasFrom);
        debug("hasTo: " + hasTo);
        debug("hasEsaConfigurationElements: " + hasEsaConfigurationElements);
        boolean result = hasFrom || hasTo || hasEsaConfigurationElements;
        if (result) {
            debug("Cannot install features from a Maven repository when using the 'to' or 'from' parameters or when specifying ESA files. Using installUtility instead.");
        }
        return result;
    }
    
    private File downloadEsaArtifact(String mavenCoordinates) throws PluginExecutionException {
        String[] mavenCoordinateArray = mavenCoordinates.split(":");
        String groupId = mavenCoordinateArray[0];
        String artifactId = mavenCoordinateArray[1];
        String version = mavenCoordinateArray[2];
        return downloadArtifact(groupId, artifactId, "esa", version);
    }
    
    private List<File> downloadEsas(Collection<?> mavenCoordsList) throws PluginExecutionException{
        List<File> repoPaths = new ArrayList<File>();
        for (Object coordinate : mavenCoordsList) {
            repoPaths.add(downloadEsaArtifact((String) coordinate));
        }
        return repoPaths;
    }
    
    /**
     * Resolve, download, and install features from a Maven repository. This
     * method calls the resolver with the given JSONs and feature list,
     * downloads the ESAs corresponding to the resolved features, then installs
     * those features.
     * 
     * @param jsonRepos
     *            JSON files, each containing an array of metadata for all
     *            features in a Liberty release.
     * @param featuresToInstall
     *            The list of features to install.
     * @throws PluginExecutionException
     *             if any of the features could not be installed
     */
    public void installFeatures(File installDirectory, boolean isAcceptLicense, List<File> jsonRepos, List<String> featuresToInstall) throws PluginExecutionException {
        debug("JSON repos: " + jsonRepos);
        info("Installing features: " + featuresToInstall);

        try {
            Map<String, Object> mapBasedInstallKernel = createMapBasedInstallKernelInstance(installDirectory);
            mapBasedInstallKernel.put("install.local.esa", true);
            mapBasedInstallKernel.put("single.json.file", jsonRepos);
            mapBasedInstallKernel.put("features.to.resolve", featuresToInstall);
            mapBasedInstallKernel.put("license.accept", isAcceptLicense);

            if (isDebugEnabled()) {
                mapBasedInstallKernel.put("debug", Level.FINEST);
            }

            Collection<?> resolvedFeatures = (Collection<?>) mapBasedInstallKernel.get("action.result");
            if (resolvedFeatures == null) {
                debug("action.exception.stacktrace: "+mapBasedInstallKernel.get("action.exception.stacktrace"));
                String exceptionMessage = (String) mapBasedInstallKernel.get("action.error.message");
                throw new PluginExecutionException(exceptionMessage);
            } else if (resolvedFeatures.isEmpty()) {
                debug("action.exception.stacktrace: "+mapBasedInstallKernel.get("action.exception.stacktrace"));
                String exceptionMessage = (String) mapBasedInstallKernel.get("action.error.message");
                if(exceptionMessage.contains("CWWKF1250I")){
                    warn(exceptionMessage);
                } else {
                    throw new PluginExecutionException(exceptionMessage);
                }
            }
            Collection<File> artifacts = downloadEsas(resolvedFeatures);

            for (File esaFile: artifacts ){
                mapBasedInstallKernel.put("license.accept", isAcceptLicense);
                mapBasedInstallKernel.put("action.install", esaFile);
                Integer ac = (Integer) mapBasedInstallKernel.get("action.result");
                debug("action.result: "+ac);
                debug("action.error.message: "+mapBasedInstallKernel.get("action.error.message"));
                if (mapBasedInstallKernel.get("action.error.message") != null) {
                    debug("action.exception.stacktrace: "+mapBasedInstallKernel.get("action.exception.stacktrace"));
                    String exceptionMessage = (String) mapBasedInstallKernel.get("action.error.message");
                    debug(exceptionMessage);
                }
            }
        } catch (PrivilegedActionException e) {
            throw new PluginExecutionException("Could not load the jar " + getMapBasedInstallKernelJar(installDirectory).getAbsolutePath(), e);
        }
    }
    
    private Map<String, Object> createMapBasedInstallKernelInstance(File installDirectory) throws PrivilegedActionException {
        final File installJarFile = getMapBasedInstallKernelJar(installDirectory);
        String installJarFileSubpath = installJarFile.getParentFile().getName() + File.separator + installJarFile.getName();
        Map<String, Object> mapBasedInstallKernel = AccessController.doPrivileged(new PrivilegedExceptionAction<Map<String, Object>>() {
            @SuppressWarnings({ "unchecked", "resource" })
            @Override
            public Map<String, Object> run() throws Exception {
                ClassLoader loader = new URLClassLoader(new URL[] { installJarFile.toURI().toURL() }, null);
                Class<Map<String, Object>> clazz;
                clazz = (Class<Map<String, Object>>) loader.loadClass("com.ibm.ws.install.map.InstallMap");
                return clazz.newInstance();
            }
        });
        if (mapBasedInstallKernel == null){
            debug("mbik is null");
        }

        // Init
        mapBasedInstallKernel.put("runtime.install.dir", installDirectory);
        mapBasedInstallKernel.put("install.map.jar", installJarFileSubpath);
        debug("install.map.jar: " + installJarFileSubpath);
        debug("install.kernel.init.code: " + mapBasedInstallKernel.get("install.kernel.init.code"));
        debug("install.kernel.init.error.message: " + mapBasedInstallKernel.get("install.kernel.init.error.message"));
        File usrDir = new File(installDirectory, "usr/tmp");
        mapBasedInstallKernel.put("target.user.directory", usrDir);
        return mapBasedInstallKernel;
    }
    
    /**
     * Find latest install map jar from lib directory
     * 
     * @return the install map jar file
     */
    public File getMapBasedInstallKernelJar(File installDirectory) {
        final String installMapPrefix = "com.ibm.ws.install.map";
        final String installMapSuffix = ".jar";

        File dir = new File(installDirectory, "lib");

        File[] installMapJars = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(installMapPrefix) && name.endsWith(installMapSuffix);
            }
        });

        File latestJar = null;
        if (installMapJars != null && installMapJars.length > 0) {
            for (File jar : installMapJars) {
                debug("latestJar: " + latestJar);
                debug("jar: " + jar);

                if (latestJar == null) {
                    // first jar found
                    latestJar = jar;
                    continue;
                }
                String latestJarVersion = extractVersion(latestJar.getName(), installMapPrefix, installMapSuffix);
                if (latestJarVersion == null) {
                    // jar without version is the oldest jar 
                    latestJar = jar;
                    continue;
                }
                String jarVersion = extractVersion(jar.getName(), installMapPrefix, installMapSuffix);
                if (jarVersion != null && jarVersion.compareTo(latestJarVersion) > 0) {
                    // jar has a later version
                    latestJar = jar;
                    continue;
                }
            }
        }

        debug("Using install map from jar: " + (latestJar == null ? null : latestJar.getAbsolutePath()));
        return latestJar;
    }
    
    private String extractVersion(String fileName, String prefix, String suffix) {
        int startIndex = prefix.length()+1;
        int endIndex = fileName.lastIndexOf(suffix);
        if (startIndex < endIndex) {
            String versionString = fileName.substring(startIndex, endIndex);
            debug("Extracted version string: " + versionString);
            return versionString;
        } else {
            return null;
        }
    }
}

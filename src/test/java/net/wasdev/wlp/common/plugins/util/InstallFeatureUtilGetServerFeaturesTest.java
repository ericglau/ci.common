package net.wasdev.wlp.common.plugins.util;
import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class InstallFeatureUtilGetServerFeaturesTest {
    private	static File wlpFolder = null;
    private static File installDirectory = null;
    private static File serverDir = null;

    /**
     * Unzips the given zip (or jar) file to the target directory.
     *
     * @param zipOrJarFile The zip or jar file to unzip
     * @param targetDir The target directory
     * @param extension If specified, only files within the zip that match this extension will be unzipped
     * @param flat Honestly, not really sure what this does!
     * @throws IOException
     * @throws ZipException
     */
    public static void unzipFile(File zipOrJarFile, File targetDir, String extension, boolean flat) throws ZipException, IOException {
        ZipFile zipFile = null;
        try {
            targetDir.mkdirs();
            zipFile = new ZipFile(zipOrJarFile);
            Enumeration<? extends ZipEntry> e = zipFile.entries();
            while (e.hasMoreElements()) {
                ZipEntry entry = e.nextElement();
                File targetPath = new File(targetDir, entry.getName());
                if (flat) {
                    targetPath = new File(targetDir, targetPath.getName());
                }
                targetPath.getParentFile().mkdirs();
                if (entry.isDirectory()) {
                    targetPath.mkdir();
                } else {
                    if (extension == null || entry.getName().toLowerCase().endsWith(extension)) {
                        extractFile(zipFile.getInputStream(entry), targetPath);
                        targetPath.setExecutable(true);
                    }
                }
            }
        } finally {
            zipFile.close();
        }
    }
    
    /**
     * Extracts the wlp file to the temporary folder
     *
     * @param inputStream
     * @param targetPath
     * @throws IOException
     */
    private static void extractFile(InputStream inputStream, File targetPath) throws IOException {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(inputStream);
            int b;
            byte buffer[] = new byte[8192];
            FileOutputStream fos = new FileOutputStream(targetPath);
            bos = new BufferedOutputStream(fos, 8192);

            while ((b = bis.read(buffer, 0, 8192)) != -1) {
                bos.write(buffer, 0, b);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bos.close();
            bis.close();
        }
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * Initializes variables and unzips wlp folder
     * 
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception{
        serverDir = new File("src/test/resources/servers");

        wlpFolder = tempFolder.newFolder();
        Path originalWlp = Paths.get("src/test/resources/wlp.zip");
        Path wlpReplace = Paths.get(wlpFolder.getAbsolutePath());
        Files.copy(originalWlp, wlpReplace, StandardCopyOption.REPLACE_EXISTING);
        File wlpZip = new File(wlpReplace.toString());

        installDirectory = tempFolder.newFolder("wlp");
        unzipFile(wlpZip, installDirectory, null, false);
    }

    /**
     * Tests base server.xml without any include locations or config dropins
     * 
     * @throws Exception
     */
    @Test
    public void testServerBaseXML() throws Exception{
        Path serverBase = Paths.get(serverDir.getAbsolutePath() + "/server.xml");
        Path serverReplace = Paths.get(installDirectory + "/wlp/usr/servers/test/server.xml");
        Files.copy(serverBase, serverReplace, StandardCopyOption.REPLACE_EXISTING);

        Set<String> expected = new HashSet<String>();
        expected.add("jsp-2.3");

        Set<String> getServerResult = new HashSet<String>();
        File serverDirectory = new File(installDirectory + "/wlp/usr/servers/test");
        getServerResult = InstallFeatureUtil.getServerFeatures(serverDirectory);
        assertEquals("The features returned from getServerFeatures do not equal the expectedFeatures.", expected, getServerResult);
    }

    /**
     * Tests server.xml with IGNORE function
     * 
     * @throws Exception
     */
    @Test
    public void testIgnoreServerXML() throws Exception{
        Path serverIgnore = Paths.get(serverDir.getAbsolutePath() + "/server_ignore.xml");
        Path serverReplace = Paths.get(installDirectory + "/wlp/usr/servers/test/server.xml");
        Files.copy(serverIgnore, serverReplace, StandardCopyOption.REPLACE_EXISTING);

        Path extraFeatures = Paths.get(serverDir.getAbsolutePath() + "/extraFeatures.xml");
        Path extraFeatReplace = Paths.get(installDirectory + "/wlp/usr/servers/test/extraFeatures.xml");
        Files.copy(extraFeatures, extraFeatReplace, StandardCopyOption.REPLACE_EXISTING);

        Set<String> expected = new HashSet<String>();
        expected.add("jsp-2.3");

        Set<String> getServerResult = new HashSet<String>();
        File serverDirectory = new File(installDirectory + "/wlp/usr/servers/test");
        getServerResult = InstallFeatureUtil.getServerFeatures(serverDirectory);
        assertEquals("The features returned from getServerFeatures do not equal the expectedFeatures.", expected, getServerResult);
    }

    /**
     * Tests server.xml with MERGE function
     * 
     * @throws Exception
     */
    @Test
    public void testMergeServerXML() throws Exception{
        Path serverMerge = Paths.get(serverDir.getAbsolutePath() + "/server_merge.xml");
        Path serverReplace = Paths.get(installDirectory + "/wlp/usr/servers/test/server.xml");
        Files.copy(serverMerge, serverReplace, StandardCopyOption.REPLACE_EXISTING);

        Path extraFeatures = Paths.get(serverDir.getAbsolutePath() + "/extraFeatures.xml");
        Path extraFeatReplace = Paths.get(installDirectory + "/wlp/usr/servers/test/extraFeatures.xml");
        Files.copy(extraFeatures, extraFeatReplace, StandardCopyOption.REPLACE_EXISTING);

        Set<String> expected = new HashSet<String>();
        expected.add("jsp-2.3");
        expected.add("adminCenter-1.0");

        Set<String> getServerResult = new HashSet<String>();
        File serverDirectory = new File(installDirectory + "/wlp/usr/servers/test");
        getServerResult = InstallFeatureUtil.getServerFeatures(serverDirectory);
        assertEquals("The features returned from getServerFeatures do not equal the expectedFeatures.", expected, getServerResult);
    }
    
    /**
     * Tests server.xml with REPLACE function
     * 
     * @throws Exception
     */
    @Test
    public void testReplaceServerXML() throws Exception{
        Path serverMerge = Paths.get(serverDir.getAbsolutePath() + "/server_replace.xml");
        Path serverReplace = Paths.get(installDirectory + "/wlp/usr/servers/test/server.xml");
        Files.copy(serverMerge, serverReplace, StandardCopyOption.REPLACE_EXISTING);

        Path extraFeatures = Paths.get(serverDir.getAbsolutePath() + "/extraFeatures.xml");
        Path extraFeatReplace = Paths.get(installDirectory + "/wlp/usr/servers/test/extraFeatures.xml");
        Files.copy(extraFeatures, extraFeatReplace, StandardCopyOption.REPLACE_EXISTING);

        Set<String> expected = new HashSet<String>();
        expected.add("adminCenter-1.0");

        Set<String> getServerResult = new HashSet<String>();
        File serverDirectory = new File(installDirectory + "/wlp/usr/servers/test");
        getServerResult = InstallFeatureUtil.getServerFeatures(serverDirectory);
        assertEquals("The features returned from getServerFeatures do not equal the expectedFeatures.", expected, getServerResult);
    }

    /**
     * Tests server.xml with multiple REPLACE functions
     * 
     * @throws Exception
     */
    @Test
    public void testMultipleReplaceXML() throws Exception{
    	Path serverMulReplace = Paths.get(serverDir.getAbsolutePath() + "/multipleReplace.xml");
    	Path serverReplace = Paths.get(installDirectory + "/wlp/usr/servers/test/server.xml");
    	Files.copy(serverMulReplace, serverReplace, StandardCopyOption.REPLACE_EXISTING);

    	Path mulReplace2 = Paths.get(serverDir.getAbsolutePath() + "/multipleReplace2.xml");
    	Path newMulReplace2 = Paths.get(installDirectory + "/wlp/usr/servers/test/multipleReplace2.xml");
    	Files.copy(mulReplace2, newMulReplace2, StandardCopyOption.REPLACE_EXISTING);

    	Path mulReplace3 = Paths.get(serverDir.getAbsolutePath() + "/multipleReplace3.xml");
    	Path newMulReplace3 = Paths.get(installDirectory + "/wlp/usr/servers/test/multipleReplace3.xml");
    	Files.copy(mulReplace3, newMulReplace3, StandardCopyOption.REPLACE_EXISTING);

    	Set<String> expected = new HashSet<String>();
    	expected.add("adminCenter-1.0");

    	Set<String> getServerResult = new HashSet<String>();
    	File serverDirectory = new File(installDirectory + "/wlp/usr/servers/test");
    	getServerResult = InstallFeatureUtil.getServerFeatures(serverDirectory);
        assertEquals("The features returned from getServerFeatures do not equal the expectedFeatures.", expected, getServerResult);
    }

    /**
     * Tests server.xml with config dropins overrides 
     * 
     * @throws Exception
     */
    @Test
    public void testConfigOverride() throws Exception{
        Path serverBase = Paths.get(serverDir.getAbsolutePath() + "/server.xml");
        Path serverReplace = Paths.get(installDirectory + "/wlp/usr/servers/test/server.xml");
        Files.copy(serverBase, serverReplace, StandardCopyOption.REPLACE_EXISTING);

        File configDropins = new File(installDirectory + "/wlp/usr/servers/test/configDropins");
        configDropins.mkdir();

        File overrides = new File (configDropins.getAbsolutePath() + "/overrides");
        overrides.mkdir();

        Path configOverride = Paths.get(serverDir.getAbsolutePath() + "/config_override.xml");
        Path configReplace = Paths.get(overrides.getAbsolutePath() + "/config_override.xml");
        Files.copy(configOverride, configReplace, StandardCopyOption.REPLACE_EXISTING);

        Set<String> expected = new HashSet<String>();
        expected.add("jsp-2.3");
        expected.add("adminCenter-1.0");

        Set<String> getServerResult = new HashSet<String>();
        File serverDirectory = new File(installDirectory + "/wlp/usr/servers/test");
        getServerResult = InstallFeatureUtil.getServerFeatures(serverDirectory);
        assertEquals("The features returned from getServerFeatures do not equal the expectedFeatures.", expected, getServerResult);
    }

    /**
     * Tests server.xml with config dropins defaults
     * 
     * @throws Exception
     */
    @Test
    public void testConfigDefault() throws Exception{
        Path serverBase = Paths.get(serverDir.getAbsolutePath() + "/server.xml");
        Path serverReplace = Paths.get(installDirectory + "/wlp/usr/servers/test/server.xml");
        Files.copy(serverBase, serverReplace, StandardCopyOption.REPLACE_EXISTING);

        File configDropins = new File(installDirectory + "/wlp/usr/servers/test/configDropins");
        configDropins.mkdir();

        File defaults = new File (configDropins.getAbsolutePath() + "/defaults");
        defaults.mkdir();

        Path configDefault = Paths.get(serverDir.getAbsolutePath() + "/config_default.xml");
        Path defaultReplace = Paths.get(defaults.getAbsolutePath() + "/config_default.xml");
        Files.copy(configDefault, defaultReplace, StandardCopyOption.REPLACE_EXISTING);

        Set<String> expected = new HashSet<String>();
        expected.add("jsp-2.3");
        expected.add("batch-1.0");

        Set<String> getServerResult = new HashSet<String>();
        File serverDirectory = new File(installDirectory + "/wlp/usr/servers/test");
        getServerResult = InstallFeatureUtil.getServerFeatures(serverDirectory);
        assertEquals("The features returned from getServerFeatures do not equal the expectedFeatures.", expected, getServerResult);
    }

    /**
     * Tests server.xml with both config dropins overrides and defaults
     * 
     * @throws Exception
     */
    @Test
    public void testOverrideAndDefault() throws Exception{
        Path serverBase = Paths.get(serverDir.getAbsolutePath() + "/server.xml");
        Path serverReplace = Paths.get(installDirectory + "/wlp/usr/servers/test/server.xml");
        Files.copy(serverBase, serverReplace, StandardCopyOption.REPLACE_EXISTING);

        File configDropins = new File(installDirectory + "/wlp/usr/servers/test/configDropins");
        configDropins.mkdir();

        File defaults = new File (configDropins.getAbsolutePath() + "/defaults");
        defaults.mkdir();

        Path configDefault = Paths.get(serverDir.getAbsolutePath() + "/config_default.xml");
        Path defaultReplace = Paths.get(defaults.getAbsolutePath() + "/config_default.xml");
        Files.copy(configDefault, defaultReplace, StandardCopyOption.REPLACE_EXISTING);

        File overrides = new File (configDropins.getAbsolutePath() + "/overrides");
        overrides.mkdir();

        Path configOverride = Paths.get(serverDir.getAbsolutePath() + "/config_override.xml");
        Path overrideReplace = Paths.get(overrides.getAbsolutePath() + "/config_override.xml");
        Files.copy(configOverride, overrideReplace, StandardCopyOption.REPLACE_EXISTING);

        Set<String> expected = new HashSet<String>();
        expected.add("jsp-2.3");
        expected.add("batch-1.0");
        expected.add("adminCenter-1.0");

        Set<String> getServerResult = new HashSet<String>();
        File serverDirectory = new File(installDirectory + "/wlp/usr/servers/test");
        getServerResult = InstallFeatureUtil.getServerFeatures(serverDirectory);
        assertEquals("The features returned from getServerFeatures do not equal the expectedFeatures.", expected, getServerResult);
    }

}

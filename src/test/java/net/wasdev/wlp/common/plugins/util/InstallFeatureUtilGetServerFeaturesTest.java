package net.wasdev.wlp.common.plugins.util;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class InstallFeatureUtilGetServerFeaturesTest {
    private static File serverDirectory = null;
    private static File src = null;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpClass() throws Exception {
        src = new File("src/test/resources/servers");
    }
    
    @Before
    public void setUp() throws Exception {
        serverDirectory = tempFolder.newFolder();
    }

    private void copyAsName(String origName, String newName) throws IOException {
        File file = new File(src, origName);
        FileUtils.copyFile(file, new File(serverDirectory, newName));
    }
    
    private void copy(String origName) throws IOException {
        File file = new File(src, origName);
        FileUtils.copyFileToDirectory(file, serverDirectory);
    }
    
    private void verifyServerFeatures(Set<String> expected) {
        Set<String> getServerResult = InstallFeatureUtil.getServerFeatures(serverDirectory);
        assertEquals("The features returned from getServerFeatures do not equal the expected features.", expected, getServerResult);
    }
    
    /**
     * Tests base server.xml without any include locations or config dropins
     * 
     * @throws Exception
     */
    @Test
    public void testServerBaseXML() throws Exception{
        copy("server.xml");

        Set<String> expected = new HashSet<String>();
        expected.add("jsp-2.3");

        verifyServerFeatures(expected);
    }

    /**
     * Tests server.xml with IGNORE function
     * 
     * @throws Exception
     */
    @Test
    public void testIgnoreServerXML() throws Exception{
        copyAsName("server_ignore.xml", "server.xml");
        copy("extraFeatures.xml");

        Set<String> expected = new HashSet<String>();
        expected.add("jsp-2.3");

        verifyServerFeatures(expected);
    }



    /**
     * Tests server.xml with MERGE function
     * 
     * @throws Exception
     */
    @Test
    public void testMergeServerXML() throws Exception{
        copyAsName("server_merge.xml", "server.xml");
        copy("extraFeatures.xml");

        Set<String> expected = new HashSet<String>();
        expected.add("jsp-2.3");
        expected.add("adminCenter-1.0");

        verifyServerFeatures(expected);
    }
    
    /**
     * Tests server.xml with REPLACE function
     * 
     * @throws Exception
     */
    @Test
    public void testReplaceServerXML() throws Exception{
        copyAsName("server_replace.xml", "server.xml");
        copy("extraFeatures.xml");

        Set<String> expected = new HashSet<String>();
        expected.add("adminCenter-1.0");

        verifyServerFeatures(expected);
    }

    /**
     * Tests server.xml with multiple REPLACE functions
     * 
     * @throws Exception
     */
    @Test
    public void testMultipleReplaceXML() throws Exception{
        copyAsName("multipleReplace.xml", "server.xml");
        copy("multipleReplace2.xml");
        copy("multipleReplace3.xml");

    	Set<String> expected = new HashSet<String>();
    	expected.add("adminCenter-1.0");

        verifyServerFeatures(expected);
    }

    /**
     * Tests server.xml with config dropins overrides 
     * 
     * @throws Exception
     */
    @Test
    public void testConfigOverride() throws Exception{
        copy("server.xml");
        copyAsName("config_override.xml", "configDropins/overrides/config_override.xml");

        Set<String> expected = new HashSet<String>();
        expected.add("jsp-2.3");
        expected.add("adminCenter-1.0");

        verifyServerFeatures(expected);
    }

    /**
     * Tests server.xml with config dropins defaults
     * 
     * @throws Exception
     */
    @Test
    public void testConfigDefault() throws Exception{
        copy("server.xml");
        copyAsName("config_default.xml", "configDropins/defaults/config_default.xml");

        Set<String> expected = new HashSet<String>();
        expected.add("jsp-2.3");
        expected.add("batch-1.0");

        verifyServerFeatures(expected);
    }

    /**
     * Tests server.xml with both config dropins overrides and defaults
     * 
     * @throws Exception
     */
    @Test
    public void testOverrideAndDefault() throws Exception{
        copy("server.xml");
        copyAsName("config_override.xml", "configDropins/overrides/config_override.xml");
        copyAsName("config_default.xml", "configDropins/defaults/config_default.xml");

        Set<String> expected = new HashSet<String>();
        expected.add("jsp-2.3");
        expected.add("batch-1.0");
        expected.add("adminCenter-1.0");

        verifyServerFeatures(expected);
    }

}

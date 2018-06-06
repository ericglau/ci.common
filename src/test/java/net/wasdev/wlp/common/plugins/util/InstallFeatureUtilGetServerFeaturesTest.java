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
        expected.add("orig");

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
        expected.add("orig");

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
        expected.add("orig");
        expected.add("extra");

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
        expected.add("extra");

        verifyServerFeatures(expected);
    }

    /**
     * Tests server.xml with nested REPLACE functions
     * 
     * @throws Exception
     */
    @Test
    public void testNestedReplaceXML() throws Exception{
        copyAsName("server_nested_replace.xml", "server.xml");
        copy("nestedReplace2.xml");
        copy("nestedReplace3.xml");

    	Set<String> expected = new HashSet<String>();
    	expected.add("nested3");

        verifyServerFeatures(expected);
    }

    /**
     * Tests server.xml with config dropins overrides 
     * 
     * @throws Exception
     */
    @Test
    public void testConfigOverrides() throws Exception{
        copy("server.xml");
        copyAsName("config_override.xml", "configDropins/overrides/config_override.xml");

        Set<String> expected = new HashSet<String>();
        expected.add("orig");
        expected.add("override");

        verifyServerFeatures(expected);
    }

    /**
     * Tests server.xml with config dropins defaults
     * 
     * @throws Exception
     */
    @Test
    public void testConfigDefaults() throws Exception{
        copy("server.xml");
        copyAsName("config_default.xml", "configDropins/defaults/config_default.xml");

        Set<String> expected = new HashSet<String>();
        expected.add("orig");
        expected.add("default");

        verifyServerFeatures(expected);
    }

    /**
     * Tests server.xml with both config dropins overrides and defaults
     * 
     * @throws Exception
     */
    @Test
    public void testOverridesAndDefaults() throws Exception{
        copy("server.xml");
        copyAsName("config_override.xml", "configDropins/overrides/config_override.xml");
        copyAsName("config_default.xml", "configDropins/defaults/config_default.xml");

        Set<String> expected = new HashSet<String>();
        expected.add("orig");
        expected.add("override");
        expected.add("default");

        verifyServerFeatures(expected);
    }
    
    /**
     * Tests server.xml with multiple MERGE functions
     * 
     * @throws Exception
     */
    @Test
    public void testMultipleMerge() throws Exception{
        copyAsName("server_multiple_merge.xml", "server.xml");
        copy("extraFeatures2.xml");
        copy("extraFeatures3.xml");

        Set<String> expected = new HashSet<String>();
        expected.add("orig");
        expected.add("extra2");
        expected.add("extra3");

        verifyServerFeatures(expected);
    }
    
    /**
     * Tests server.xml with multiple REPLACE functions
     * 
     * @throws Exception
     */
    @Test
    public void testMultipleReplace() throws Exception{
        copyAsName("server_multiple_replace.xml", "server.xml");
        copy("extraFeatures2.xml");
        copy("extraFeatures3.xml");

        // only the last replace should be kept
        Set<String> expected = new HashSet<String>();
        expected.add("extra3");

        verifyServerFeatures(expected);
    }
    
    /**
     * Tests server.xml with REPLACE function that has no featureManager section
     * 
     * @throws Exception
     */
    @Test
    public void testNoListReplace() throws Exception{
        copyAsName("server_no_list_replace.xml", "server.xml");
        copy("noList.xml");

        // parent is kept, since child list does not exist
        Set<String> expected = new HashSet<String>();
        expected.add("orig");

        verifyServerFeatures(expected);
    }
    
    /**
     * Tests server.xml with REPLACE function that has empty featureManager section
     * 
     * @throws Exception
     */
    @Test
    public void testEmptyListReplace() throws Exception{
        copyAsName("server_empty_list_replace.xml", "server.xml");
        copy("emptyList.xml");

        // parent is kept, since child list is empty
        Set<String> expected = new HashSet<String>();
        expected.add("orig");

        verifyServerFeatures(expected);
    }

    /**
     * Tests server.xml with IGNORE function that has no featureManager section
     * 
     * @throws Exception
     */
    @Test
    public void testParentNoListIgnore() throws Exception{
        copyAsName("server_parent_no_list_ignore.xml", "server.xml");
        copy("extraFeatures.xml");

        // do not ignore the child list since parent has no featureManager section
        Set<String> expected = new HashSet<String>();
        expected.add("extra");

        verifyServerFeatures(expected);
    }
    
    /**
     * Tests server.xml with IGNORE function that has empty featureManager section
     * 
     * @throws Exception
     */
    @Test
    public void testParentEmptyListIgnore() throws Exception{
        copyAsName("server_parent_empty_list_ignore.xml", "server.xml");
        copy("extraFeatures.xml");

        // ignore the child list since parent has a featureManager section which counts as a conflict
        Set<String> expected = new HashSet<String>();

        verifyServerFeatures(expected);
    }
    
    /**
     * Tests server.xml with a combination of everything, including a replace that is empty
     * 
     * @throws Exception
     */
    @Test
    public void testCombinationEmptyReplace() throws Exception{
        copyAsName("server_combination_empty_replace.xml", "server.xml");
        copyAsName("config_override.xml", "configDropins/overrides/config_override.xml");
        copyAsName("config_default.xml", "configDropins/defaults/config_default.xml");
        copy("extraFeatures.xml");

        // Keep everything except the ignore (obviously).
        // Do not replace existing contents since replace file is empty.
        Set<String> expected = new HashSet<String>();
        expected.add("orig");
        expected.add("override");
        expected.add("default");
        expected.add("extra");
        expected.add("extra3");
        
        verifyServerFeatures(expected);
    }
    
    /**
     * Tests server.xml with a combination of everything, including a replace that is empty
     * 
     * @throws Exception
     */
    @Test
    public void testCombinationWithReplace() throws Exception{
        copyAsName("server_combination_replace.xml", "server.xml");
        copyAsName("config_override.xml", "configDropins/overrides/config_override.xml");
        copyAsName("config_default.xml", "configDropins/defaults/config_default.xml");
        copy("extraFeatures.xml");

        // Only keep the configDropins and the replace
        Set<String> expected = new HashSet<String>();
        expected.add("override");
        expected.add("default");
        expected.add("extra3");
        
        verifyServerFeatures(expected);
    }
}

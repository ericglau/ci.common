/**
 * (C) Copyright IBM Corporation 2021.
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

package io.openliberty.tools.common.plugins.util;

import java.io.File;

/**
 * Compile paths specific to a Maven module or Gradle project.
 */
public class DevModule {

    private File sourceDirectory;
    private File sourceOutputDirectory;
    private File testDirectory;
    private File testOutputDirectory;

    public DevModule(File sourceDirectory, File sourceOutputDirectory, File testDirectory,
            File testOutputDirectory) {
        this.sourceDirectory = sourceDirectory;
        this.sourceOutputDirectory = sourceOutputDirectory;
        this.testDirectory = testDirectory;
        this.testOutputDirectory = testOutputDirectory;
    }

    public File getSourceDirectory() {
        return sourceDirectory;
    }

    public void setSourceDirectory(File sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    public File getSourceOutputDirectory() {
        return sourceOutputDirectory;
    }

    public void setSourceOutputDirectory(File sourceOutputDirectory) {
        this.sourceOutputDirectory = sourceOutputDirectory;
    }

    public File getTestDirectory() {
        return testDirectory;
    }

    public void setTestDirectory(File testDirectory) {
        this.testDirectory = testDirectory;
    }

    public File getTestOutputDirectory() {
        return testOutputDirectory;
    }

    public void setTestOutputDirectory(File testOutputDirectory) {
        this.testOutputDirectory = testOutputDirectory;
    }

}

/*
 * The MIT License
 *
 * Copyright (c) 2017 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.tools.configcloner.library;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


 //TODO: sort out character encoding

public class JenkinsLibrary {

    private Path libraryPath;
    private Path jobRepoPath;
    private Path propertiesPath;
    private Properties properties;

    public JenkinsLibrary(Path libraryPath) throws IOException {
        this.libraryPath = libraryPath;
        this.jobRepoPath = libraryPath.resolve("jobs");
        this.propertiesPath = libraryPath.resolve("jenkins.properties");
        loadProperties();
    }

    private void loadProperties() throws IOException {
        properties = new Properties();
        try (InputStream is = Files.newInputStream(propertiesPath)) {
            properties.load(is);
        }
    }

    public Path getLibraryPath() {
        return libraryPath;
    }

    public String getName() {
        return properties.getProperty("name");
    }

    public String getUrl() {
        String url = properties.getProperty("url");
        return url.endsWith("/") ? url : url + "/";
    }

    public List<String> listJobs() throws IOException {
        List<String> jobs = new ArrayList<>();
        Files.list(jobRepoPath).forEach(path -> jobs.add( path.getFileName().toString()) );
        return jobs;
    }

    public String getJobXml(String jobName) throws IOException {
        Path jobPath = jobRepoPath.resolve(jobName);
        return new String (Files.readAllBytes(jobPath));
    }
}

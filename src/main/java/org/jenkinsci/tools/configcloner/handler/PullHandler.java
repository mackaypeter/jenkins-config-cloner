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

package org.jenkinsci.tools.configcloner.handler;

import hudson.cli.NoCheckTrustManager;
import org.jenkinsci.tools.configcloner.CommandResponse;
import org.jenkinsci.tools.configcloner.ConfigTransfer;
import org.jenkinsci.tools.configcloner.library.JenkinsLibrary;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class PullHandler implements Handler {

    @Argument(multiValued = false, usage = "[<JENKINS_NAME>]", metaVar = "JENKINS_NAME")
    protected String jenkinsName;

    @Option(name = "-l", aliases = { "--local-copy" }, usage = "Specify a directory for local copy storage.")
    protected String localLibrary = "";

    @Option(name = "-i", aliases = { "--insecure" }, usage = "Do not check SSL certificate")
    private void setInsecure(boolean insecure) throws NoSuchAlgorithmException, KeyManagementException {
        if (insecure == true) {
            System.out.println("Skipping HTTPS certificate checks altogether. Note that this is not secure at all.");
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[]{new NoCheckTrustManager()}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
            // bypass host name check, too.
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });
        }
    }

    protected final ConfigTransfer config;

    public PullHandler(ConfigTransfer config) {
        this.config = config;
    }

    @Override
    public CommandResponse run(CommandResponse response) {
        JenkinsLibrary jenkins = null;
        List<String> jobs = null;
        try {
            jenkins = new JenkinsLibrary(Paths.get(localLibrary, jenkinsName));
            jobs = jenkins.listJobs();
        } catch (IOException e) {
            response.err().println("Could not load local jenkins library: " + e);
            return response;
        }
        String jenkinsUrl = jenkins.getUrl();
        List<String> sources = new ArrayList<>();
        jobs.stream().forEach(job -> sources.add(jenkinsUrl + "job/" + job));
        new DownloadJob(config).setEntites(sources)
            .setLocalLibraryDir(localLibrary)
            .setForce(true)
            .run(response);
        return response;
    }

    @Override
    public String name() {
        return "pull";
    }

    @Override
    public String description() {
        return "Download and rewrite job configurations for all jobs in the local library for <JENKINS_NAME>";
    }
}

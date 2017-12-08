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

import org.jenkinsci.tools.configcloner.CommandResponse;
import org.jenkinsci.tools.configcloner.ConfigDestination;
import org.jenkinsci.tools.configcloner.ConfigTransfer;
import org.jenkinsci.tools.configcloner.UrlParser;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadJob extends TransferHandler {

    public DownloadJob(final ConfigTransfer config) {

        super(config);
    }

    @Override
    public CommandResponse run(final CommandResponse response) {

        if (localLibrary.isEmpty()) {
            response.err().println("You must specify the local library directory.");
            return response;
        }

        // Get both of these before doing any work to fail validation early.
        final List<ConfigDestination> sources = this.sources();

        for (ConfigDestination source : sources) {
            download(source, response);
        }

        return response;
    }

    private CommandResponse download(
            final ConfigDestination source,
            final CommandResponse response
    ) {

        response.out().println("Fetching " + source);
        final CommandResponse.Accumulator xml = config.execute(
            source, "", this.getCommandName(), source.entity()
        );

        if (!xml.succeeded()) return response.merge(xml);

        final String xmlString = getXml(fixupConfig(xml.stdout(), source), response);

        createLocalCopy(xmlString, source);

        return response;
    }

    @Override
    protected String getCommandName() {
        return "get-job";
    }

    @Override
    protected String updateCommandName() {
        return "update-job";
    }

    @Override
    protected String createCommandName() {
        return "create-job";
    }

    @Override
    protected String deleteCommandName() {
        return "delete-job";
    }

    protected List<ConfigDestination> sources() {
        if (entities == null || entities.size() < 1) throw new IllegalArgumentException(
            "Expecting 1 or more positional arguments"
        );
        List<ConfigDestination> sources = new ArrayList<>();
        for (String entity : entities) {
            sources.add(urlParser().destination(entity));
        }
        return sources;
    }

    @Override
    protected UrlParser urlParser() {
        return new UrlParser(force) {
            @Override
            protected ConfigDestination parseDestination(final URL url) {

                final Matcher urlMatcher = Pattern
                    .compile("^(.*?/)(?:view/[^/]+/)*job/([^/]+).*")
                    .matcher(url.toString())
                    ;

                if (!urlMatcher.matches()) return new ConfigDestination(url, "");

                final String jenkins = urlMatcher.group(1);
                String entity = urlMatcher.group(2);

                return new ConfigDestination(jenkins, entity);
            }
        };
    }

    public String name() {
        return "download-job";
    }

    public String description() {
        return "Clone job configuration from <SRC> to <DST>";
    }
}

/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.torquebox.mojo.rubygems.layout;

import org.jruby.runtime.builtin.IRubyObject;
import org.torquebox.mojo.rubygems.ApiV1File;
import org.torquebox.mojo.rubygems.ApiV2File;
import org.torquebox.mojo.rubygems.DependencyFile;
import org.torquebox.mojo.rubygems.FileType;
import org.torquebox.mojo.rubygems.GemFile;
import org.torquebox.mojo.rubygems.GemspecFile;
import org.torquebox.mojo.rubygems.GemspecHelper;
import org.torquebox.mojo.rubygems.IOUtil;
import org.torquebox.mojo.rubygems.CompactInfoFile;
import org.torquebox.mojo.rubygems.RubygemsFile;
import org.torquebox.mojo.rubygems.RubygemsGateway;
import org.torquebox.mojo.rubygems.SpecsHelper;
import org.torquebox.mojo.rubygems.SpecsIndexFile;
import org.torquebox.mojo.rubygems.SpecsIndexType;
import org.torquebox.mojo.rubygems.SpecsIndexZippedFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * to make a HTTP POST to hosted repository allows only two path: <ul>
 * <li>/api/v1/gem</li>
 * <li>/gems/{name-version(-platform)}.gem</li></ul>
 * <p>
 * all other paths are forbidden.
 * <p>
 * when uploading a gem all the specs.4.8 files will be updated.
 * <p>
 * the dependency file for the gemname and gemspec file will be generated on
 * demand.
 *
 * @author christian
 * @see HostedGETLayout
 */
public class HostedPOSTLayout extends NoopDefaultLayout {
    public HostedPOSTLayout(RubygemsGateway gateway, Storage store) {
        super(gateway, store);
    }

    @Override
    public void addGem(InputStream in, RubygemsFile file) {
        if (file.type() != FileType.GEM && file.type() != FileType.API_V1) {
            throw new RuntimeException("BUG: not allowed to store " + file);
        }
        try {
            store.create(in, file);
            if (file.hasNoPayload()) {
                // an error or something else but we need the payload now
                return;
            }
            GemspecHelper spec;
            try (InputStream is = store.getInputStream(file)) {
                spec = gateway.newGemspecHelperFromGem(is);
            }

            // check gemname matches coordinates from its specification
            switch (file.type()) {
                case GEM:
                    if (!(((GemFile) file).filename() + ".gem").equals(spec.filename())) {
                        store.delete(file);
                        // now set the error for further processing
                        file.setException(new IOException("filename " + file.name() + " does not match gemname: " + spec.filename()));
                        return;
                    }
                    break;
                case API_V1:
                    try (InputStream is = store.getInputStream(file)) {
                        store.create(is, ((ApiV1File) file).gem(spec.filename()));
                    }
                    store.delete(file);
                    break;
                default:
                    throw new RuntimeException("BUG");
            }

            addSpecToIndex(spec.gemspec());

            // delete dependencies so the next request will recreate it
            delete(super.dependencyFile(spec.name()));
            // delete gemspec so the next request will recreate it
            delete(super.gemspecFile(spec.filename().replaceFirst(".gem$", "")));
        } catch (IOException e) {
            file.setException(e);
        }
    }

    /**
     * add a spec (Ruby Object) to the specs.4.8 indices.
     */
    private void addSpecToIndex(IRubyObject spec) throws IOException {
        SpecsHelper specs = gateway.newSpecsHelper();
        for (SpecsIndexType type : SpecsIndexType.values()) {
            SpecsIndexZippedFile specsIndex = ensureSpecsIndexZippedFile(type);

            ByteArrayInputStream gzippedResult = null;
            try (InputStream in = new GZIPInputStream(store.getInputStream(specsIndex))) {
                try (InputStream result = specs.addSpec(spec, in, type)) {
                    // if nothing was added the content is NULL
                    if (result != null) {
                        gzippedResult = IOUtil.toGzipped(result);
                    }
                }
            }
            if (gzippedResult != null) {
                store.update(gzippedResult, specsIndex);
            }
        }
    }

    @Override
    public ApiV1File apiV1File(String name) {
        ApiV1File apiV1 = super.apiV1File(name);
        if (!"api_key".equals(apiV1.name())) {
            apiV1.markAsForbidden();
        }
        return apiV1;
    }

    @Override
    public SpecsIndexFile specsIndexFile(SpecsIndexType type) {
        SpecsIndexFile file = super.specsIndexFile(type);
        file.markAsForbidden();
        return file;
    }

    @Override
    public SpecsIndexZippedFile specsIndexZippedFile(SpecsIndexType type) {
        SpecsIndexZippedFile file = super.specsIndexZippedFile(type);
        file.markAsForbidden();
        return file;
    }

    @Override
    public SpecsIndexFile specsIndexFile(String name) {
        SpecsIndexFile file = super.specsIndexFile(name);
        file.markAsForbidden();
        return file;
    }

    @Override
    public SpecsIndexZippedFile specsIndexZippedFile(String name) {
        SpecsIndexZippedFile file = super.specsIndexZippedFile(name);
        file.markAsForbidden();
        return file;
    }

    @Override
    public GemspecFile gemspecFile(String name, String version, String platform) {
        GemspecFile file = super.gemspecFile(name, version, platform);
        file.markAsForbidden();
        return file;
    }

    @Override
    public GemspecFile gemspecFile(String name) {
        GemspecFile file = super.gemspecFile(name);
        file.markAsForbidden();
        return file;
    }

    @Override
    @Deprecated
    public DependencyFile dependencyFile(String name) {
        DependencyFile file = super.dependencyFile(name);
        file.markAsForbidden();
        return file;
    }

    @Override
    public ApiV2File rubygemsInfoV2(String name, String version) {
        ApiV2File file = super.rubygemsInfoV2(name, version);
        file.markAsForbidden();
        return file;
    }

    @Override
    public CompactInfoFile compactInfo(String name) {
        CompactInfoFile file = super.compactInfo(name);
        file.markAsForbidden();
        return file;
    }
}

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
import org.torquebox.mojo.rubygems.GemFile;
import org.torquebox.mojo.rubygems.GemspecFile;
import org.torquebox.mojo.rubygems.GemspecHelper;
import org.torquebox.mojo.rubygems.IOUtil;
import org.torquebox.mojo.rubygems.CompactInfoFile;
import org.torquebox.mojo.rubygems.RubygemsGateway;
import org.torquebox.mojo.rubygems.SpecsHelper;
import org.torquebox.mojo.rubygems.SpecsIndexFile;
import org.torquebox.mojo.rubygems.SpecsIndexType;
import org.torquebox.mojo.rubygems.SpecsIndexZippedFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * on hosted rubygems repositories you can delete only gem files. deleting
 * a gem file also means to adjust the specs.4.8 indices and their associated
 * dependency file as well deleting the gemspec file of the gem.
 * <p>
 * note: to restrict deleting to gem file is more precaution then a necessity.
 * <ul><li>it is possible to regenerate the gemspec file from the gem</li>
 * <li>to generate the dependencies file from the stored gems of the same name</li>
 * <li>even the specs.4.8 can be regenerated from the stored gems</li></ul>
 *
 * @author christian
 * @see HostedGETLayout
 */
// TODO why not using DELETELayout instead of NoopDefaultLayout ?
public class HostedDELETELayout extends NoopDefaultLayout {
    public HostedDELETELayout(RubygemsGateway gateway, Storage store) {
        super(gateway, store);
    }

    @Override
    public SpecsIndexFile specsIndexFile(String name) {
        SpecsIndexFile file = super.specsIndexFile(name);
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

    @Override
    public ApiV1File apiV1File(String name) {
        ApiV1File file = super.apiV1File(name);
        file.markAsForbidden();
        return file;
    }

    @Override
    public GemFile gemFile(String name, String version, String platform) {
        GemFile file = super.gemFile(name, version, platform);
        deleteGemFile(file);
        return file;
    }

    @Override
    public GemFile gemFile(String name) {
        GemFile file = super.gemFile(name);
        deleteGemFile(file);
        return file;
    }

    /**
     * delete the gem and its metadata in the specs.4.8 indices and
     * dependency file
     */
    private void deleteGemFile(GemFile file) {
        store.retrieve(file);
        try (InputStream is = store.getInputStream(file)) {
            GemspecHelper spec = gateway.newGemspecHelperFromGem(is);

            deleteSpecFromIndex(spec.gemspec());

            // delete dependencies so the next request will recreate it
            delete(super.dependencyFile(spec.name()));
            // delete the gemspec.rz altogether
            delete(super.gemspecFile(file.filename()));
        } catch (IOException e) {
            file.setException(e);
        }
        store.delete(file);
    }

    /**
     * delete given spec (a Ruby Object) and delete it from all the specs.4.8 indices.
     */
    private void deleteSpecFromIndex(IRubyObject spec) throws IOException {
        SpecsHelper specs = gateway.newSpecsHelper();
        for (SpecsIndexType type : SpecsIndexType.values()) {
            SpecsIndexZippedFile specsIndex = ensureSpecsIndexZippedFile(type);
            try (InputStream in = new GZIPInputStream(store.getInputStream(specsIndex))) {
                try (InputStream result = specs.deleteSpec(spec, in, type)) {
                    // if nothing was added the content is NULL
                    if (result != null) {
                        store.update(IOUtil.toGzipped(result), specsIndex);
                    }
                }
            }
        }
    }
}

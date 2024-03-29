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

import org.torquebox.mojo.rubygems.ApiV2File;
import org.torquebox.mojo.rubygems.BundlerApiFile;
import org.torquebox.mojo.rubygems.DependencyFile;
import org.torquebox.mojo.rubygems.DependencyHelper;
import org.torquebox.mojo.rubygems.GemFile;
import org.torquebox.mojo.rubygems.GemspecFile;
import org.torquebox.mojo.rubygems.GemspecHelper;
import org.torquebox.mojo.rubygems.CompactInfoFile;
import org.torquebox.mojo.rubygems.RubygemsGateway;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class ProxiedGETLayout
        extends GETLayout {
    private final ProxyStorage store;

    public ProxiedGETLayout(RubygemsGateway gateway, ProxyStorage store) {
        super(gateway, store);
        this.store = store;
    }

    private void maybeCreate(GemspecFile gemspec) {
        if (gemspec.notExists() || gemspec.hasException()) {
            Exception exp = gemspec.getException();
            GemFile gem = gemspec.gem();
            store.retrieve(gem);
            if (gem.exists()) {
                try {
                    GemspecHelper helper = gateway.newGemspecHelperFromGem(store.getInputStream(gem));
                    store.update(helper.getRzInputStream(), gemspec);
                    store.expireNow(gemspec);
                } catch (IOException e) {
                    // in this case we stick to the original error of the gemspec file
                    if (exp != null) {
                        gemspec.setException(exp);
                    }
                }
            }
        }
    }

    @Override
    public GemspecFile gemspecFile(String name, String version, String platform) {
        GemspecFile gemspec = super.gemspecFile(name, version, platform);
        maybeCreate(gemspec);
        return gemspec;
    }

    @Override
    public GemspecFile gemspecFile(String filename) {
        GemspecFile gemspec = super.gemspecFile(filename);
        maybeCreate(gemspec);
        return gemspec;
    }

    @Override
    @Deprecated
    public DependencyFile dependencyFile(String name) {
        DependencyFile file = super.dependencyFile(name);
        store.retrieve(file);
        return file;
    }

    @Override
    public ApiV2File rubygemsInfoV2(String name, String version) {
        ApiV2File file = super.rubygemsInfoV2(name, version);
        store.retrieve(file);
        return file;
    }

    @Override
    public CompactInfoFile compactInfo(String name) {
        CompactInfoFile file = super.compactInfo(name);
        store.retrieve(file);
        return file;
    }

    @Override
    protected void retrieveAll(BundlerApiFile file, DependencyHelper deps) throws IOException {
        List<String> expiredNames = new LinkedList<>();
        for (String name : file.gemnames()) {
            CompactInfoFile dep = super.compactInfo(name);
            if (store.isExpired(dep)) {
                expiredNames.add(name);
            } else {
                store.retrieve(dep);
                try (InputStream is = store.getInputStream(dep)) {
                    deps.addCompact(is, name, store.getModified(dep));
                }
            }
        }
        if (expiredNames.size() > 0) {
            BundlerApiFile expired = super.bundlerApiFile(expiredNames.toArray(new String[expiredNames.size()]));
            store.retrieve(expired);
            if (expired.hasException()) {
                file.setException(expired.getException());
            } else if (expired.hasPayload()) {
                DependencyHelper bundlerDeps = gateway.newDependencyHelper();
                try (InputStream bundlerResult = store.getInputStream(expired)) {
                    bundlerDeps.addCompact(bundlerResult, expired.name(), store.getModified(expired));
                }
                for (String gemname : expiredNames) {
                    CompactInfoFile dep = super.compactInfo(gemname);
                    // first store the data for caching
                    store.update(bundlerDeps.getInputStreamOf(gemname), dep);
                    // then add it to collector
                    try (InputStream is = store.getInputStream(dep)) {
                        deps.addCompact(is, gemname, store.getModified(dep));
                    }
                }
            } else {
                // no payload so let's fall back and add the expired content
                for (String name : expiredNames) {
                    CompactInfoFile dep = super.compactInfo(name);
                    store.retrieve(dep);
                    try (InputStream is = store.getInputStream(dep)) {
                        deps.addCompact(is, name, store.getModified(dep));
                    }
                }
            }
        }
    }
}

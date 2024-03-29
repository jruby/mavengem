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
package org.torquebox.mojo.rubygems;

import org.jruby.RubyInstanceConfig;
import org.jruby.embed.ScriptingContainer;

import java.io.InputStream;

public class DefaultRubygemsGateway
        implements RubygemsGateway {
    private final ScriptingContainer container;
    private Object dependencyHelperImplClass;
    private Object gemspecHelperImplClass;
    private Object specsHelperImplClass;
    private Object mergeSpecsHelperImplClass;
    private Object dependencyDataImplClass;
    private Object rubygemsV2GemInfoImplClass;
    private Object compactDependencyDataImplClass;

    /**
     * Ctor that accepts prepared non-null scripting container.
     */
    public DefaultRubygemsGateway(final ScriptingContainer container) {
        this.container = container;
        this.container.setCompileMode(RubyInstanceConfig.CompileMode.OFF);
        dependencyHelperImplClass = container.runScriptlet("require 'nexus/dependency_helper_impl';"
                + "Nexus::DependencyHelperImpl");
        gemspecHelperImplClass = container.runScriptlet("require 'nexus/gemspec_helper_impl';"
                + "Nexus::GemspecHelperImpl");
        specsHelperImplClass = container.runScriptlet("require 'nexus/specs_helper_impl';"
                + "Nexus::SpecsHelperImpl");
        mergeSpecsHelperImplClass = container.runScriptlet("require 'nexus/merge_specs_helper_impl';"
                + "Nexus::MergeSpecsHelperImpl");
        dependencyDataImplClass = container.runScriptlet("require 'nexus/dependency_data_impl';"
                + "Nexus::DependencyDataImpl");
        rubygemsV2GemInfoImplClass = container.runScriptlet("require 'nexus/rubygems_v2_gem_info_impl';"
                + "Nexus::RubygemsV2GemInfoImpl");
        compactDependencyDataImplClass = container.runScriptlet("require 'nexus/compact_dependency_data';"
                + "Nexus::CompactDependencyData");
    }

    @Override
    public void terminate() {
        dependencyHelperImplClass = null;
        gemspecHelperImplClass = null;
        specsHelperImplClass = null;
        mergeSpecsHelperImplClass = null;
        dependencyDataImplClass = null;
        rubygemsV2GemInfoImplClass = null;
        compactDependencyDataImplClass = null;
        container.terminate();
    }

    @Override
    public SpecsHelper newSpecsHelper() {
        return container.callMethod(specsHelperImplClass, "new", SpecsHelper.class);
    }

    @Override
    public MergeSpecsHelper newMergeSpecsHelper() {
        return container.callMethod(mergeSpecsHelperImplClass, "new", MergeSpecsHelper.class);
    }

    @Override
    public DependencyHelper newDependencyHelper() {
        return container.callMethod(dependencyHelperImplClass, "new", DependencyHelper.class);
    }

    @Override
    public GemspecHelper newGemspecHelper(InputStream gemspec) {
        return container.callMethod(gemspecHelperImplClass, "from_gemspec_rz", gemspec, GemspecHelper.class);
    }

    @Override
    public GemspecHelper newGemspecHelperFromGem(InputStream gem) {
        return container.callMethod(gemspecHelperImplClass, "from_gem", gem, GemspecHelper.class);
    }

    @Override
    public GemspecHelper newGemspecHelperFromV2GemInfo(InputStream gem) {
        return container.callMethod(gemspecHelperImplClass, "from_rubygems_v2_gem_info", gem, GemspecHelper.class);
    }

    @Override
    public DependencyData newDependencyData(InputStream dependency, String name, long modified) {
        return container.callMethod(dependencyDataImplClass, "new", new Object[]{dependency, name, modified},
                DependencyData.class);
    }

    @Override
    public RubygemsV2GemInfo newRubygemsV2GemInfo(InputStream apiV2FileIS, String name, String version, long modified) {
        return container.callMethod(rubygemsV2GemInfoImplClass, "new", new Object[]{apiV2FileIS, name, version, modified},
                RubygemsV2GemInfo.class);
    }

    @Override
    public DependencyData newCompactDependencyData(InputStream dependency, String name, long modified) {
        return container.callMethod(compactDependencyDataImplClass, "new", new Object[]{dependency, name, modified},
                DependencyData.class);
    }
}
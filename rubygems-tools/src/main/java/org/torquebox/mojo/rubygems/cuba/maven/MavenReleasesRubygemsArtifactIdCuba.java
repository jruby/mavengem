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
package org.torquebox.mojo.rubygems.cuba.maven;

import org.torquebox.mojo.rubygems.MavenMetadataFile;
import org.torquebox.mojo.rubygems.RubygemsFile;
import org.torquebox.mojo.rubygems.cuba.Cuba;
import org.torquebox.mojo.rubygems.cuba.State;

/**
 * cuba for /maven/releases/rubygems/{artifactId}
 *
 * @author christian
 */
public class MavenReleasesRubygemsArtifactIdCuba
        implements Cuba {

    public static final String MAVEN_METADATA_XML = "maven-metadata.xml";

    private final String artifactId;

    public MavenReleasesRubygemsArtifactIdCuba(String artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * directories one for each version of the gem with given name/artifactId
     * <p>
     * files [maven-metadata.xml,maven-metadata.xml.sha1]
     */
    @Override
    public RubygemsFile on(State state) {
        switch (state.name) {
            case MavenReleasesRubygemsArtifactIdCuba.MAVEN_METADATA_XML:
                return state.context.factory.mavenMetadata(artifactId, false);
            case MavenReleasesRubygemsArtifactIdCuba.MAVEN_METADATA_XML + ".sha1":
                MavenMetadataFile file = state.context.factory.mavenMetadata(artifactId, false);
                return state.context.factory.sha1(file);
            case "":
                return state.context.factory.gemArtifactIdDirectory(state.context.original, artifactId, false);
            default:
                return state.nested(new MavenReleasesRubygemsArtifactIdVersionCuba(artifactId, state.name));
        }
    }
}
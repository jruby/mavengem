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

/**
 * represents /maven/releases/rubygems/{name}/maven-metadata.xml or /maven/prereleases/rubygems/{name}/maven-metadata.xml
 *
 * @author christian
 */
public class MavenMetadataFile
        extends RubygemsFile {

    private final boolean prereleased;

    MavenMetadataFile(RubygemsFileFactory factory, String path, String name, boolean prereleased) {
        super(factory, FileType.MAVEN_METADATA, path, path, name);
        this.prereleased = prereleased;
    }

    /**
     * whether it is a prerelease or not
     */
    public boolean isPrerelease() {
        return prereleased;
    }

    /**
     * retrieve the associated DependencyFile
     */
    public CompactInfoFile dependency() {
        return factory.compactInfo(name());
    }
}
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
 * for the only v2 API currently, /api/v2/rubygems/NAME/versions/VERSION.json
 */
public class ApiV2File
        extends RubygemsFile {
    private final String version;

    ApiV2File(RubygemsFileFactory factory, String storage, String remote, String name, String version) {
        super(factory, FileType.JSON_API, storage, remote, name);

        this.version = version;

        set(null);// no payload
    }

    /**
     * the version of the gem
     */
    public String version() {
        return version;
    }
}
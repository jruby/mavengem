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

import org.torquebox.mojo.rubygems.CompactInfoFile;
import org.torquebox.mojo.rubygems.DependencyFile;
import org.torquebox.mojo.rubygems.Directory;
import org.torquebox.mojo.rubygems.RubygemsFile;
import org.torquebox.mojo.rubygems.SpecsIndexFile;
import org.torquebox.mojo.rubygems.SpecsIndexZippedFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * storage abstraction using <code>RubygemsFile</code>. all the CRUD methods do set the
 * the payload. these CRUD methods do NOT throw exceptions but sets those exceptions
 * as payload of the passed in <code>RubygemsFile</code>.
 * <p>
 * for GroupRepositories the <code>SpecsIndexFile</code>, <code>SpecsIndexZippedFile</code>
 * and <code>DependencyFile</code> needs to be merged, all other files will be served the first find.
 *
 * @author christian
 */
public interface Storage {
    /**
     * create the given file from an <code>InputStream</code>.
     */
    void create(InputStream is, RubygemsFile file);

    /**
     * retrieve the payload of the given file.
     */
    void retrieve(RubygemsFile file);

    /**
     * retrieve the payload of the given file.
     */
    void retrieve(SpecsIndexFile file);

    /**
     * retrieve the payload of the given file.
     */
    void retrieve(SpecsIndexZippedFile file);

    /**
     * retrieve the payload of the given file.
     */
    void retrieve(DependencyFile file);

    /**
     * retrieve the payload of the given file.
     */
    void retrieve(CompactInfoFile file);

    /**
     * update the given file from an <code>InputStream</code>.
     */
    void update(InputStream is, RubygemsFile file);

    /**
     * delete the given file.
     */
    void delete(RubygemsFile file);

    /**
     * use the <code>String</code> to generate the payload
     * for the <code>RubygemsFile</code> instance.
     */
    void memory(ByteArrayInputStream data, RubygemsFile file);

    /**
     * use the <code>String</code> can converts it with to <code>byte</code> array
     * for the the payload of the <code>RubygemsFile</code> instance.
     */
    void memory(String data, RubygemsFile file);

    /**
     * get an <code>inputStream</code> to actual file from the physical storage.
     *
     * @throws IOException on IO related errors or
     *                     wrapped the exception if the payload has an exception.
     */
    InputStream getInputStream(RubygemsFile file) throws IOException;

    /**
     * get the last-modified unix time for the given file from the physical storage location.
     */
    long getModified(RubygemsFile file);

    /**
     * list given <code>Directory</code> from the physical storage location.
     */
    String[] listDirectory(Directory dir);
}

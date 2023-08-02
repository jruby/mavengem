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
package org.torquebox.mojo.rubygems.cuba.api;

import org.torquebox.mojo.rubygems.RubygemsFile;
import org.torquebox.mojo.rubygems.cuba.Cuba;
import org.torquebox.mojo.rubygems.cuba.State;

/**
 * cuba for /info/GEMNAME
 */
public class CompactInfoCuba implements Cuba {
    public CompactInfoCuba() {
    }

    /**
     * json listing of all versions of a given gem with dependencies
     */
    @Override
    public RubygemsFile on(State state) {
        if (state.name != null) {
            String baseName = state.name;
            int ext = baseName.indexOf(".compact");
            if (ext != -1) {
                baseName = baseName.substring(0, ext);
            }
            return state.context.factory.compactInfo(baseName);
        }

        return state.context.factory.noContent(state.path);
    }
}
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
 * cuba for /api/v2/rubygems to fetch gem details
 */
public class ApiV2RubygemsCuba implements Cuba {
    /**
     * directory [dependencies], files [api_key,gems]
     */
    @Override
    public RubygemsFile on(State state) {
        if (state.name.isEmpty()) {
            return state.context.factory.notFound(state.context.original);
        }
        return state.nested(new ApiV2RubygemsNameCuba(state.name));
    }
}
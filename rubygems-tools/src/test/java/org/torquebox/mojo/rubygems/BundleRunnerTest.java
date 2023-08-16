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

import org.junit.Rule;
import org.junit.Test;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.torquebox.mojo.rubygems.TestUtils.lastLine;
import static org.torquebox.mojo.rubygems.TestUtils.numberOfLines;

public class BundleRunnerTest
        extends TestSupport {
    @Rule
    public TestJRubyContainerRule testJRubyContainerRule = new TestJRubyContainerRule();

    @Test
    public void testInstall() throws Exception {
        final BundleRunner runner = new BundleRunner(testJRubyContainerRule.getScriptingContainer());
        String output = runner.install();
        System.err.println("bundler output:" + output);
        assertThat(numberOfLines(output), is(14));
        assertThat(lastLine(output),
                startsWith("Use `bundle info [gemname]` to see where a bundled gem is installed."));
    }

    @Test
    public void testShowAll() throws Exception {
        final BundleRunner runner = new BundleRunner(testJRubyContainerRule.getScriptingContainer());
        final String output = runner.show();
        System.err.println("bundler output:" + output);
        assertThat(numberOfLines(output), is(7));
    }

    @Test
    public void testShow() throws Exception {
        final BundleRunner runner = new BundleRunner(testJRubyContainerRule.getScriptingContainer());
        String zip = runner.show("zip");
        System.err.println("bundler output:" + zip);
        /*
           Disable this check due to some behavior causing extra output on MacOS:
             "Found no changes, using resolution from the lockfile"
           which causes test failures here due to the extra informational line of
           text. We disable this for now as it appears to be environmental.
         */
        //assertThat(numberOfLines(zip), is(2));
        assertThat(lastLine(zip), endsWith("zip-2.0.2"));
    }

    @Test
    public void testConfig() throws Exception {
        final BundleRunner runner = new BundleRunner(testJRubyContainerRule.getScriptingContainer());
        // FIXME: @headius not sure what this is supposed to test but the output has changed
        assertThat(runner.config(), containsString("Settings are listed in order of priority"));
    }
}

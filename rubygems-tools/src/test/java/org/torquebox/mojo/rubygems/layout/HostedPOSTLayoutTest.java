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

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.torquebox.mojo.rubygems.FileType;
import org.torquebox.mojo.rubygems.RubyScriptingTestSupport;
import org.torquebox.mojo.rubygems.RubygemsFile;
import org.torquebox.mojo.rubygems.cuba.DefaultRubygemsFileSystem;
import org.torquebox.mojo.rubygems.layout.SimpleStorage.BytesStreamLocation;
import org.torquebox.mojo.rubygems.layout.SimpleStorage.URLGzipStreamLocation;
import org.torquebox.mojo.rubygems.layout.SimpleStorage.URLStreamLocation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@RunWith(Parameterized.class)
public class HostedPOSTLayoutTest
        extends RubyScriptingTestSupport {
    private final DefaultRubygemsFileSystem fileSystem;
    private final boolean isHosted;

    public HostedPOSTLayoutTest(Storage store) throws IOException {
        if (store instanceof CachingProxyStorage) {
            isHosted = false;
            fileSystem =
                    new DefaultRubygemsFileSystem(new HostedGETLayout(rubygemsGateway(),
                            new SimpleStorage(hostedBase())),
                            null,
                            null);
        } else {
            isHosted = true;
            fileSystem =
                    new DefaultRubygemsFileSystem(new HostedGETLayout(rubygemsGateway(),
                            new SimpleStorage(hostedBase())),
                            new HostedPOSTLayout(rubygemsGateway(),
                                    store),
                            null);
        }
    }

    private static File proxyBase() throws IOException {
        File base = new File("target/hosted-post-proxy");
        FileUtils.deleteDirectory(base);
        return base;
    }

    private static File hostedBase() throws IOException {
        File base = new File("target/hosted-post-repo");
        return base;
    }

    @Parameters
    public static Collection<Object[]> stores() throws IOException {
        return Arrays.asList(new Object[][]{
                {new SimpleStorage(hostedBase())},
                {
                        new CachingProxyStorage(proxyBase(), hostedBase().toURI().toURL()) {

                            protected URLStreamLocation toUrl(RubygemsFile file) throws MalformedURLException {
                                return new URLStreamLocation(new URL(baseurl + file.storagePath()));
                            }
                        }
                }
        });
    }

    @Before
    public void pushGem() throws IOException {
        if (isHosted) {
            // only the hosted one gets a new repo and a new gem
            File base = hostedBase();
            File source = new File("src/test/hostedrepo");
            FileUtils.deleteDirectory(base);
            FileUtils.copyDirectory(source, base, true);
            fileSystem.post(new FileInputStream("src/test/second-2.gem"),
                    "/gems/second-2.gem");
        } else {
            // get those files in place for the proxy to find
            fileSystem.get("/quick/Marshal.4.8/pre-0.1.0.beta.gemspec.rz");
            fileSystem.get("/api/v1/dependencies/pre.ruby");
            fileSystem.get("/quick/Marshal.4.8/zip-2.0.2.gemspec.rz");
            fileSystem.get("/api/v1/dependencies/zip.ruby");
            fileSystem.get("/quick/Marshal.4.8/second-2.gemspec.rz");
            fileSystem.get("/api/v1/dependencies/second.ruby");
        }
    }

    @Test
    public void testSpecsZippedIndex() throws Exception {
        String[] pathes = {
                "/specs.4.8.gz",
                "/prerelease_specs.4.8.gz",
                "/latest_specs.4.8.gz"
        };
        assertFiletypeWithPayload(pathes, FileType.SPECS_INDEX_ZIPPED, URLStreamLocation.class);
    }

    @Test
    public void testSpecsUnzippedIndex() throws Exception {
        String[] pathes = {
                "/specs.4.8",
                "/prerelease_specs.4.8",
                "/latest_specs.4.8"
        };
        assertFiletypeWithPayload(pathes, FileType.SPECS_INDEX, URLGzipStreamLocation.class);
    }

    @Test
    public void testSha1() throws Exception {

        String[] pathes = {
                "/maven/releases/rubygems/zip/2.0.2/zip-2.0.2.gem.sha1",
                "/maven/releases/rubygems/zip/2.0.2/zip-2.0.2.pom.sha1",
                "/maven/prereleases/rubygems/psych/5.1.0.pre1/psych-5.1.0.pre1-123213123.gem.sha1",
                "/maven/prereleases/rubygems/psych/5.1.0.pre1/psych-5.1.0.pre1-123213123.pom.sha1",
                "/maven/releases/rubygems/psych/5.1.0.pre1/psych-5.1.0.pre1.gem.sha1",
                "/maven/releases/rubygems/psych/5.1.0.pre1/psych-5.1.0.pre1.pom.sha1"
        };
        String[] shas = {
                "6fabc32da123f7013b2db804273df428a50bc6a4",
                "cc658431eb13f47cd1ad10ab5e937676040317fc",
                "edfb9b1923514277b7256b0670a388da43706d39",
                "2b57344d1f255519ae19b34cd3836bbe35721c72",
                "edfb9b1923514277b7256b0670a388da43706d39",
                "d162aa950471128b37485453091ca10086aabdc6"
        };

        assertFiletypeWithPayload(pathes, FileType.SHA1, shas);

        // these files carry a timestamp of creation of the .ruby file
        pathes = new String[]{
                "/maven/releases/rubygems/zip/maven-metadata.xml.sha1",
                "/maven/prereleases/rubygems/psych/maven-metadata.xml.sha1",
                "/maven/prereleases/rubygems/psych/5.1.0.pre1-SNAPSHOT/maven-metadata.xml.sha1",
                "/maven/releases/rubygems/psych/maven-metadata.xml.sha1"
        };
        assertFiletypeWithPayload(pathes, FileType.SHA1, BytesStreamLocation.class);
    }

    @Test
    public void testGemArtifact() throws Exception {
        String[] pathes = {
                "/maven/releases/rubygems/zip/2.0.2/zip-2.0.2.gem",
                "/maven/releases/rubygems/psych/5.1.0.pre1/psych-5.1.0.pre1.gem",
                "/maven/prereleases/rubygems/psych/5.1.0.pre1/psych-5.1.0.pre1-123213123.gem"
        };
        assertFiletypeWithPayload(pathes, FileType.GEM_ARTIFACT, URLStreamLocation.class);
    }

    @Test
    public void testPom() throws Exception {
        String[] pathes = {
                "/maven/releases/rubygems/zip/2.0.2/zip-2.0.2.pom",
                "/maven/releases/rubygems/psych/5.1.0.pre1/psych-5.1.0.pre1.pom",
                "/maven/prereleases/rubygems/psych/5.1.0.pre1/psych-5.1.0.pre1-123213123.pom"
        };
        String[] xmls = {
                loadPomResource("zip.pom"),
                loadPomResource("psych-release.pom"),
                loadPomResource("psych-prerelease.pom")
        };
        assertFiletypeWithPayload(pathes, FileType.POM, xmls);
    }

    @Test
    public void testMavenMetadata() throws Exception {
        String[] pathes = {
                "/maven/releases/rubygems/zip/maven-metadata.xml",
                "/maven/releases/rubygems/psych/maven-metadata.xml",
                "/maven/prereleases/rubygems/psych/maven-metadata.xml"
        };
        String[] xmls = {
                loadPomResource("zip-release-metadata.xml"),
                loadPomResource("psych-release-metadata.xml"),
                loadPomResource("psych-prerelease-metadata.xml")
        };
        assertFiletypeWithPayload(pathes, FileType.MAVEN_METADATA, xmls);
    }

    @Test
    public void testMavenMetadataSnapshot() throws Exception {
        String[] pathes = {"/maven/prereleases/rubygems/pre/0.1.0-SNAPSHOT/maven-metadata.xml"};
        String[] xmls = {
                "<metadata>\n"
                        + "  <groupId>rubygems</groupId>\n"
                        + "  <artifactId>pre</artifactId>\n"
                        + "  <versioning>\n"
                        + "    <versions>\n"
                        + "      <snapshot>\n"
                        + "        <timestamp>2014</timestamp>\n"
                        + "        <buildNumber>1</buildNumber>\n"
                        + "      </snapshot>\n"
                        + "      <lastUpdated>2014</lastUpdated>\n"
                        + "      <snapshotVersions>\n"
                        + "        <snapshotVersion>\n"
                        + "          <extension>gem</extension>\n"
                        + "          <value>0.1.0-2014-1</value>\n"
                        + "          <updated>2014</updated>\n"
                        + "        </snapshotVersion>\n"
                        + "        <snapshotVersion>\n"
                        + "          <extension>pom</extension>\n"
                        + "          <value>0.1.0-2014-1</value>\n"
                        + "          <updated>2014</updated>\n"
                        + "        </snapshotVersion>\n"
                        + "      </snapshotVersions>\n"
                        + "    </versions>\n"
                        + "  </versioning>\n"
                        + "</metadata>\n"
        };
        assertFiletypeWithPayload(pathes, FileType.MAVEN_METADATA_SNAPSHOT, xmls);
    }

    @Test
    public void testBundlerApi() throws Exception {
        String[] pathes = {"/api/v1/dependencies?gems=zip,pre", "/api/v1/dependencies?gems=zip,pre,second"};
        if (isHosted) {
            assertNotExists(pathes);
        } else {
            assertFiletypeWithPayload(pathes, FileType.BUNDLER_API, BytesStreamLocation.class);
        }
    }


    @Test
    public void testApiV1Gems() throws Exception {
        String[] pathes = {"/api/v1/gems"};
        assertForbiddenGet(pathes);
    }

    @Test
    public void testApiV1ApiKey() throws Exception {
        String[] pathes = {"/api/v1/api_key"};
        assertFiletypeWithNullPayload(pathes, FileType.API_V1);
    }

    @Test
    public void testGemspec() throws Exception {
        String[] pathes = {
                "/quick/Marshal.4.8/zip-2.0.2.gemspec.rz",
                "/quick/Marshal.4.8/z/zip-2.0.2.gemspec.rz",
                "/quick/Marshal.4.8/psych-5.1.0.pre1-java.gemspec.rz",
                "/quick/Marshal.4.8/p/psych-5.1.0.pre1-java.gemspec.rz"
        };
        assertFiletypeWithPayload(pathes, FileType.GEMSPEC, URLStreamLocation.class);
    }

    @Test
    public void testGem() throws Exception {
        String[] pathes = {
                "/gems/zip-2.0.2.gem", "/gems/z/zip-2.0.2.gem",
                "/gems/second-2.gem", "/gems/s/second-2.gem",
        };
        assertFiletypeWithPayload(pathes, FileType.GEM, URLStreamLocation.class);
    }

    @Test
    public void testDirectory() throws Exception {
        String[] pathes = {
                "/", "/api", "/api/", "/api/v1", "/api/v1/",
                "/api/v1/dependencies/", "/gems/", "/gems",
                "/maven/releases/rubygems/jbundler",
                "/maven/releases/rubygems/jbundler/1.2.3",
                "/maven/prereleases/rubygems/jbundler",
                "/maven/prereleases/rubygems/jbundler/1.2.3-SNAPSHOT",
        };
        assertFiletypeWithNullPayload(pathes, FileType.DIRECTORY);
    }

    @Test
    public void testNoContent() throws Exception {
        String[] pathes = {
                "/api/v1/dependencies", "/api/v1/dependencies?gems=",
        };
        assertFiletypeWithNullPayload(pathes, FileType.NO_CONTENT);
    }

    @Test
    public void testNotFound() throws Exception {
        String[] pathes = {
                "/asa", "/asa/", "/api/a", "/api/v1ds", "/api/v1/ds",
                "/api/v1/dependencies/jbundler.jruby", "/api/v1/dependencies/b/bundler.rubyd",
                "/api/v1/dependencies/basd/bundler.ruby",
                "/quick/Marshal.4.8/jbundler.jruby.rz", "/quick/Marshal.4.8/b/bundler.gemspec.rzd",
                "/quick/Marshal.4.8/basd/bundler.gemspec.rz",
                "/gems/jbundler.jssaonrz", "/gems/b/bundler.gemsa",
                "/gems/basd/bundler.gem",
                "/maven/releasesss/rubygemsss/a",
                "/maven/releases/rubygemsss/jbundler",
                "/maven/releases/rubygems/jbundler/1.2.3/jbundler-1.2.3.gema",
                "/maven/releases/rubygems/jbundler/1.2.3/jbundler-1.2.3.pom2",
                "/maven/releases/rubygems/jbundler/1.2.3/jbundler-1.2.3.gem.sha",
                "/maven/releases/rubygems/jbundler/1.2.3/jbundler-1.2.3.pom.msa",
                "/maven/prereleases/rubygemsss/jbundler",
                "/maven/prereleases/rubygems/jbundler/1.2.3-SNAPSHOT/maven-metadata.xml.sha1a",
                "/maven/prereleases/rubygems/jbundler/1.2.3-SNAPSHOT/jbundler-1.2.3-123213123.gem.sh1",
                "/maven/prereleases/rubygems/jbundler/1.2.3-SNAPSHOT/jbundler-1.2.3-123213123.pom.sha",
                "/maven/prereleases/rubygems/jbundler/1.2.3-SNAPSHOT/jbundler-1.2.3-123213123.gema",
                "/maven/prereleases/rubygems/jbundler/1.2.3-SNAPSHOT/jbundler-1.2.3-123213123.pom2",
        };
        assertFiletypeWithNullPayload(pathes, FileType.NOT_FOUND);
    }

    protected void assertFiletype(String[] pathes, FileType type) {
        for (String path : pathes) {
            RubygemsFile file = fileSystem.get(path);
            assertThat(path, file.type(), equalTo(type));
            assertThat(path, file.get(), notNullValue());
            assertThat(path, file.hasException(), is(false));
        }
    }

    protected void assertFiletypeWithPayload(String[] pathes, FileType type, String[] payloads) {
        int index = 0;
        for (String path : pathes) {
            RubygemsFile file = fileSystem.get(path);
            assertThat(path, file.type(), equalTo(type));
            assertThat(path, file.get(), is(instanceOf(BytesStreamLocation.class)));
            assertThat(path, file.hasException(), is(false));
            assertThat(path, readPayload(file).replaceAll("[0-9]{8}\\.?[0-9]{6}", "2014"), equalTo(payloads[index++]));
        }
        if (isHosted) {
            assertForbiddenPost(pathes);
        }
    }

    protected String readPayload(RubygemsFile file) {
        try {
            ByteArrayInputStream b = (ByteArrayInputStream) ((BytesStreamLocation) file.get()).openStream();
            byte[] bb = new byte[b.available()];
            b.read(bb);
            return new String(bb);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected RubygemsFile[] assertFiletypeWithPayload(String[] pathes, FileType type, Class<?> payload) {
        RubygemsFile[] result = new RubygemsFile[pathes.length];
        int index = 0;
        for (String path : pathes) {
            RubygemsFile file = fileSystem.get(path);
            assertThat(path, file.type(), equalTo(type));
            assertThat(path, file.get(), is(instanceOf(payload)));
            assertThat(path, file.hasException(), is(false));
            result[index++] = file;
        }
        if (type != FileType.GEM || !isHosted) {
            assertForbiddenPost(pathes);
        }
        return result;
    }

    protected void assertFiletypeWithNullPayload(String[] pathes, FileType type) {
        for (String path : pathes) {
            RubygemsFile file = fileSystem.get(path);
            assertThat(path, file.type(), equalTo(type));
            assertThat(path, file.get(), nullValue());
            assertThat(path, file.hasException(), is(false));
        }
        if (!isHosted && type != FileType.NOT_FOUND) {
            assertForbiddenPost(pathes);
        }
    }

    protected void assertNotFound(String[] pathes) {
        assertFiletypeWithNullPayload(pathes, FileType.NOT_FOUND);
    }

    protected void assertForbiddenPost(String[] pathes) {
        for (String path : pathes) {
            assertThat(path, fileSystem.post(null, path).forbidden(), is(true));
        }
    }

    protected void assertForbiddenGet(String[] pathes) {
        for (String path : pathes) {
            assertThat(path, fileSystem.get(path).forbidden(), is(true));
        }
    }

    protected void assertNotExists(String[] pathes) {
        for (String path : pathes) {
            assertThat(path, fileSystem.post(null, path).notExists(), is(true));
        }
    }
}

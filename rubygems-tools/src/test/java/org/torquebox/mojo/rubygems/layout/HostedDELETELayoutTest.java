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
import org.torquebox.mojo.rubygems.GemArtifactFile;
import org.torquebox.mojo.rubygems.RubyScriptingTestSupport;
import org.torquebox.mojo.rubygems.RubygemsFile;
import org.torquebox.mojo.rubygems.cuba.DefaultRubygemsFileSystem;
import org.torquebox.mojo.rubygems.layout.SimpleStorage.BytesStreamLocation;
import org.torquebox.mojo.rubygems.layout.SimpleStorage.URLGzipStreamLocation;
import org.torquebox.mojo.rubygems.layout.SimpleStorage.URLStreamLocation;

import java.io.ByteArrayInputStream;
import java.io.File;
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
public class HostedDELETELayoutTest
        extends RubyScriptingTestSupport {
    private final DefaultRubygemsFileSystem fileSystem;

    public HostedDELETELayoutTest(Storage store) throws IOException {
        fileSystem = new DefaultRubygemsFileSystem(
                new HostedGETLayout(rubygemsGateway(),
                        store),
                null,
                new HostedDELETELayout(rubygemsGateway(),
                        new SimpleStorage(hostedBase())));
        // delete proxy files
        proxyBase();
    }

    private static File proxyBase() throws IOException {
        File base = new File("target/hosted-delete-proxy");
        FileUtils.deleteDirectory(base);
        return base;
    }

    private static File hostedBase() throws IOException {
        File source = new File("src/test/hostedrepo");
        File base = new File("target/hosted-delete-repo");
        FileUtils.deleteDirectory(base);
        FileUtils.copyDirectory(source, base, true);
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
    public void deleteGems() {
        fileSystem.delete("/gems/zip-2.0.2.gem");
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
                "/maven/prereleases/rubygems/psych/5.1.0.pre1/psych-5.1.0.pre1-123213123.gem.sha1",
                "/maven/prereleases/rubygems/psych/5.1.0.pre1/psych-5.1.0.pre1-123213123.pom.sha1",
                "/maven/releases/rubygems/psych/5.1.0.pre1/psych-5.1.0.pre1.gem.sha1",
                "/maven/releases/rubygems/psych/5.1.0.pre1/psych-5.1.0.pre1.pom.sha1"
        };
        String[] shas = {
                "edfb9b1923514277b7256b0670a388da43706d39",
                "b30edd7dae6d79b36b03fd03a2f0af17da475b0d",
                "edfb9b1923514277b7256b0670a388da43706d39",
                "cf017cee279df5a255e49490bf0d5f67da260b5f"
        };

        assertFiletypeWithPayload(pathes, FileType.SHA1, shas);

        pathes = new String[]{
                "/maven/releases/rubygems/zip/9.9.9.9/zip-9.9.9.9.gem.sha1",
                "/maven/releases/rubygems/zip/9.9.9.9/zip-9.9.9.9.pom.sha1"
        };
        assertNotFound(pathes, FileType.SHA1);

        // these files carry a timestamp of creation of the .ruby file
        pathes = new String[]{
                "/maven/prereleases/rubygems/psych/maven-metadata.xml.sha1",
                "/maven/prereleases/rubygems/psych/5.1.0.pre1-SNAPSHOT/maven-metadata.xml.sha1",
                "/maven/releases/rubygems/psych/maven-metadata.xml.sha1"
        };
        assertFiletypeWithPayload(pathes, FileType.SHA1, BytesStreamLocation.class);
    }

    @Test
    public void testGemArtifact() throws Exception {
        String[] pathes = {
                "/maven/releases/rubygems/psych/5.1.0.pre1/psych-5.1.0.pre1.gem",
                "/maven/prereleases/rubygems/psych/5.1.0.pre1-SNAPSHOT/psych-5.1.0.pre1-123213123.gem"
        };
        assertFiletypeWithPayload(pathes, FileType.GEM_ARTIFACT, URLStreamLocation.class);

        pathes = new String[]{"/maven/releases/rubygems/zip/9.9.9.9/zip-9.9.9.9.gem"};
        assertNotFound(pathes, FileType.GEM_ARTIFACT);

        pathes = new String[]{
                "/maven/releases/rubygems/hufflepuf/0.1.0/hufflepuf-0.1.0.gem",
                "/maven/releases/rubygems/hufflepuf/0.2.0/hufflepuf-0.2.0.gem"
        };
        RubygemsFile[] result = assertFiletypeWithPayload(pathes, FileType.GEM_ARTIFACT, URLStreamLocation.class);
        for (RubygemsFile file : result) {
            GemArtifactFile a = (GemArtifactFile) file;
            assertThat(a.gem(null).filename(), is("hufflepuf-" + a.version() + "-universal-java-1.5"));
        }
    }

    @Test
    public void testPom() throws Exception {
        String[] pathes = {
                "/maven/releases/rubygems/jbundler/0.8.0.pre/jbundler-0.8.0.pre.pom",
                "/maven/prereleases/rubygems/jbundler/0.8.0.pre-SNAPSHOT/jbundler-0.8.0.pre-123213123.pom"
        };
        String[] xmls = {
                loadPomResource("jbundler-release.pom"),
                loadPomResource("jbundler-prerelease.pom")
        };
        assertFiletypeWithPayload(pathes, FileType.POM, xmls);
        pathes = new String[]{"/maven/releases/rubygems/zip/9.9.9.9/zip-9.9.9.9.pom"};
        assertNotFound(pathes, FileType.POM);
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
        String[] pathes = {"/api/v1/dependencies?gems=zip,psych"};
        assertFiletypeWithPayload(pathes, FileType.BUNDLER_API, BytesStreamLocation.class);
    }

    @Test
    public void testApiV1Gems() throws Exception {
        String[] pathes = {"/api/v1/gems"};
        assertForbidden(pathes);
    }

    @Test
    public void testApiV1ApiKey() throws Exception {
        String[] pathes = {"/api/v1/api_key"};
        assertFiletypeWithNullPayload(pathes, FileType.API_V1);
    }

    @Test
    public void testDependency() throws Exception {
        String[] pathes = {
                "/info/zip", "/info/psych.compact", "/info/psych.compact"
        };
        assertFiletypeWithPayload(pathes, FileType.COMPACT, URLStreamLocation.class);
    }

    @Test
    public void testGemspec() throws Exception {
        String[] pathes = {
                "/api/v2/rubygems/psych/versions/5.1.0.pre1.json", "/api/v2/rubygems/psych/versions/5.1.0.pre1.json"
        };
        assertFiletypeWithPayload(pathes, FileType.JSON_API, URLStreamLocation.class);
        pathes = new String[]{"/quick/Marshal.4.8/zip-2.0.2.gemspec.rz", "/quick/Marshal.4.8/z/zip-2.0.2.gemspec.rz"};
        assertNotFound(pathes, FileType.GEMSPEC);
    }

    @Test
    public void testGem() throws Exception {
        String[] pathes = {"/gems/psych-5.1.0.pre1-java.gem", "/gems/psych-5.1.0.pre1-java.gem"};
        assertFiletypeWithPayload(pathes, FileType.GEM, URLStreamLocation.class);
        pathes = new String[]{"/gems/zip-2.0.2.gem", "/gems/z/zip-2.0.2.gem"};
        assertNotFound(pathes, FileType.GEM);
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
        assertFiletypeWithNullPayload(pathes, FileType.NOT_FOUND, false);
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
        return result;
    }

    protected void assertFiletypeWithNullPayload(String[] pathes, FileType type) {
        assertFiletypeWithNullPayload(pathes, type, true);
    }

    protected void assertFiletypeWithNullPayload(String[] pathes, FileType type, boolean found) {
        for (String path : pathes) {
            RubygemsFile file = fileSystem.get(path);
            assertThat(path, file.type(), equalTo(type));
            assertThat(path, file.get(), nullValue());
            assertThat(path, file.hasNoPayload(), is(found));
            assertThat(path, file.hasException(), is(false));
            assertThat(path, file.exists(), is(found));
        }
    }

    protected void assertNotFound(String[] pathes, FileType type) {
        assertFiletypeWithNullPayload(pathes, type, false);
    }

    protected void assertForbidden(String[] pathes) {
        for (String path : pathes) {
            assertThat(path, fileSystem.get(path).forbidden(), is(true));
        }
    }
}

package org.torquebox.mojo.mavengem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jruby.embed.IsolatedScriptingContainer;
import org.jruby.embed.ScriptingContainer;
import org.torquebox.mojo.rubygems.DefaultRubygemsGateway;
import org.torquebox.mojo.rubygems.RubygemsGateway;
import org.torquebox.mojo.rubygems.FileType;
import org.torquebox.mojo.rubygems.GemArtifactFile;
import org.torquebox.mojo.rubygems.IOUtil;
import org.torquebox.mojo.rubygems.RubygemsFile;
import org.torquebox.mojo.rubygems.cuba.RubygemsFileSystem;
import org.torquebox.mojo.rubygems.layout.CachingProxyStorage;
import org.torquebox.mojo.rubygems.layout.ProxiedRubygemsFileSystem;
import org.torquebox.mojo.rubygems.layout.ProxyStorage;

public class Rubygems {

    private static RubygemsGateway gateway = new DefaultRubygemsGateway(new IsolatedScriptingContainer());

    private static Map<URL, Rubygems> facades = new HashMap<URL, Rubygems>();

    private final ProxyStorage storage;
    private final RubygemsFileSystem files;

    Rubygems(URL url, File baseCacheDir) {
	// we do not want to expose credentials inside the directory name
        File cachedir = new File(baseCacheDir, url.toString().replaceFirst("://[^:]+:[^:]+@", "://").replaceAll("[/:.]", "_"));
	this.storage = new CachingProxyStorage(cachedir, url);
        this.files = new ProxiedRubygemsFileSystem(gateway, storage);
    }

    public InputStream getInputStream(RubygemsFile file) throws IOException {
        return this.storage.getInputStream(file);
    }

    public RubygemsFile get(String path) {
        return this.files.get(path);
    }

    public long getModified(RubygemsFile file) {
	return this.storage.getModified(file);
    }
}


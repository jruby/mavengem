package org.torquebox.mojo.mavengem;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.NoSuchFileException;

import org.torquebox.mojo.rubygems.GemArtifactFile;
import org.torquebox.mojo.rubygems.RubygemsFile;

public class MavenGemURLConnection extends URLConnection {

    public static final String MAVEN_RELEASES = "/maven/releases";
    public static final String PING = MAVEN_RELEASES + "/ping";

    private static final RubygemsFactory NO_RUBYGEMS_FACTORY = null;

    private final Proxy proxy;

    private InputStream in;
    private long timestamp = -1;

    private int counter = 12; // seconds

    // package private for testing
    final URL baseurl;
    final String path;
    final RubygemsFactory factory;

    public static MavenGemURLConnection create(String uri) throws MalformedURLException {
        return create(NO_RUBYGEMS_FACTORY, uri, Proxy.NO_PROXY);
    }

    public static MavenGemURLConnection create(String uri, Proxy proxy) throws MalformedURLException {
        return create(NO_RUBYGEMS_FACTORY, uri, proxy);
    }

    public static MavenGemURLConnection create(RubygemsFactory factory, String uri)
        throws MalformedURLException {
        return create(factory, uri, Proxy.NO_PROXY);
    }

    public static MavenGemURLConnection create(RubygemsFactory factory, String uri, Proxy proxy)
	     throws MalformedURLException {
	int index = uri.indexOf(MAVEN_RELEASES);
        String path = uri.substring(index);
        String baseurl = uri.substring(0, index);
	return new MavenGemURLConnection(factory, new URL(baseurl), path, proxy);
    }

    public MavenGemURLConnection(URL baseurl, String path)
        throws MalformedURLException {
        this(NO_RUBYGEMS_FACTORY, baseurl, path, Proxy.NO_PROXY);
    }

    public MavenGemURLConnection(URL baseurl, String path, Proxy proxy)
        throws MalformedURLException {
        this(NO_RUBYGEMS_FACTORY, baseurl, path, proxy);
    }

    public MavenGemURLConnection(RubygemsFactory factory, URL baseurl, String path)
        throws MalformedURLException {
        this(factory, baseurl, path, Proxy.NO_PROXY);
    }

    public MavenGemURLConnection(RubygemsFactory factory, URL baseurl, String path, Proxy proxy)
	    throws MalformedURLException {
        super(baseurl);
        this.proxy = proxy == null ? Proxy.NO_PROXY : proxy;
	this.factory = factory == null ? RubygemsFactory.defaultFactory() : factory;
        this.baseurl = baseurl;
        this.path = path.startsWith(MAVEN_RELEASES) ? path : MAVEN_RELEASES + path;
    }

    @Override
    synchronized public InputStream getInputStream() throws IOException {
        if (in == null) {
            connect();
        }
        return in;
    }

    @Override
    synchronized public long getLastModified() {
        if (timestamp == -1) {
            try {
                connect();
            }
            catch (IOException e) {
                // ignore
            }
        }
        return timestamp;
    }

    @Override
    synchronized public void connect() throws IOException {
        connect(factory.getOrCreate(baseurl));
    }

    private void connect(Rubygems facade) throws IOException {
        RubygemsFile file = facade.get(path);
        switch( file.state() )
        {
        case FORBIDDEN:
            throw new IOException("forbidden: " + file + " on " + baseurl);
        case NOT_EXISTS:
            if (path.equals(PING)) {
                in = new ByteArrayInputStream("pong".getBytes());
                break;
            }
	    throw new FileNotFoundException(file.toString() + " on " + baseurl);
        case NO_PAYLOAD:
            switch( file.type() )
            {
            case GEM_ARTIFACT:
                // we can pass in null as dependenciesData since we have already the gem
                in = openInputStream(baseurl + "/gems/" + ((GemArtifactFile) file ).gem( null ).filename() + ".gem");
            case GEM:
		// TODO timestamp
                in = openInputStream(baseurl + "/" +  file.remotePath());
            default:
                throw new FileNotFoundException("view - not implemented. " + file + " on " + baseurl);
            }
        case ERROR:
	    if (file.getException() instanceof NoSuchFileException) {
		throw new FileNotFoundException(file.toString() + " on " + baseurl);
	    }
	    throw new IOException(file.toString() + " on " + baseurl, file.getException());
        case TEMP_UNAVAILABLE:
            try {
                Thread.sleep(1000);
            }
            catch(InterruptedException ignore) {
            }
            if (--counter > 0) {
                connect(facade);
            }
            break;
        case PAYLOAD:
            in = facade.getInputStream(file);
	    timestamp = facade.getModified(file);
            break;
        case NEW_INSTANCE:
        default:
            throw new RuntimeException("BUG: should never reach here. " + file + " on " + baseurl);
        }
    }

    private InputStream openInputStream(final String uri) throws IOException {
        if (proxy == Proxy.NO_PROXY) {
            return new URL(uri).openStream();
        }
        System.err.println(proxy);
        return new URL(uri).openConnection(proxy).getInputStream();
    }
}


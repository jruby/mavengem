package org.torquebox.mojo.rubygems;

public interface RubygemsV2GemInfo {
    public String version();
    public String platform();
    public String name();
    public long modified();
}

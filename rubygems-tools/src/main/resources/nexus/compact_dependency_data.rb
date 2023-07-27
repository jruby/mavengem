require 'nexus/rubygems_helper'

java_import org.torquebox.mojo.rubygems.DependencyData

# Dependency data based on the compact index format
module Nexus
  class CompactDependencyData
    include DependencyData
    include RubygemsHelper
    
    attr_reader :name, :modified

    # contructor parsing dependency data
    # @param data [Array, IO, String] either the unmarshalled array or 
    #        the stream or filename to the marshalled data
    def initialize( data, name, modified )
      data = read_binary(data)

      @name = name
      @modified = modified
      @versions = {}

      # Format is as follows:
      # First line: "---"
      # [version-platform] [space] [comma-delimited-deps] [pipe] [checksum] [comma] [min_ruby] [comma] [min_rubygems] [newline]
      # The version-platform is [version] [dash] [platform].
      # The deps and mins are specified as [gemname] [colon] [comparator] [space] [version].
      data.lines.filter_map do |line|
        line.match(/^(?<version>\S+?)(?:-(?<platform>\S+))? (.*)\|checksum:(?<checksum>[a-f0-9]+)(?:,(?<reqs>.*))?$/)&.named_captures
      end.sort do |match1, match2|
        Gem::Version.new(match1['version']) <=> Gem::Version.new(match2['version'])
      end.each do |match|
        platform = match['platform']
        version = match['version']
        if platform.nil?
          @versions[version] ||= match
          match['platform'] = "ruby"
        elsif platform.downcase =~ /(java|jruby)/
          # java overwrites since it has higher prio
          @versions[version] = match
        end
      end
    end
    
    # returns all the version
    # @param prereleases [boolean] whether or not to include prereleased
    #         versions
    # @return [Array] an array of versions
    def versions(prereleases)
      if prereleases
        @versions.keys.select {|v| v =~ /[a-zA-Z]/}
      else
        @versions.keys.select {|v| !(v =~ /[a-zA-Z]/)}
      end
    end

    # retrieve the platform for a given version
    # @param version [String] the version to query
    # @return [String] the platform
    def platform(version)
      @versions[version]&.[]('platform')
    end
  end
end

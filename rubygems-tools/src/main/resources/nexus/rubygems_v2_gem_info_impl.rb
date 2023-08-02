require 'nexus/rubygems_helper'

java_import org.torquebox.mojo.rubygems.RubygemsV2GemInfo

# parsed dependency data and restructure to retrieve the platform for
# given version or give the list of all version.
#
# @author Christian Meier
module Nexus
  class RubygemsV2GemInfoImpl
    include RubygemsV2GemInfo
    include RubygemsHelper

    attr_reader :name, :version, :modified

    def initialize(data, name, version, modified)
      @name, @version, @modified = name, version, modified

      @data = json_load(data)
    end

    def platform
      @data["platform"]
    end
  end
end

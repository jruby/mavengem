#
# Sonatype Nexus (TM) Open Source Version
# Copyright (c) 2008-present Sonatype, Inc.
# All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
#
# This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
# which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
#
# Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
# of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
# Eclipse Foundation. All other trademarks are the property of their respective owners.
#

require 'nexus/rubygems_helper'
require 'maven/tools/pom'
# we do not need openssl, so let rubygems find the dummy
$LOAD_PATH.insert(0, File.expand_path(File.dirname(__FILE__)))
require 'rubygems/package'

java_import org.torquebox.mojo.rubygems.GemspecHelper

# wrapper around a Gem::Specification object with some extra
# helper methods and only delegate a few needed methods to
# the underlying object.
#
# @author Christian Meier

# workaround https://github.com/jruby/jruby/issues/2035 until
# the next version of maven-tools gem is released
if Encoding.default_external.to_s != 'UTF-8'
  warn "set default encoding to UTF-8 for current runtime"
  Encoding.default_external = 'utf-8'
end
module Nexus
  class GemspecHelperImpl
    include GemspecHelper
    include RubygemsHelper

    # getter for the wrapped Gem::Specification object
    attr_reader :gemspec

    GEM_TYPE = :gem
    GEMSPEC_TYPE = :gemspec
    RUBYGEMS_V2_INFO = :rubygems_v2_info

    # contructor for GemspecHelperImpl
    # @param io [IO, String] stream or filename
    # @param data_type [boolean] whether io belongs to gemspec.rz file
    #        or gem file
    def initialize( io, data_type )
      case data_type
      when GEM_TYPE
        @gemspec = load_spec( io )
      when GEMSPEC_TYPE
        @gemspec = runzip( io )
      when RUBYGEMS_V2_INFO
        @gemspec = rubygems_v2_info_gemspec( io )
      end
    end

    # extract the gemspec from the given gem file
    # @param gem [IO,String] stream or filename
    # @return [GemspecHelperImpl] wrapper around the gemspec
    def self.from_gem( gem )
      new( gem, GEM_TYPE )
    end

    # extract the gemspec from the given gemspec.rz file
    # @param gem [IO,String] stream or filename
    # @return [GemspecHelperImpl] wrapper around the gemspec
    def self.from_gemspec_rz( gemspec_rz )
      new( gemspec_rz, GEMSPEC_TYPE )
    end

    def self.from_rubygems_v2_gem_info( rg_v2_info )
      new(rg_v2_info, RUBYGEMS_V2_INFO)
    end

    # filename of the associated gem file
    # @return [String] filename
    def filename
      @gemspec.file_name
    end

    # name of the associated gem file
    # @return [String] name
    def name
      @gemspec.name
    end

    # generates the pom XML from the gemspec
    # @param snapshot [boolean] whether or not to use a snapshot version.
    #                           snapshot versions only works with prereleased
    #                           gem version
    # @return [String] pom XML
    def pom( snapshot )
      proj = Maven::Tools::POM.new( @gemspec, snapshot )
      proj.to_s
    end

    # the binary content for a gemspec.rz file from the
    # underlying Gem::Specification object.
    # @return [IO] stream to the content
    def rz_input_stream
      rzip( @gemspec )
    end
    alias :get_rz_input_stream :rz_input_stream

    # string representation of Gem::Specification object
    # @return [String]
    def to_s
      @gemspec.inspect
    end

    private

    def load_spec( gemfile )
      case gemfile
      when String
        Gem::Package.new( gemfile ).spec
      else
        io = StringIO.new( read_binary( gemfile ) )
        # this part if basically copied from rubygems/package.rb
        Gem::Package::TarReader.new( io ) do |reader|
          reader.each do |entry|
            case entry.full_name
            when 'metadata' then
              return Gem::Specification.from_yaml entry.read
            when 'metadata.gz' then
              args = [entry]
              args << { :external_encoding => Encoding::UTF_8 } if
                Object.const_defined?(:Encoding) &&
                Zlib::GzipReader.method(:wrap).arity != 1

              Zlib::GzipReader.wrap(*args) do |gzio|
                return Gem::Specification.from_yaml gzio.read
              end
            end
          end
        end
        raise "failed to load spec from #{gemfile}"
      end
    end

    def rubygems_v2_info_gemspec( io )
      info = json_load(io)
      Gem::Specification.new(info["name"], info["version"]) do |spec|
        spec.summary = info["summary"]
        spec.homepage = info["homepage_uri"]
        spec.description = info["description"]
        spec.licenses = info["licenses"]
        spec.authors = info["authors"].split(', ')
        # v2 API does not appear to provide emails
        # spec.emails = info["emails"]
        spec.metadata["source_code_uri"] = info["source_code_uri"]
        info["dependencies"]["runtime"].each do |dep|
          spec.add_dependency(dep["name"], *dep["requirements"].split(", "))
        end
      end
    end
  end
end

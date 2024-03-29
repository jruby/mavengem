require File.expand_path("../../path.rb", __FILE__)
require File.expand_path("../../../../lib/bundler/deprecate", __FILE__)
include Spec::Path

# Set up pretend http gem server with FakeWeb
$LOAD_PATH.unshift "#{Dir[base_system_gems.join("gems/artifice*/lib")].first}"
$LOAD_PATH.unshift "#{Dir[base_system_gems.join("gems/rack-*/lib")].first}"
$LOAD_PATH.unshift "#{Dir[base_system_gems.join("gems/rack-*/lib")].last}"
$LOAD_PATH.unshift "#{Dir[base_system_gems.join("gems/tilt*/lib")].first}"
$LOAD_PATH.unshift "#{Dir[base_system_gems.join("gems/sinatra*/lib")].first}"
require 'artifice'
require 'sinatra/base'

class Endpoint < Sinatra::Base

  helpers do
    def dependencies_for(gem_names, marshal = gem_repo1("specs.4.8"))
      return [] if gem_names.nil? || gem_names.empty?

      require 'rubygems'
      require 'bundler'
      Bundler::Deprecate.skip_during do
        Marshal.load(File.open(marshal).read).map do |name, version, platform|
          spec = load_spec(name, version, platform)
          if gem_names.include?(spec.name)
            {
              :name         => spec.name,
              :number       => spec.version.version,
              :platform     => spec.platform.to_s,
              :dependencies => spec.dependencies.select {|dep| dep.type == :runtime }.map do |dep|
                [dep.name, dep.requirement.requirements.map {|a| a.join(" ") }.join(", ")]
              end
            }
          end
        end.compact
      end
    end

    def load_spec(name, version, platform)
      full_name = "#{name}-#{version}"
      full_name += "-#{platform}" if platform != "ruby"
      Marshal.load(Zlib::Inflate.inflate(File.open(gem_repo1("quick/Marshal.4.8/#{full_name}.gemspec.rz")).read))
    end
  end

  get "/quick/Marshal.4.8/:id" do
    redirect "/fetch/actual/gem/#{params[:id]}"
  end

  get "/fetch/actual/gem/:id" do
    File.read("#{gem_repo1}/quick/Marshal.4.8/#{params[:id]}")
  end

  get "/gems/:id" do
    File.read("#{gem_repo1}/gems/#{params[:id]}")
  end

  get "/api/v1/dependencies" do
    Marshal.dump(dependencies_for(params[:gems]))
  end

  get "/specs.4.8.gz" do
    File.read("#{gem_repo1}/specs.4.8.gz")
  end

  get "/prerelease_specs.4.8.gz" do
    File.read("#{gem_repo1}/prerelease_specs.4.8.gz")
  end
end

Artifice.activate_with(Endpoint)

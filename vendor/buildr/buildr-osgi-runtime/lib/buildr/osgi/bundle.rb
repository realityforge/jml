module Buildr
  module OSGi
    class Bundle
      MAX_RUN_LEVEL = 120
      DEFAULT_RUN_LEVEL = 70

      attr_reader :artifact_spec
      attr_reader :run_level

      def initialize(artifact_spec, run_level)
        @artifact_spec, @run_level = artifact_spec, run_level
        @artifact_specs = {}
      end

      def artifact
        Buildr.artifacts([artifact_spec]).last
      end

      # The path to install bundle to relative to the base of the bundle dir
      def relative_install_path
        a = artifact
        name = File.basename(a.to_s)
        a.respond_to?(:group) ? "#{a.group.gsub('.', '/')}/#{name}" : "#{name}"
      end
    end
  end
end


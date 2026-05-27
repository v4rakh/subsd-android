{
  description = "subsd Android build environment";

  inputs.nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable";
  inputs.systems.url = "github:nix-systems/default";
  inputs.flake-utils = {
    url = "github:numtide/flake-utils";
    inputs.systems.follows = "systems";
  };

  outputs =
    { nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            android_sdk.accept_license = true;
            allowUnfree = true;
          };
        };

        androidComposition = pkgs.androidenv.composeAndroidPackages {
          platformVersions = [ "36" ];
          buildToolsVersions = [ "37.0.0" ];
          includeNDK = false;
          includeSystemImages = false;
        };

        androidSdk = androidComposition.androidsdk;

        androidEnvVars = ''
          export ANDROID_SDK_ROOT="${androidSdk}/libexec/android-sdk"
          export ANDROID_HOME="${androidSdk}/libexec/android-sdk"
          export JAVA_HOME="${pkgs.jdk17}"
          export GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx2g"
        '';

        # Android SDK tooling expects a standard FHS filesystem layout,
        # which NixOS does not provide by default.
        fhs = pkgs.buildFHSEnv {
          name = "android-fhs";
          targetPkgs = _: [
            androidSdk
            pkgs.android-studio-full
            pkgs.androidenv.androidPkgs.androidsdk
            pkgs.git
            pkgs.jdk17
            pkgs.fastlane
            pkgs.ruby
          ];
          profile = androidEnvVars;
        };

        # Stripped-down FHS environment without Android Studio, suitable for CI / fastlane builds.
        # buildFHSEnv provides a proper dynamic linker path so downloaded protoc/grpc plugin
        # binaries (generic Linux ELFs) work without requiring nix-ld on the host.
        # Usage:
        #   interactive : nix develop .#ci
        #   non-interactive: nix run .#ci-shell -- fastlane build
        ciFhs = pkgs.buildFHSEnv {
          name = "android-ci-fhs";
          targetPkgs = _: [
            androidSdk
            pkgs.git
            pkgs.jdk17
            pkgs.fastlane
            pkgs.ruby
          ];
          profile = androidEnvVars;
          # Pass-through args so non-interactive invocations work:
          #   nix run .#ci-shell -- <cmd>  →  exec <cmd> inside FHS
          runScript = pkgs.writeShellScript "ci-entrypoint" ''
            if [ $# -gt 0 ]; then
              exec "$@"
            else
              exec bash --login
            fi
          '';
        };
      in
      {
        devShells.default = fhs.env;
        devShells.ci = ciFhs.env; # interactive: nix develop .#ci
        packages.ci-shell = ciFhs; # non-interactive: nix run .#ci-shell -- <cmd>
      }
    );
}

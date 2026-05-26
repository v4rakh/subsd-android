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
          # compileSdk = 36, targetSdk = 35
          platformVersions = [
            "36"
            "35"
          ];
          buildToolsVersions = [ "37.0.0" ];
          # ABIs to include in the SDK (for device deployment; not needed for compilation)
          abiVersions = [
            "arm64-v8a"
            "x86_64"
          ];
          includeNDK = false;
          includeSystemImages = false;
        };

        androidSdk = androidComposition.androidsdk;

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
          profile = ''
            export ANDROID_SDK_ROOT="${androidSdk}/libexec/android-sdk"
            export ANDROID_HOME="${androidSdk}/libexec/android-sdk"
            export JAVA_HOME="${pkgs.jdk17}"
            export GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx4g"
          '';
        };
      in
      {
        devShells.default = fhs.env;
      }
    );
}
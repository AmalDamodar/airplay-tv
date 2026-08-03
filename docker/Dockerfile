FROM ghcr.io/cirruslabs/android-sdk@sha256:f9b3ea9ed2b5fc9522adae82c7b4622ab7aa54207ef532c8e615a347dca08f31

RUN yes | sdkmanager --licenses >/dev/null \
    && sdkmanager "ndk;27.0.12077973" "cmake;3.22.1"

RUN sdkmanager "build-tools;35.0.0"

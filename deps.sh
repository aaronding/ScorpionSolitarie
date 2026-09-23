# Third-party libraries, sourced by build.sh and test.sh. Each is downloaded
# once from Maven Central into lib/ and checked against a known SHA-256.

MAVEN=https://repo1.maven.org/maven2

# fetch <path under Maven Central> <sha256>
fetch() {
    jar="lib/$(basename "$1")"
    if [ ! -f "$jar" ] || ! echo "$2  $jar" | shasum -a 256 -c - >/dev/null 2>&1; then
        mkdir -p lib
        curl -sSfL -o "$jar" "$MAVEN/$1"
        if ! echo "$2  $jar" | shasum -a 256 -c - >/dev/null 2>&1; then
            echo "Checksum mismatch for $jar" >&2
            rm -f "$jar"
            exit 1
        fi
    fi
}

# Bundled into Scorpion.jar
fetch com/formdev/flatlaf/3.7.2/flatlaf-3.7.2.jar \
      917aff3963c88d797d0fd9b9ccbd70f7681c101df9d11c59e2bc7a3a6c0fabf4
fetch com/github/weisj/jsvg/2.2.0/jsvg-2.2.0.jar \
      abc17a201f9cba7f690f1f68e9c0e9dacf225a8727922226b2b480c0d11c99e9
APP_LIBS="lib/flatlaf-3.7.2.jar lib/jsvg-2.2.0.jar"
APP_CLASSPATH="lib/flatlaf-3.7.2.jar:lib/jsvg-2.2.0.jar"

# Tests only
JUNIT=lib/junit-platform-console-standalone-6.1.3.jar
fetch_junit() {
    fetch org/junit/platform/junit-platform-console-standalone/6.1.3/junit-platform-console-standalone-6.1.3.jar \
          e62b96ac475dbcde8599ea905d088f65d90778f86e259b856a49fa5c4ea256ec
}

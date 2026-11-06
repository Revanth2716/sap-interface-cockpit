#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# SAP Interface Cockpit - convenience runner (Linux / macOS)
#
#   ./run.sh            build, test and start the cockpit
#   ./run.sh test       run the test suite only
#   ./run.sh package    build without starting
#   ./run.sh clean      remove build output
# ---------------------------------------------------------------------------
set -e
cd "$(dirname "$0")"

case "${1:-run}" in
  test)
    mvn -B test
    ;;
  package)
    mvn -B clean package
    ;;
  clean)
    mvn -B clean
    ;;
  *)
    echo ""
    echo "Building the SAP Interface Cockpit..."
    mvn -B clean package
    echo ""
    echo "Starting on http://localhost:8082"
    echo "Sign in with ops.lead / demo1234 for the full view."
    echo ""
    java -jar target/sap-interface-cockpit.jar
    ;;
esac

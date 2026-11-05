#!/data/data/com.termux/files/usr/bin/sh
set -eu
cd "$(dirname "$0")"
if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle non trovato. Apri questo progetto con Android Studio oppure installa una toolchain Android funzionante e riprova." >&2
  exit 127
fi
gradle --no-daemon :app:assembleDebug
echo "APK pronto: app/build/outputs/apk/debug/app-debug.apk"

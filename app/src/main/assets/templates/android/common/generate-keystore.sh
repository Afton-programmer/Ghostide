#!/bin/sh
# Ghostide Release Keystore Generator
echo "Generating production release keystore (release.keystore)..."
keytool -genkey -v -keystore release.keystore -alias androidreleasekey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Android App, OU=Mobile, O=Ghostide, L=City, S=State, C=US"
if [ $? -eq 0 ]; then
  echo "✅ Release keystore created successfully at release.keystore"
  echo "Alias: androidreleasekey | StorePass: android | KeyPass: android"
else
  echo "❌ Failed to generate keystore. Ensure keytool is installed."
fi

# Install Guide (MVP Scaffold)

1. Build debug APK: `./gradlew :app:assembleDebug`
2. Sideload on Android API 28+ device.
3. On first launch, grant Health Connect permissions.
4. Run `ohc pair qr` on the host and scan the QR in the Android app.
5. Pick the shared export folder from the Android app.
   For Nextcloud/WebDAV, this should be the client-managed synced folder that also lands on the host machine.
6. If needed, switch transport mode between Syncthing, Nextcloud/WebDAV, and Tailscale before exporting.
7. Use `Export Now` for the first smoke test; background sync runs daily when device constraints are met.

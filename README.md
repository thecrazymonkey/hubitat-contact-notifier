# Contact & Lock Notifier (Hubitat)

Checks contact sensors and locks at a fixed time on chosen weekdays and sends ONE message listing every device not in the expected state. Silent when everything is fine.

## Install
**Bundle (easiest):** run `gradle bundle` (output: `build/distributions/ContactNotifier.zip`), then Hubitat → **Developer tools → Bundle Manager → Import ZIP** (it adds the parent and profile apps; the manifest lists the parent first). Needs a local Gradle 9 + Java 17 (no wrapper). Then **Apps → Add User App → Contact Notifier**, then **Add profile**. The bundle's file layout (`install.txt`/`update.txt` + `namespace.Name.groovy`) was copied from a published community bundle, since Hubitat's docs don't describe it, so confirm the first import on your hub.

**Manual:**
1. Hubitat → **Apps Code → New App** → paste `apps/ContactNotifierParent.groovy` → Save.
2. **Apps Code → New App** → paste `apps/ContactNotifierProfile.groovy` → Save.
3. **Apps → Add User App → Contact Notifier**, then **Add profile**.

## Profile settings
Devices and expected state, time + weekdays, message, then targets (notification devices, TTS devices, music players), optional volume (restored after N seconds), optional "no activity for N hours" check.
Message placeholders: `{devices}` `{contacts}` `{locks}` `{count}` `{time}`. Devices not reporting a clean state appear as e.g. `Back lock (unknown)`. `unlocked with timeout` counts as `unlocked`.

A speaker picked as both TTS and music player is announced twice (`speak` and `playText`) on purpose, so you can compare them.
Known limitation: two profiles announcing on the same speaker within the restore delay may leave it at the announcement volume.

## Tests (off-hub)
- `tests/run_all.sh` - fast logic tests. Run them with Groovy 2.4, the version Hubitat uses ([developer overview](https://docs2.hubitat.com/developer/overview)); verified on 2.4.21. They use stubs, so they do not prove the app works on a hub.
- `gradle test` - Hubitat CI (`hubitat_ci` 0.17): validates both apps against Hubitat's Groovy sandbox rules and checks that `initialize()` calls the Hubitat scheduling/label APIs correctly. Needs **Java 17** (verified passing under Temurin 17; Java 25 will not work with this 2021 library); the JVM flags in `build.gradle` open JDK internals it needs.
- Known gap: `hubitat_ci` 0.17 does not know the `button` input type, so the spec validates the profile with that one input swapped for a `bool`.

## Verify on your hub
- [ ] Open a door and unlock a lock, press **Run now**: ONE message naming both with states.
- [ ] Close/lock everything, **Run now**: nothing sent.
- [ ] Notification, TTS and music targets each receive the message when selected.
- [ ] Volume is set for the announcement and restored afterwards.
- [ ] Set a time 2 minutes ahead on today's weekday: fires once. Edit the time: the old one no longer fires.
- [ ] Two profiles with different times/states/targets run independently.

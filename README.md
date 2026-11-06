# Contact & Lock Notifier (Hubitat)

Checks contact sensors and locks at a fixed time on chosen weekdays and sends ONE message listing every device not in the expected state. Silent when everything is fine.

## Install
1. Hubitat → **Apps Code → New App** → paste `apps/ContactNotifierParent.groovy` → Save.
2. **Apps Code → New App** → paste `apps/ContactNotifierProfile.groovy` → Save.
3. **Apps → Add User App → Contact Notifier**, then **Add profile**.

## Profile settings
Devices and expected state, time + weekdays, message, then targets (notification devices, TTS devices, music players), optional volume (restored after N seconds), optional "no activity for N hours" check.
Message placeholders: `{devices}` `{contacts}` `{locks}` `{count}` `{time}`. Devices not reporting a clean state appear as e.g. `Back lock (unknown)`.

## Tests (off-hub)
- `tests/run_all.sh` - fast logic tests (needs Groovy 4). They use stubs, so they do not prove the app works on a hub.
- `gradle test` - Hubitat CI (`hubitat_ci` 0.17): validates both apps against Hubitat's Groovy sandbox rules and checks that `initialize()` calls the Hubitat scheduling/label APIs correctly. Needs **Java 17** (Java 25 will not work with this 2021 library); the JVM flags in `build.gradle` open JDK internals it needs.
- Known gap: `hubitat_ci` 0.17 does not know the `button` input type, so the spec validates the profile with that one input swapped for a `bool`.

## Verify on your hub
- [ ] Open a door and unlock a lock, press **Run now**: ONE message naming both with states.
- [ ] Close/lock everything, **Run now**: nothing sent.
- [ ] Notification, TTS and music targets each receive the message when selected.
- [ ] Volume is set for the announcement and restored afterwards.
- [ ] Set a time 2 minutes ahead on today's weekday: fires once. Edit the time: the old one no longer fires.
- [ ] Two profiles with different times/states/targets run independently.

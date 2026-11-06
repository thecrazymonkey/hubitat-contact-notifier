# Contact & Lock Notifier — Design

## Intent
Rule Machine can check doors and locks on a schedule, but it needs one IF per device to name the offender and cannot put a list of devices into one notification. This app replaces that rule: at a scheduled time it checks chosen contact sensors and locks against an expected state and sends ONE notification naming every device that is not in that state.

## Goals
- Pick contact sensors and locks from lists (no per-device rules).
- Group devices into **profiles**; each profile has its own schedule, expected states, message and delivery targets.
- One notification per profile run, listing all offending devices in a single sentence.
- Deliver via any combination of: notification device, TTS device, music player.
- Report devices with unclean state (unknown/jammed/stale) as their own item so silent failures never look like "all secure".

## Non-goals
- Sunrise/sunset schedules, repeating intervals, event-driven (instant) alerts.
- "All clear" messages (silent when nothing matches, by design).
- Acting on devices (locking/closing). Check and notify only.
- Global defaults in the parent app (the parent only manages profiles).

## Anti-goals
- Many notifications per run.
- A broken/offline device being read as "secure".

## Structure
- **Parent app** `Contact Notifier`: lists/creates/deletes profiles.
- **Child app** `Contact Notifier Profile`: one per profile, holds all settings below.
- Groovy, namespace `ivank`. Installed through a Hubitat Bundle (`gradle bundle` builds the ZIP) or by pasting the two files into Apps Code.

## Profile settings
| Setting | Detail |
|---|---|
| Name | Free text; also used as the app label |
| Contact sensors | `capability.contactSensor`, multiple |
| Locks | `capability.lock`, multiple |
| Contact alert state | Alert when contacts are: `open` or `closed` |
| Lock alert state | Alert when locks are: `locked` or `unlocked` |
| Schedule | One fixed time of day + days-of-week |
| Message template | Placeholders below; default `Not secure: {devices}` |
| Notification devices | `capability.notification`, multiple |
| Speakers | `capability.speechSynthesis` (`speak`) and `capability.musicPlayer` (`playText`), multiple. A speaker in both lists is announced by both methods, so the two can be compared |
| Speaker volume | Optional; set before announcing, previous level restored after |
| Restore delay | Seconds before the previous volume is restored (default 15) |
| Stale check | Optional "no activity for N hours" (1–8760), off by default |
| Info logging | On by default |
| Run now | Button; runs the same code path as the schedule |

A profile may mix contacts and locks. "Alert when" means: a device whose current state equals the chosen value is reported.

## Message
Default: one combined sentence. Placeholders:
- `{devices}` — all offenders, each with its own state, e.g. `Front door (open), Garage lock (unlocked)`
- `{contacts}` — offending contacts only; `{locks}` — offending locks only (enables "Unlocked: {locks}. Open: {contacts}.")
- `{count}`, `{time}`

Offenders with no clean state are included as e.g. `Back lock (unknown)`. Unknown placeholders are left as written.

## Run behaviour
1. At the scheduled time on a ticked day, read each selected device's current state.
2. A device is reported when its state equals the alert value, or when it has no clean state: a value other than the two valid ones for its type (e.g. `unknown`, `jammed`, null) is reported with that state, or as `unknown` when null. A lock reporting `unlocked with timeout` is treated as `unlocked`.
3. No offenders → nothing is sent (logged only).
4. Otherwise build ONE message and send it to every selected notification device, TTS device and music player. If offenders exist but no target is selected, a warning is logged instead.

## Design decisions and known limitations
1. **Stale sensors:** a sensor that keeps showing an old value (e.g. dead battery) cannot be detected from its state alone. Null, unknown and unrecognised values are always reported. The optional stale check additionally reports a device as `stale` when its `lastActivity` is older than N hours or missing. It is off by default because contact sensors that are legitimately idle for days make it noisy.
2. **Volume attribute** differs by driver (`level` on musicPlayer, `volume` on AudioVolume). Both are handled, and the volume step is skipped for a speaker with neither.
3. **Volume restore** relies on a timer, because Hubitat gives no reliable "speech finished" signal. Saved volumes carry a timestamp. The first saved value wins when announcements overlap. An entry older than the restore delay + 60 s is treated as expired (e.g. the hub restarted before the restore ran), and the speaker's current volume is saved instead, so a stale value is never restored.
4. **Per-profile volume state:** two profiles announcing on the same speaker within the restore delay can leave it at the announcement volume, because each profile only knows its own saved level.
5. **Scheduling:** only the check job is unscheduled when a profile is saved, so a pending volume restore is not cancelled.
6. **Logging:** the info toggle covers the run summary. Warnings (nothing scheduled, no devices, no targets, failed device calls) are always logged.

## Acceptance criteria
Verified off-hub by the test suites; the hub-only items are tracked in the README checklist.
- [ ] Can create, edit and delete several profiles from the parent app.
- [ ] Device pickers list contact sensors and locks; a profile can contain both.
- [ ] Profile fires at its fixed time only on ticked days.
- [ ] With one door open and one lock unlocked, exactly ONE notification is sent naming both with their states.
- [ ] With nothing offending, nothing is sent.
- [ ] A lock reporting `unknown` is named in the message.
- [ ] Message placeholders `{devices} {contacts} {locks} {count} {time}` all substitute correctly.
- [ ] Notification, TTS and music-player targets each receive the message when selected.
- [ ] Speaker volume is set for the announcement and restored afterwards.
- [ ] "Run now" triggers a check using the same code path as the schedule.
- [ ] Profiles run independently with different schedules, states, messages and targets.

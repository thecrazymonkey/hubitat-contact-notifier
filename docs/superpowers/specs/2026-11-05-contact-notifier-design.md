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
- "All clear" messages (silent when nothing matches — explicit user decision).
- Acting on devices (locking/closing). Check and notify only.

## Anti-goals
- Many notifications per run (user explicitly wants few).
- A broken/offline device being read as "secure".

## Structure (builder's call; Hubitat idiom)
- **Parent app** `Contact Notifier`: lists/creates/deletes profiles; optional global defaults.
- **Child app** `Contact Notifier Profile`: one per profile, holds all settings below.
- Groovy, single repo, installable via manual code import (Hubitat Package Manager manifest optional).

## Profile settings
| Setting | Detail |
|---|---|
| Name | Free text |
| Contact sensors | `capability.contactSensor`, multiple |
| Locks | `capability.lock`, multiple |
| Contact expected state | Alert when contacts are: `open` or `closed` |
| Lock expected state | Alert when locks are: `locked` or `unlocked` |
| Schedule | One fixed time of day + days-of-week checkboxes |
| Message template | Placeholders below |
| Notification devices | `capability.notification`, multiple |
| Speakers | `capability.speechSynthesis` (speak) and `capability.musicPlayer` (playText), multiple |
| Speaker volume | Optional; set before announcing, restore previous level after |

A profile may mix contacts and locks. "Alert when" means: a device whose current state equals the chosen value is reported.

## Message
Default: one combined sentence. Placeholders:
- `{devices}` — all offenders, each with its own state, e.g. `Front door (open), Garage lock (unlocked)`
- `{contacts}` — offending contacts only; `{locks}` — offending locks only (enables "Unlocked: {locks}. Open: {contacts}.")
- `{count}`, `{time}`
Offenders with no clean state are included as e.g. `Back lock (unknown)`.

## Run behaviour
1. At the scheduled time on a ticked day, read each selected device's current state.
2. Offender = state equals the alert value, OR state is not one of the two valid values for its type (e.g. `unknown`, null) — see assumption below. A lock reporting `unlocked with timeout` is treated as `unlocked` (user decision, 2026-11-05).
3. No offenders → do nothing (log only).
4. Otherwise build ONE message, send to every selected notification device, TTS device and music player.

## Assumptions / calls I made (please check)
1. **Stale sensors:** user asked that dead-battery sensors not look secure. A sensor still showing a stale value is not detectable from state alone. My call: v1 treats null/unknown/unrecognised values as offenders and adds an optional per-profile "stale after N hours (no activity)" check, off by default, using `lastActivity`; when the check is on, a device with no `lastActivity` at all is also reported as stale. Contact sensors that are legitimately idle for days make this noisy, hence off by default.
2. Volume attribute differs by driver (`level` on musicPlayer, `volume` on AudioVolume); builder must handle both and skip restore if neither exists.
3. A "Run now" button on each profile for testing is included (low cost, high value).
4. Info logging toggle per profile (on by default).
5. Volume restore is per profile. Two profiles announcing on the same speaker within the restore delay can leave it at the announcement volume (each profile only knows its own saved level). Known limitation.
6. Each saved volume carries a timestamp. If the restore timer is lost (e.g. hub reboot during the delay), an entry older than the restore delay + 60 s is ignored and the speaker's current volume is saved instead, so a stale value is never restored.

## Acceptance criteria (definition of done)
- [ ] Can create, edit and delete several profiles from the parent app.
- [ ] Device pickers list contact sensors and locks; a profile can contain both.
- [ ] Profile fires at its fixed time only on ticked days.
- [ ] With one door open and one lock unlocked, exactly ONE notification is sent naming both with their states.
- [ ] With nothing offending, nothing is sent.
- [ ] A lock reporting `unknown` is named in the message.
- [ ] Message placeholders `{devices} {contacts} {locks} {count} {time}` all substitute correctly.
- [ ] Notification, TTS and music-player targets each receive the message when selected.
- [ ] Speaker volume is set for the announcement and restored afterwards.
- [ ] "Run now" button triggers a check using the same code path as the schedule.
- [ ] Profiles run independently with different schedules, states, messages and targets.

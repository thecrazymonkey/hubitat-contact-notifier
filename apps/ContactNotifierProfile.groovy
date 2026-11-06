definition(
    name: "Contact Notifier Profile",
    namespace: "ivan",
    author: "ivan",
    description: "One profile: devices, schedule, message and delivery targets.",
    parent: "ivan:Contact Notifier",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
    page(name: "mainPage")
}

@groovy.transform.Field static final Map DAYS = [MON: "Monday", TUE: "Tuesday", WED: "Wednesday", THU: "Thursday",
                                                 FRI: "Friday", SAT: "Saturday", SUN: "Sunday"]

def mainPage() {
    dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
        section("Profile") {
            input "profileName", "text", title: "Profile name", required: true
        }
        section("Devices to check") {
            input "contacts", "capability.contactSensor", title: "Contact sensors", multiple: true, required: false
            input "contactAlert", "enum", title: "Alert when contact sensors are", options: ["open", "closed"],
                  defaultValue: "open", required: true
            input "locks", "capability.lock", title: "Locks", multiple: true, required: false
            input "lockAlert", "enum", title: "Alert when locks are", options: ["unlocked", "locked"],
                  defaultValue: "unlocked", required: true
        }
        section("Schedule") {
            input "runTime", "time", title: "Check at", required: true
            input "runDays", "enum", title: "On days", options: DAYS, multiple: true, required: true
        }
        section("Message") {
            paragraph "Placeholders: {devices} {contacts} {locks} {count} {time}"
            input "messageTemplate", "text", title: "Message", defaultValue: "Not secure: {devices}", required: false
        }
        section("Send to") {
            input "notifiers", "capability.notification", title: "Notification devices", multiple: true, required: false
            input "ttsDevices", "capability.speechSynthesis", title: "TTS devices (speak)", multiple: true, required: false
            input "musicDevices", "capability.musicPlayer", title: "Music players (play text)", multiple: true, required: false
            input "speakerVolume", "number", title: "Announcement volume (0-100, optional)", range: "0..100", required: false
            input "restoreDelaySec", "number", title: "Restore previous volume after (seconds)", defaultValue: 15, required: false
        }
        section("Advanced") {
            input "staleHours", "number", title: "Report devices with no activity for N hours (blank = off)", required: false
            input "logEnable", "bool", title: "Enable debug logging", defaultValue: false
            input "runNow", "button", title: "Run now"
        }
    }
}

def installed() { initialize() }
def updated() { initialize() }

def initialize() {
    unschedule()
    String cron = cronFor(settings.runTime as String, settings.runDays as List)
    if (cron) schedule(cron, "runCheck")
    if (settings.profileName) app.updateLabel(settings.profileName as String)
}

def appButtonHandler(String btn) {
    if (btn == "runNow") runCheck()
}

/** Quartz cron for a fixed time of day on selected days; null when time or days are missing. */
String cronFor(String timeIso, List days) {
    def m = timeIso =~ /T(\d{2}):(\d{2})/
    if (!m.find() || !days) return null
    String sorted = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"].findAll { it in days }.join(",")
    if (!sorted) return null
    return "0 ${m.group(2) as int} ${m.group(1) as int} ? * ${sorted}"
}

// ---------------------------------------------------------------- evaluation

/** Returns one map per device that is not in the expected state: [name, kind, state]. */
List<Map> evaluate(Collection contacts, Collection locks, String contactAlert, String lockAlert,
                   Integer staleHours, Long nowMs) {
    List<Map> out = []
    contacts?.each { d -> Map r = classify(d, "contact", ["open", "closed"], contactAlert, staleHours, nowMs); if (r) out << r }
    locks?.each { d -> Map r = classify(d, "lock", ["locked", "unlocked"], lockAlert, staleHours, nowMs); if (r) out << r }
    return out
}

private Map classify(def d, String kind, List valid, String alertState, Integer staleHours, Long nowMs) {
    String name = "Unknown device"
    String state = null
    Date lastActivity = null
    try {
        name = d.displayName
        state = d.currentValue(kind)?.toString()
        lastActivity = d.getLastActivity()
    } catch (Exception e) {
        state = null
    }
    if (state == null || !(state in valid)) return [name: name, kind: kind, state: state ?: "unknown"]
    if (state == alertState) return [name: name, kind: kind, state: state]
    if (staleHours && lastActivity != null && (nowMs - lastActivity.time) > staleHours * 3600000L) {
        return [name: name, kind: kind, state: "stale"]
    }
    return null
}

// ----------------------------------------------------------------- rendering

String render(String template, List<Map> offenders, String timeText) {
    String t = template?.trim() ? template : "Not secure: {devices}"
    Closure fmt = { List l -> l ? l.collect { "${it.name} (${it.state})" }.join(", ") : "none" }
    Map values = [
        devices : fmt(offenders),
        contacts: fmt(offenders.findAll { it.kind == "contact" }),
        locks   : fmt(offenders.findAll { it.kind == "lock" }),
        count   : offenders.size().toString(),
        time    : timeText ?: ""
    ]
    return t.replaceAll(/\{(devices|contacts|locks|count|time)\}/) { all, key -> values[key] }
}

// ------------------------------------------------------------------ delivery

void deliver(String message) {
    List tts = (settings.ttsDevices ?: []) as List
    List music = (settings.musicDevices ?: []) as List
    Integer volume = settings.speakerVolume as Integer
    state.savedVolumes = [:]

    if (volume != null) {
        (tts + music).unique { it.id }.each { d -> safely("set volume on ${d}") { saveAndSetVolume(d, volume) } }
    }
    (settings.notifiers ?: []).each { d -> safely("notify ${d}") { d.deviceNotification(message) } }
    tts.each { d -> safely("speak on ${d}") { d.speak(message) } }
    music.each { d -> safely("playText on ${d}") { d.playText(message) } }

    if (state.savedVolumes) runIn((settings.restoreDelaySec ?: 15) as Integer, "restoreVolumes")
}

void restoreVolumes() {
    List all = (((settings.ttsDevices ?: []) as List) + ((settings.musicDevices ?: []) as List))
    (state.savedVolumes ?: [:]).each { String id, Map saved ->
        def d = all.find { it.id.toString() == id }
        if (d) safely("restore volume on ${d}") { setVolumeAttr(d, saved.attr as String, saved.value) }
    }
    state.savedVolumes = [:]
}

private void saveAndSetVolume(def d, Integer volume) {
    String attr = ["level", "volume"].find { d.hasAttribute(it) }
    if (!attr) return
    state.savedVolumes[d.id.toString()] = [attr: attr, value: d.currentValue(attr)]
    setVolumeAttr(d, attr, volume)
}

private void setVolumeAttr(def d, String attr, def value) {
    if (value == null) return
    if (attr == "level") d.setLevel(value) else d.setVolume(value)
}

private void safely(String what, Closure c) {
    try { c() } catch (Exception e) { log.warn "Contact Notifier: failed to ${what}: ${e.message}" }
}

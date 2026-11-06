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

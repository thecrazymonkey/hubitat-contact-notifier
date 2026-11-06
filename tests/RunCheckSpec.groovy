def dev = { id, attrs -> new FakeDevice(id: id, displayName: id, attrs: attrs) }
def load = { Map s -> Harness.load('apps/ContactNotifierProfile.groovy', [settings: s]) }

// open door + unlocked lock -> exactly ONE notification naming both
def door = dev('Front door', [contact: 'open']); def lock = dev('Garage lock', [lock: 'unlocked'])
def n = dev('n', [:])
def h = load([contacts: [door], locks: [lock], notifiers: [n], messageTemplate: '{count} not secure: {devices}'])
h.call('runCheck')
assert n.calls == [['deviceNotification', ['2 not secure: Front door (open), Garage lock (unlocked)']]]

// everything secure -> silent
n = dev('n', [:])
h = load([contacts: [dev('D', [contact: 'closed'])], locks: [dev('L', [lock: 'locked'])], notifiers: [n]])
h.call('runCheck')
assert n.calls.isEmpty()

// lock reporting unknown is named
n = dev('n', [:])
h = load([locks: [dev('Back lock', [lock: 'unknown'])], notifiers: [n], messageTemplate: '{devices}'])
h.call('runCheck')
assert n.calls[0][1] == ['Back lock (unknown)']

// no devices / no settings -> silent no-op
h = load([:])
h.call('runCheck')
assert h.named('runIn').isEmpty()

// {time} uses the hub time zone formatting
n = dev('n', [:])
h = load([contacts: [door], notifiers: [n], messageTemplate: '{time}'])
h.hooks.now = { 22 * 3600000L + 5 * 60000L }   // 22:05 UTC
h.call('runCheck')
assert n.calls[0][1] == ['10:05 PM']

// stale option wired from settings
n = dev('n', [:])
def old = new FakeDevice(id: 'o', displayName: 'Old', attrs: [contact: 'closed'], lastActivity: new Date(0))
h = load([contacts: [old], notifiers: [n], staleHours: 2, messageTemplate: '{devices}'])
h.hooks.now = { 5 * 3600000L }
h.call('runCheck')
assert n.calls[0][1] == ['Old (stale)']

// run-now button uses the same path
n = dev('n', [:])
h = load([contacts: [door], notifiers: [n]])
h.call('appButtonHandler', 'runNow')
assert n.calls.size() == 1

// debug logging only when enabled
h = load([logEnable: true]); h.call('runCheck')
assert h.script.binding.getVariable('log').lines.any { it[0] == 'debug' }
h = load([:]); h.call('runCheck')
assert h.script.binding.getVariable('log').lines.isEmpty()
println 'RunCheckSpec ok'

def dev = { id, attrs = [:] -> new FakeDevice(id: id, displayName: id, attrs: attrs) }
def load = { Map s -> Harness.load('apps/ContactNotifierProfile.groovy', [settings: s]) }

// all three target types receive the message
def n = dev('n'); def t = dev('t'); def m = dev('m')
def h = load([notifiers: [n], ttsDevices: [t], musicDevices: [m]])
h.call('deliver', 'Hello')
assert n.calls == [['deviceNotification', ['Hello']]]
assert t.calls == [['speak', ['Hello']]]
assert m.calls == [['playText', ['Hello']]]

// volume set before announce, previous saved, restored later
t = dev('t', [level: 20])
h = load([ttsDevices: [t], speakerVolume: 40, restoreDelaySec: 7])
h.call('deliver', 'Hi')
assert t.calls*.getAt(0) == ['setLevel', 'speak']
assert t.calls[0][1] == [40]
assert h.named('runIn')[0][1] == [7, 'restoreVolumes']
h.call('restoreVolumes')
assert t.calls.last() == ['setLevel', [20]]
assert h.script.binding.getVariable('state').savedVolumes == [:]

// AudioVolume style attribute and default delay
m = dev('m', [volume: 11])
h = load([musicDevices: [m], speakerVolume: 30])
h.call('deliver', 'Hi')
assert m.calls[0] == ['setVolume', [30]]
assert h.named('runIn')[0][1][0] == 15
h.call('restoreVolumes')
assert m.calls.last() == ['setVolume', [11]]

// two announcements inside the restore window keep the ORIGINAL volume
t = dev('t', [level: 20])
h = load([ttsDevices: [t], speakerVolume: 40])
h.call('deliver', 'one'); h.call('deliver', 'two'); h.call('restoreVolumes')
assert t.attrs.level == 20

// restoreDelaySec 0 is respected, not replaced by the default
t = dev('t', [level: 20])
h = load([ttsDevices: [t], speakerVolume: 40, restoreDelaySec: 0])
h.call('deliver', 'Hi')
assert h.named('runIn')[0][1] == [0, 'restoreVolumes']

// no volume attribute: still announces, nothing saved, no restore scheduled
t = dev('t')
h = load([ttsDevices: [t], speakerVolume: 40])
h.call('deliver', 'Hi')
assert t.calls == [['speak', ['Hi']]]
assert h.named('runIn').isEmpty()

// no speakerVolume setting: volume untouched
t = dev('t', [level: 20])
h = load([ttsDevices: [t]])
h.call('deliver', 'Hi')
assert t.calls == [['speak', ['Hi']]]

// one failing device does not block the others
n = dev('n'); n.throwOn = ['deviceNotification']; t = dev('t')
h = load([notifiers: [n], ttsDevices: [t]])
h.call('deliver', 'Hi')
assert t.calls == [['speak', ['Hi']]]
println 'DeliverSpec ok'

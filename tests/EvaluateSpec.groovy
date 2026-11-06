def h = Harness.load('apps/ContactNotifierProfile.groovy')
def c = { n, v, la = null -> new FakeDevice(id: n, displayName: n, attrs: [contact: v], lastActivity: la) }
def l = { n, v, la = null -> new FakeDevice(id: n, displayName: n, attrs: [lock: v], lastActivity: la) }
def ev = { cs, ls, ca = 'open', la = 'unlocked', st = null, now = 0L -> h.call('evaluate', cs, ls, ca, la, st, now) }

assert ev([c('Front door', 'open')], []) == [[name: 'Front door', kind: 'contact', state: 'open']]
assert ev([c('Front door', 'closed')], []) == []
assert ev([], [l('Garage lock', 'unlocked')]) == [[name: 'Garage lock', kind: 'lock', state: 'unlocked']]
assert ev([], [l('Garage lock', 'locked')]) == []
assert ev([], [l('Back lock', 'unknown')]) == [[name: 'Back lock', kind: 'lock', state: 'unknown']]
assert ev([], [l('Back lock', null)])[0].state == 'unknown'
assert ev([c('Gone', 'closed')], [])  == []
// removed/null device does not throw
assert ev([null], [])[0].state == 'unknown'
// inverted expectations
assert ev([c('D', 'closed')], [l('L', 'locked')], 'closed', 'locked').size() == 2
// stale
long now = 10 * 3600000L
assert ev([c('Old', 'closed', new Date(now - 3 * 3600000L))], [], 'open', 'unlocked', 2, now) == [[name: 'Old', kind: 'contact', state: 'stale']]
assert ev([c('Old', 'closed', new Date(now - 3 * 3600000L))], [], 'open', 'unlocked', 0, now) == []
assert ev([c('Old', 'closed', new Date(now - 3 * 3600000L))], [], 'open', 'unlocked', null, now) == []
assert ev([c('Fresh', 'closed', new Date(now - 1 * 3600000L))], [], 'open', 'unlocked', 2, now) == []
// order: contacts then locks
assert ev([c('A', 'open')], [l('B', 'unlocked')])*.kind == ['contact', 'lock']
println 'EvaluateSpec ok'

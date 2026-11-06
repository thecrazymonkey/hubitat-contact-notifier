def h = Harness.load('apps/ContactNotifierProfile.groovy')
def offs = [[name: 'Front door', kind: 'contact', state: 'open'], [name: 'Garage lock', kind: 'lock', state: 'unlocked']]
def r = { t, o = offs, tm = '10:00 PM' -> h.call('render', t, o, tm) }

assert r('Heads up, {count} not secure: {devices}') == 'Heads up, 2 not secure: Front door (open), Garage lock (unlocked)'
assert r('Unlocked: {locks}. Open: {contacts}.') == 'Unlocked: Garage lock (unlocked). Open: Front door (open).'
assert r('{time} check: {devices}') == '10:00 PM check: Front door (open), Garage lock (unlocked)'
assert r('Unlocked: {locks}. Open: {contacts}.', [offs[0]]) == 'Unlocked: none. Open: Front door (open).'
assert r('No placeholders here') == 'No placeholders here'
assert r('Unknown {foo} stays') == 'Unknown {foo} stays'
assert r('') == 'Not secure: Front door (open), Garage lock (unlocked)'
assert r(null) == 'Not secure: Front door (open), Garage lock (unlocked)'
// a device name that looks like a placeholder is not re-expanded
assert r('{devices} / {count}', [[name: '{count}', kind: 'lock', state: 'unknown']]) == '{count} (unknown) / 1'
println 'RenderSpec ok'

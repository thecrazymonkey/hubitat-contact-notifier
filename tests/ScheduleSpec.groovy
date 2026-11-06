def h = Harness.load('apps/ContactNotifierProfile.groovy')
assert h.call('cronFor', '2026-01-01T22:05:00.000-0500', ['WED', 'MON']) == '0 5 22 ? * MON,WED'
assert h.call('cronFor', '2026-01-01T06:00:00.000+0100', ['SUN', 'SAT']) == '0 0 6 ? * SAT,SUN'
assert h.call('cronFor', '2026-01-01T22:05:00.000-0500', []) == null
assert h.call('cronFor', '2026-01-01T22:05:00.000-0500', null) == null
assert h.call('cronFor', '', ['MON']) == null
assert h.call('cronFor', null, ['MON']) == null

// initialize: unschedule first, then schedule once; label updated
h = Harness.load('apps/ContactNotifierProfile.groovy', [settings: [runTime: '2026-01-01T22:05:00.000-0500', runDays: ['MON'], profileName: 'Night']])
h.call('initialize')
assert h.calls*.getAt(0) .findAll { it in ['unschedule', 'schedule'] } == ['unschedule', 'schedule']
assert h.named('schedule')[0][1] == ['0 5 22 ? * MON', 'runCheck']
assert h.script.binding.getVariable('app').labels == ['Night']

// no days -> unscheduled only, no crash
h = Harness.load('apps/ContactNotifierProfile.groovy', [settings: [runTime: '2026-01-01T22:05:00.000-0500', runDays: []]])
h.call('initialize')
assert h.named('unschedule').size() == 1 && h.named('schedule').isEmpty()

// preferences page defines every documented input
h = Harness.load('apps/ContactNotifierProfile.groovy')
h.call('mainPage')
def names = h.named('input')*.getAt(1)*.find { it instanceof String }
['profileName','contacts','locks','contactAlert','lockAlert','runTime','runDays','messageTemplate','notifiers',
 'ttsDevices','musicDevices','speakerVolume','restoreDelaySec','staleHours','logEnable','runNow'].each { assert it in names, it }

// Run-now button triggers a check (no devices -> silent, no exception)
h.call('appButtonHandler', 'runNow')
println 'ScheduleSpec ok'

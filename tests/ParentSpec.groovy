def h = Harness.load('apps/ContactNotifierParent.groovy')
assert h.named('definition')[0][1][0].name == 'Contact Notifier'
h.call('mainPage')
def appCall = h.named('app')[0][1][0]
assert appCall.appName == 'Contact Notifier Profile'
assert appCall.multiple == true
h.call('installed'); h.call('updated')
println 'ParentSpec ok'

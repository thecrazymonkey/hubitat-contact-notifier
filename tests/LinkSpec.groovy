// the parent/child link is spread over three places that must agree, or the hub cannot add profiles
def parent = Harness.load('apps/ContactNotifierParent.groovy')
def child = Harness.load('apps/ContactNotifierProfile.groovy')
Map pd = parent.named('definition')[0][1][0]
Map cd = child.named('definition')[0][1][0]
parent.call('mainPage')
Map appCall = parent.named('app')[0][1][0]

assert cd.parent == "${pd.namespace}:${pd.name}".toString()
assert appCall.namespace == cd.namespace
assert appCall.appName == cd.name
println 'LinkSpec ok'

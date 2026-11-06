class FakeDevice {
    def id; String displayName; Map attrs = [:]; Date lastActivity; List calls = []; List throwOn = []
    def currentValue(String a) { attrs[a] }
    Date getLastActivity() { lastActivity }
    boolean hasAttribute(String a) { attrs.containsKey(a) }
    def methodMissing(String n, args) {
        if (n in throwOn) throw new RuntimeException("boom $n")
        calls << [n, args.toList()]
        if (n in ['setLevel', 'setVolume']) { attrs[attrs.containsKey('level') ? 'level' : 'volume'] = args[0] }
        null
    }
}
class FakeLog { List lines = []; def methodMissing(String n, args) { lines << [n, args[0]] } }
class FakeApp { List labels = []; def updateLabel(String l) { labels << l } }

class Harness {
    List calls = []     // recorded stubbed platform calls: [name, args]
    Map hooks = [now: { System.currentTimeMillis() }]
    Script script
    static Harness load(String path, Map binding = [:]) {
        Harness h = new Harness()
        def b = new Binding([settings: [:], state: [:], log: new FakeLog(), app: new FakeApp(),
                             location: [timeZone: TimeZone.getTimeZone('UTC')]] + binding)
        h.script = new GroovyShell(new GroovyClassLoader(), b).parse(new File(path))
        h.script.metaClass.methodMissing = { String name, args ->
            if (h.hooks.containsKey(name)) return h.hooks[name](*args)
            h.calls << [name, args.toList()]
            def last = args ? args[-1] : null
            if (last instanceof Closure) { last.delegate = h.script; return last.call() }
            null
        }
        h.script.run()
        h
    }
    def call(String m, Object... a) { script."$m"(*a) }
    List named(String n) { calls.findAll { it[0] == n } }
}

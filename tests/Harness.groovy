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
        b.getVariable('app').onPageCall = { Map m -> h.calls << ['app', [m]] }
        h.script.run()
        h
    }
    def call(String m, Object... a) { script."$m"(*a) }
    List named(String n) { calls.findAll { it[0] == n } }
}

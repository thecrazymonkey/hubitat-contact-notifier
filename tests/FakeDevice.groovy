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

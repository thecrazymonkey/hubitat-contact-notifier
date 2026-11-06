class FakeLog { List lines = []; def methodMissing(String n, args) { lines << [n, args[0]] } }

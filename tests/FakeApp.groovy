// Stand-in for Hubitat's `app` object. The parent app also calls a page builder named app(...); in Groovy 2.4
// (the hub's version) the `app` variable wins over that method, so call(Map) forwards it to the harness recorder.
class FakeApp {
    List labels = []
    Closure onPageCall
    def updateLabel(String l) { labels << l }
    def call(Map args) { onPageCall?.call(args) }
}

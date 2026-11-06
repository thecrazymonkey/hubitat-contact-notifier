/**
 * Contact Notifier (parent) - manages notification profiles.
 * Install together with "Contact Notifier Profile" (child app), namespace ivan.
 */
definition(
    name: "Contact Notifier",
    namespace: "ivan",
    author: "ivan",
    description: "Scheduled check of doors and locks with a single combined notification per profile.",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    singleInstance: true
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Contact Notifier", install: true, uninstall: true) {
        section {
            app(name: "profiles", appName: "Contact Notifier Profile", namespace: "ivan",
                title: "Add profile", multiple: true)
        }
    }
}

def installed() { initialize() }
def updated() { initialize() }
def initialize() { }

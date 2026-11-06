import me.biocomp.hubitat_ci.api.app_api.AppExecutor
import me.biocomp.hubitat_ci.api.common_api.InstalledAppWrapper
import me.biocomp.hubitat_ci.app.HubitatAppSandbox
import spock.lang.Specification

class AppValidationSpec extends Specification {
    def "parent app passes Hubitat validation"() {
        expect:
        new HubitatAppSandbox(new File("apps/ContactNotifierParent.groovy")).run()
    }

    def "profile app passes Hubitat validation"() {
        // hubitat_ci 0.17 (2021) predates the 'button' input type that real Hubitat supports,
        // so validate with that one input swapped for a 'bool'. Everything else is checked as-is.
        given:
        String text = new File("apps/ContactNotifierProfile.groovy").text
        assert text.contains('input "runNow", "button"')
        String patched = text.replace('input "runNow", "button"', 'input "runNow", "bool"')

        expect:
        new HubitatAppSandbox(patched).run()
    }

    def "profile initialize() schedules via the Hubitat API and sets the label"() {
        given:
        String patched = new File("apps/ContactNotifierProfile.groovy").text
                .replace('input "runNow", "button"', 'input "runNow", "bool"')
        def installedApp = Mock(InstalledAppWrapper)
        def appApi = Mock(AppExecutor) { _ * getApp() >> installedApp }
        def sandbox = new HubitatAppSandbox(patched)

        when:
        def script = sandbox.run(api: appApi, userSettingValues: [runTime: "2026-01-01T22:05:00.000-0500",
                                                                   runDays: ["MON", "WED"], profileName: "Night"])
        script.initialize()

        then:
        1 * appApi.unschedule("runCheck")
        1 * appApi.schedule("0 5 22 ? * MON,WED", "runCheck")
        1 * installedApp.updateLabel("Night")
    }
}


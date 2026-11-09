package runners;

import io.cucumber.junit.CucumberOptions;
import io.cucumber.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(features = "src/test/resources/features", glue = "definitions",
    plugin = {"io.cucumber.core.plugin.SerenityReporter", "pretty",
        "html:build/reports/cucumber.html", "json:build/reports/cucumber.json"})
public class TurnosE2ETest { }

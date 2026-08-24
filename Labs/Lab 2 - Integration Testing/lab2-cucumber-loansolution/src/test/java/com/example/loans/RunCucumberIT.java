package com.example.loans;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * The Cucumber runner. You do not need to change anything in this file.
 *
 * What each annotation does:
 *
 *   @Suite                    marks this as a JUnit Platform test suite
 *   @IncludeEngines           run the Cucumber engine (not plain JUnit tests)
 *   @SelectClasspathResource  where the .feature files live, relative to
 *                             src/test/resources
 *   GLUE_PROPERTY_NAME        which package holds the step definitions
 *   PLUGIN_PROPERTY_NAME      how to report results
 *
 * NOTE THE CLASS NAME: it ends in "IT", not "Test".
 *
 * Maven's Surefire plugin runs classes named *Test during `mvn test`.
 * Maven's Failsafe plugin runs classes named *IT during `mvn verify`.
 * Cucumber scenarios are integration tests, so this project uses Failsafe.
 *
 *   mvn test     -> runs nothing, reports success  (there are no *Test classes)
 *   mvn verify   -> runs the scenarios             <-- use this one
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.example.loans")
@ConfigurationParameter(
        key = PLUGIN_PROPERTY_NAME,
        value = "pretty, summary, html:target/cucumber-report.html")
public class RunCucumberIT {
}

package net.serenitybdd.integration.jenkins;

import net.serenitybdd.integration.jenkins.client.JenkinsClient;
import net.serenitybdd.integration.jenkins.environment.PluginDescription;
import net.serenitybdd.integration.jenkins.environment.rules.ApplicativeTestRule;
import net.serenitybdd.integration.jenkins.environment.rules.InstallPlugins;
import net.serenitybdd.integration.jenkins.environment.rules.ManageJenkinsServer;
import net.serenitybdd.integration.utils.RuleChains;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static net.serenitybdd.integration.utils.ListFunctions.concat;

public class JenkinsInstance implements TestRule {
    private final PluginDescription pluginUnderTest;

    private Path jenkinsHome = Paths.get(System.getProperty("java.io.tmpdir"));
    private int  portNumber  = 8080;

    private JenkinsClient client = null;    // instantiated when the Jenkins server is up and running

    private List<? extends ApplicativeTestRule<JenkinsInstance>> customRulesToApplyBeforeStart = new ArrayList<>();
    private List<? extends ApplicativeTestRule<JenkinsInstance>> defaultRules;
    private List<? extends ApplicativeTestRule<JenkinsInstance>> customRulesToApplyAfterStart  = new ArrayList<>();

    /**
     * @param   pluginUnderTest
     *          Path to the plugin under test, either a .hpi or a .jpi file
     * @param   jackson2ApiPlugin
     *          Path to the Jackson 2 API plugin, either a .hpi or a .jpi file
     * @param   snakeyamlApiPlugin
     *          Path to the SnakeYAML API plugin, either a .hpi or a .jpi file
     */
    public JenkinsInstance(Path pluginUnderTest, Path jackson2ApiPlugin, Path snakeyamlApiPlugin) {
        this(PluginDescription.of(pluginUnderTest), PluginDescription.of(jackson2ApiPlugin), PluginDescription.of(snakeyamlApiPlugin));
    }

    /**
     * @param   description
     *          Plugin meta-data derived from the manifest file packaged with the plugin
     */
    public JenkinsInstance(PluginDescription description, PluginDescription jackson2ApiPlugin, PluginDescription snakeyamlApiPlugin) {
        this.pluginUnderTest = description;

        defaultRules = asList(
                InstallPlugins.fromDisk(snakeyamlApiPlugin.path()),
                InstallPlugins.fromDisk(jackson2ApiPlugin.path()),
                InstallPlugins.fromDisk(pluginUnderTest.path()),
                new ManageJenkinsServer()
        );
    }

    public String pluginUnderTestName() {
        return pluginUnderTest.fullName();
    }

    public String pluginUnderTestVersion() {
        return pluginUnderTest.version();
    }

    public String version() {
        return System.getProperty("jenkins.version");
    }

    public Path home() {
        return this.jenkinsHome;
    }

    public void setHome(Path jenkinsHome) {
        this.jenkinsHome = jenkinsHome;
    }

    public JenkinsClient client() {
        return client;
    }

    public void setClient(JenkinsClient client) {
        this.client = client;
    }

    public URL url() {
        try {
            return new URL(format("http://localhost:%d/", portNumber));
        } catch (MalformedURLException e) {
            throw new RuntimeException(format("Couldn't instantiate a URL as 'http://localhost:%d/'", portNumber));
        }
    }

    public int  port() {
        return portNumber;
    }

    public void setPort(int portNumber) {
        this.portNumber = portNumber;
    }

    public <ATR extends ApplicativeTestRule<JenkinsInstance>> JenkinsInstance beforeStartApply(List<ATR> customRulesToBeApplied) {
        this.customRulesToApplyBeforeStart = Collections.unmodifiableList(new ArrayList<>(customRulesToBeApplied));

        return this;
    }

    public <ATR extends ApplicativeTestRule<JenkinsInstance>> JenkinsInstance afterStartApply(List<ATR> customRulesToBeApplied) {
        this.customRulesToApplyAfterStart = Collections.unmodifiableList(new ArrayList<>(customRulesToBeApplied));

        return this;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return chainOf(concat(customRulesToApplyBeforeStart, defaultRules, customRulesToApplyAfterStart)).apply(base, description);
    }

    private <ATR extends ApplicativeTestRule<JenkinsInstance>> RuleChain chainOf(List<ATR> rules) {
        return RuleChains.chained(instantiated(rules));
    }

    private <ATR extends ApplicativeTestRule<JenkinsInstance>> List<TestRule> instantiated(List<ATR> rules) {
        List<TestRule> instantiatedRules = new ArrayList<>(rules.size());

        for (ATR testRule : rules) {
            instantiatedRules.add(testRule.applyTo(this));
        }

        return instantiatedRules;
    }
}

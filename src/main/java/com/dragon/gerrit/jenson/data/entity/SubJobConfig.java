package com.dragon.gerrit.jenson.data.entity;

import org.kohsuke.stapler.DataBoundConstructor;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

public class SubJobConfig extends AbstractDescribableImpl<SubJobConfig> {
    private String sonarReportPath;

    @DataBoundConstructor
    public SubJobConfig(String sonarReportPath) {
        this.sonarReportPath = sonarReportPath;
    }

    public String getSonarReportPath() {
        return sonarReportPath;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SubJobConfig> {
        public String getDisplayName() {
            return "SubJobConfig";
        }
    }
}

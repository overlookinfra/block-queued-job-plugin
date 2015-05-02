package org.jenkinsci.plugins.blockqueuedjob.utils;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * @author Kanstantsin Shautsou
 */
public class DelayBuilder extends Builder {
    private long delay;

    @DataBoundConstructor
    public DelayBuilder(long delay) {
        this.delay = delay;
    }

    public long getDelay() {
        return delay;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        Thread.sleep(getDelay());
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> {

        @Override
        public String getDisplayName() {
            return "Delay Builder";
        }
    }
}

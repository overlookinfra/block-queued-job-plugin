package org.jenkinsci.plugins.blockqueuedjob.condition;

import hudson.Extension;
import hudson.model.*;
import hudson.model.queue.CauseOfBlockage;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.blockqueuedjob.utils.Utils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Block this job in queue when there are other matched by regexp in queue jobs.
 * Implements logic of other plugin:
 * {@see https://wiki.jenkins-ci.org/display/JENKINS/Build+Blocker+Plugin}
 *
 * @author Kanstantsin Shautsou
 */
public class RegexBuildingBlockQueueCondition extends BlockQueueCondition {
    private final static Logger LOGGER = Logger.getLogger(RegexBuildingBlockQueueCondition.class.getClass().getName());

    private String regexStr;

    private transient List<Pattern> patterns;

    @DataBoundConstructor
    public RegexBuildingBlockQueueCondition(String regexStr) {
        this.regexStr = regexStr;
    }

    public String getRegexStr() {
        return regexStr;
    }

    public List<Pattern> getPatterns() {
        if (patterns == null) {
            patterns = strToPattern(regexStr);
        }
        return patterns;
    }

    public static List<Pattern> strToPattern(String str) {
        final ArrayList<Pattern> patterns = new ArrayList<>();

        for (String pattern : str.split("\n")) {
            if (!pattern.isEmpty() && !pattern.startsWith("#")) {
                patterns.add(Pattern.compile(pattern));
            }
        }

        return patterns;
    }

    @Override
    public CauseOfBlockage isBlocked(Queue.Item item) {
        final AbstractProject<?, ?> thisProject = ((AbstractProject<?, ?>) item.task).getRootProject();
        LOGGER.log(Level.INFO, "Checking regex blockage for {0}", thisProject.getFullName());

        final Jenkins jenkins = Utils.getJenkinsInstance();

        final List<Queue.BuildableItem> buildableItems = jenkins.getQueue().getBuildableItems();


        for (Queue.BuildableItem buildableItem : buildableItems) {
            if (buildableItem.task instanceof AbstractProject<?, ?>) {
                final AbstractProject<?, ?> abstractProject = (AbstractProject<?, ?>) buildableItem.task;
                // no relative/patterns
                final String fullName = abstractProject.getFullName();
                for (final Pattern pattern : getPatterns()) {
                    if (pattern.matcher(fullName).matches()) {
                        return new CauseOfBlockage() {
                            @Override
                            public String getShortDescription() {
                                return "RegexBuildingBlockQueueCondition: pattern '" + pattern.toString() +
                                        "' matched job: '" + fullName + "'";
                            }
                        };
                    }
                }
            }
        }

        final Computer[] computers = jenkins.getComputers();
        for (Computer computer : computers) {
            final List<Executor> executors = computer.getExecutors();
            executors.addAll(computer.getOneOffExecutors());

            for (Executor executor : executors) {
                if (executor.isBusy()) {
                    final Queue.Executable currentExecutable = executor.getCurrentExecutable();
                    if (currentExecutable instanceof AbstractBuild<?, ?>) {
                        final AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) currentExecutable;
                        AbstractProject<?, ?> currentProject = build.getRootBuild().getProject();
                        final String fullName = currentProject.getFullName();
                        for (final Pattern pattern : getPatterns()) {
                            if (pattern.matcher(fullName).matches()) {
                                return new CauseOfBlockage() {
                                    @Override
                                    public String getShortDescription() {
                                        return "RegexBuildingBlockQueueCondition: pattern '" + pattern.toString() +
                                                "' matched job: '" + fullName + "'";
                                    }
                                };
                            }
                        }
                    }
                }
            }
        }


        return null;
    }

    @Extension
    public static class DescriptorImpl extends BlockQueueConditionDescriptor {

        public FormValidation doCheckRegexStr(@QueryParameter String regexStr) {
            try {
                strToPattern(regexStr);
            } catch (Throwable t) {
                return FormValidation.error("Can't parse patterns: " + t.getMessage());
            }

            return FormValidation.ok();
        }

        @Override
        public String getDisplayName() {
            return "Block by regexp in queue";
        }
    }
}
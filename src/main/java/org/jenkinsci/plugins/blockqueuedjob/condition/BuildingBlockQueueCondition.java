package org.jenkinsci.plugins.blockqueuedjob.condition;

import hudson.Extension;
import hudson.model.*;
import hudson.model.queue.CauseOfBlockage;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.blockqueuedjob.utils.Utils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;

/**
 * Blocks when specified last build from job is building
 * //TODO get all building for throttle concurrent?
 * @author Kanstantsin Shautsou
 */
public class BuildingBlockQueueCondition extends BlockQueueCondition {
    @CheckForNull
    private String project; // atm supports only one job

    @DataBoundConstructor
    public BuildingBlockQueueCondition(String project) {
        this.project = project;
    }

    public String getProject() {
        return project;
    }

    @Override
    public CauseOfBlockage isBlocked() {
        if (project == null || project.isEmpty()) {
            return null; //don't pause all jobs because of not filled configuration
        }

        CauseOfBlockage blocked = null;
        Jenkins instance = Utils.getJenkinsInstance();
        TopLevelItem item = instance.getItem(project);
        if (item != null) {
            if (item instanceof AbstractProject<?, ?>) {
                final AbstractProject<?, ?> project = (AbstractProject<?, ?>) item;
                final AbstractBuild<?, ?> lastBuild = project.getLastBuild();
                if (lastBuild != null && lastBuild.isBuilding()) { // wait result
                    blocked = new CauseOfBlockage() {
                        @Override
                        public String getShortDescription() {
                            return project.getFullName() + " is building: " + lastBuild.getDisplayName();
                        }
                    };
                }
            }
        }

        return blocked;
    }


    @Extension
    public static class DescriptorImpl extends BlockQueueConditionDescriptor {

        public AutoCompletionCandidates doAutoCompleteProject(@QueryParameter String value,
                                                               @AncestorInPath Item self,
                                                               @AncestorInPath ItemGroup container) {
            return AutoCompletionCandidates.ofJobNames(Job.class, value, self, container);
        }

        public FormValidation doCheckProject(@QueryParameter String project) {
            FormValidation formValidation;

            if (project == null || project.isEmpty()) {
                formValidation = FormValidation.error("Job must be specified");
            } else if (Utils.getJenkinsInstance().getItem(project) == null){
                formValidation = FormValidation.error("Job " + project + " not found");
            } else {
                formValidation = FormValidation.ok();
            }

            return formValidation;
        }

        @Override
        public String getDisplayName() {
            return "Block when last build is building";
        }
    }

}

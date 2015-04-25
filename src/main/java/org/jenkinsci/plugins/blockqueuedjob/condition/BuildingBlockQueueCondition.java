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
 *
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
    public CauseOfBlockage isBlocked(Queue.Item item) {
        // user configured blocking, so doesn't allow bad configurations
        if (project == null || project.isEmpty()) {
            return new CauseOfBlockage() {
                @Override
                public String getShortDescription() {
                    return "BuildingBlockQueueCondition: project is not specified!";
                }
            };
        }

        CauseOfBlockage blocked = null;
        Jenkins instance = Utils.getJenkinsInstance();
        AbstractProject<?, ?> taskProject = (AbstractProject<?, ?>) item.task;

        Item targetProject = instance.getItem(project, taskProject.getParent());
        if (targetProject instanceof AbstractProject<?, ?>) {
            final AbstractProject<?, ?> project = (AbstractProject<?, ?>) targetProject;
            final AbstractBuild<?, ?> lastBuild = project.getLastBuild();
            if (lastBuild != null && lastBuild.isBuilding()) { // wait result
                blocked = new CauseOfBlockage() {
                    @Override
                    public String getShortDescription() {
                        return "BuildingBlockQueueCondition: " + project.getFullName() + " is building: " + lastBuild.getDisplayName();
                    }
                };
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

        public FormValidation doCheckProject(@QueryParameter String project,
                                             @AncestorInPath Item self) {
            FormValidation formValidation;

            if (project == null || project.isEmpty()) {
                formValidation = FormValidation.error("Job must be specified");
            } else if (Utils.getJenkinsInstance().getItem(project, self.getParent()) == null) {
                formValidation = FormValidation.error("Job: '" + project + "', parent: '" + self.getParent().getFullName() + "' not found");
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

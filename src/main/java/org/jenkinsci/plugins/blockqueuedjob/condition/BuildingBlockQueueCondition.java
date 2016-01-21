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

import static org.apache.commons.lang.StringUtils.isEmpty;

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
        if (isEmpty(project)) {
            return new CauseOfBlockage() {
                @Override
                public String getShortDescription() {
                    return "BuildingBlockQueueCondition: project is not specified!";
                }
            };
        }

        CauseOfBlockage blocked = null;
        Jenkins instance = Jenkins.getActiveInstance();

        Job<?, ?> taskJob = (Job<?, ?>) item.task;

        final Item targetJob = instance.getItem(project, taskJob.getParent());
        if (targetJob instanceof Job<?, ?>) {
            final Job<?, ?> job = (Job<?, ?>) targetJob;
            final Run<?, ?> lastRun = job.getLastBuild();
            if (lastRun != null && lastRun.isBuilding()) { // wait result
                blocked = new CauseOfBlockage() {
                    @Override
                    public String getShortDescription() {
                        return String.format("BuildingBlockQueueCondition: %s is building: %s",
                                job.getFullName(), lastRun.getDisplayName());
                    }
                };
            }
        } else {
            blocked = new CauseOfBlockage() {
                @Override
                public String getShortDescription() {
                    String error;
                    if (targetJob == null) {
                        error = "BuildingBlockQueueCondition: Job " + project + " not exist";
                    } else {
                        error = String.format("BuildingBlockQueueCondition: Job %s has unknown type %s",
                                project, project.getClass());
                    }
                    return error;
                }
            };
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

            if (isEmpty(project)) {
                formValidation = FormValidation.error("Job must be specified");
            } else if (Jenkins.getActiveInstance().getItem(project, self.getParent()) == null) {
                formValidation = FormValidation.error(String.format("Job: '%s', parent: '%s' not found", project,
                        self.getParent().getFullName()));
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

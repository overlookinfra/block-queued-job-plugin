package org.jenkinsci.plugins.blockqueuedjob.condition;

import hudson.Extension;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.queue.CauseOfBlockage;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Blocks according to last build result of specified job
 *
 * @author Kanstantsin Shautsou
 */
public class JobResultBlockQueueCondition extends BlockQueueCondition {
    @CheckForNull
    private String project; // atm supports only one job

    @CheckForNull
    private Result result = Result.UNSTABLE;

    @DataBoundConstructor
    public JobResultBlockQueueCondition(String project, Result result) {
        this.project = project;
        this.result = result;
    }

    public String getProject() {
        return project;
    }

    public Result getResult() {
        return result;
    }

    @Override
    public CauseOfBlockage isBlocked(Queue.Item item) {
        // user configured blocking, so doesn't allow bad configurations
        if (isEmpty(project) || result == null) {
            return new CauseOfBlockage() {
                @Override
                public String getShortDescription() {
                    return "JobResultBlockQueueCondition: bad condition configuration!";
                }
            };
        }

        CauseOfBlockage blocked = null;
        Jenkins instance = Jenkins.getActiveInstance();
        final Job<?, ?> itemTask = (Job<?, ?>) item.task;

        final Item targetProject = instance.getItem(project, itemTask.getParent());
        if (targetProject instanceof Job<?, ?>) {
            final Job<?, ?> job = (Job<?, ?>) targetProject;
            final Run<?, ?> lastRun = job.getLastBuild();
            if (lastRun != null) {
                if (lastRun.getResult().isWorseOrEqualTo(result)) {
                    blocked = new CauseOfBlockage() {
                        @Override
                        public String getShortDescription() {
                            return String.format("Last %s  build is %s", job.getFullName(), lastRun.getResult());
                        }
                    };
                }
            } // else no builds -> allow run
        } else {
            blocked = new CauseOfBlockage() {
                @Override
                public String getShortDescription() {
                    String error;
                    if (targetProject == null) {
                        error = "Job " + project + " not exist";
                    } else {
                        error = "Job " + project + " has unknown type " + project.getClass();
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
                formValidation = FormValidation.error(String.format("Job: '%s', parent: '%s' not found",
                        project, self.getParent().getFullName()));
            } else {
                formValidation = FormValidation.ok();
            }

            return formValidation;
        }

        public ListBoxModel doFillResultItems() {
            ListBoxModel items = new ListBoxModel();
            items.add(Result.SUCCESS.toString());
            items.add(Result.UNSTABLE.toString());
            items.add(Result.FAILURE.toString());
            return items;
        }

        @Override
        public String getDisplayName() {
            return "Block when last build result";
        }
    }
}

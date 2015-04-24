package org.jenkinsci.plugins.blockqueuedjob.condition;

import hudson.Extension;
import hudson.model.*;
import hudson.model.queue.CauseOfBlockage;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.blockqueuedjob.utils.Utils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;

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
    public CauseOfBlockage isBlocked() {
        if (project == null || project.isEmpty() || result == null) {
            return null; //don't pause all jobs because of not filled configuration
        }

        CauseOfBlockage blocked = null;
        Jenkins instance = Utils.getJenkinsInstance();
        TopLevelItem item = instance.getItem(project);
        if (item != null) {
            if (item instanceof AbstractProject<?, ?>) {
                final AbstractProject<?, ?> project = (AbstractProject<?, ?>) item;
                final AbstractBuild<?, ?> lastBuild = project.getLastBuild();
                if (lastBuild != null) {
                    if (lastBuild.getResult().isWorseOrEqualTo(result)) {
                        blocked = new CauseOfBlockage() {
                            @Override
                            public String getShortDescription() {
                                return "Last " + project.getFullName() + "  build is " + lastBuild.getResult();
                            }
                        };
                    }
                } else {
                    // no builds -> allow run
                }
            }
        } else {
            blocked = new CauseOfBlockage() {
                @Override
                public String getShortDescription() {
                    return project + " job not found";
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

        public FormValidation doCheckProject(@QueryParameter String project) {
            FormValidation formValidation;

            if (project == null || project.isEmpty()) {
                formValidation = FormValidation.error("Job must be specified");
            } else if (Utils.getJenkinsInstance().getItem(project) == null) {
                formValidation = FormValidation.error("Job " + project + " not found");
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

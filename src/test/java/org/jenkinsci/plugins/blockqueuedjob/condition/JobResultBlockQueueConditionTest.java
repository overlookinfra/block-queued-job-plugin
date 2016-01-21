package org.jenkinsci.plugins.blockqueuedjob.condition;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.queue.CauseOfBlockage;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class, ItemGroup.class, AbstractBuild.class})
public class JobResultBlockQueueConditionTest {

    private static final String PROJECT_NAME = "project_name";
    private static final String BAD_PROJECT_NAME = "";
    private static final Result RESULT = Result.FAILURE;

    @Mock
    private Queue.Item queueItem;
    @Mock
    private Item item;
    @Mock
    private AbstractProject targetProject;
    @Mock
    private AbstractProject<?, ?> taskProject;
    @Mock
    private Jenkins instance;
    @Mock
    private AbstractBuild<?, ?> lastBuild;
    @Mock
    private ItemGroup parent;

    @Test
    public void isBlockedProjectNotSpecified() {
        JobResultBlockQueueCondition condition = new JobResultBlockQueueCondition(null, RESULT);
        CauseOfBlockage result = condition.isBlocked(queueItem);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getShortDescription());

        condition = new JobResultBlockQueueCondition(BAD_PROJECT_NAME, RESULT);
        result = condition.isBlocked(queueItem);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getShortDescription());

        condition = new JobResultBlockQueueCondition(PROJECT_NAME, null);
        result = condition.isBlocked(queueItem);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getShortDescription());
    }

    @Test
    public void isBlockedTargetProjectIsNotAbstractProject() {
        commonExpectations(item);

        JobResultBlockQueueCondition condition = new JobResultBlockQueueCondition(PROJECT_NAME, RESULT);
        CauseOfBlockage result = condition.isBlocked(queueItem);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getShortDescription());
        Assert.assertEquals(PROJECT_NAME, condition.getProject());
    }

    @Test
    public void isBlockedGoodTargetProjectNullLastBuild() {
        commonExpectations(targetProject);
        when(targetProject.getLastBuild()).thenReturn(null);

        JobResultBlockQueueCondition condition = new JobResultBlockQueueCondition(PROJECT_NAME, RESULT);
        CauseOfBlockage result = condition.isBlocked(queueItem);
        verify(targetProject).getLastBuild();
        Assert.assertNull(result);
    }

    @Test
    public void isBlockedGoodTargetProjectSuccessfulLastBuild() {
        commonExpectations(targetProject);
        when(targetProject.getLastBuild()).thenReturn(lastBuild);
        when(lastBuild.getResult()).thenReturn(Result.SUCCESS);

        JobResultBlockQueueCondition condition = new JobResultBlockQueueCondition(PROJECT_NAME, RESULT);
        CauseOfBlockage result = condition.isBlocked(queueItem);
        verify(targetProject).getLastBuild();
        Assert.assertNull(result);
    }

    @Test
    public void isBlockedGoodTargetProjectFailedLastBuild() {
        commonExpectations(targetProject);
        when(targetProject.getLastBuild()).thenReturn(lastBuild);
        when(lastBuild.getResult()).thenReturn(Result.FAILURE);
        when(targetProject.getParent()).thenReturn(parent);
        when(lastBuild.getDisplayName()).thenReturn("name");

        JobResultBlockQueueCondition condition = new JobResultBlockQueueCondition(PROJECT_NAME, RESULT);
        CauseOfBlockage result = condition.isBlocked(queueItem);
        verify(targetProject).getLastBuild();
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getShortDescription());
    }

    @Test
    public void doCheckProjectNullProjectName() {
        JobResultBlockQueueCondition.DescriptorImpl descriptor = new JobResultBlockQueueCondition.DescriptorImpl();
        FormValidation result = descriptor.doCheckProject(null, item);
        Assert.assertEquals(FormValidation.Kind.ERROR, result.kind);
    }

    @Test
    public void doCheckProjectBadProjectName() {
        JobResultBlockQueueCondition.DescriptorImpl descriptor = new JobResultBlockQueueCondition.DescriptorImpl();
        FormValidation result = descriptor.doCheckProject(BAD_PROJECT_NAME, item);
        Assert.assertEquals(FormValidation.Kind.ERROR, result.kind);
    }

    @Test
    public void doCheckProjectGoodProject() {
        PowerMockito.mockStatic(Jenkins.class);
        when(Jenkins.getActiveInstance()).thenReturn(instance);
        when(item.getParent()).thenReturn(parent);
        when(instance.getItem(PROJECT_NAME, parent)).thenReturn(targetProject);

        JobResultBlockQueueCondition.DescriptorImpl descriptor = new JobResultBlockQueueCondition.DescriptorImpl();
        FormValidation result = descriptor.doCheckProject(PROJECT_NAME, item);
        Assert.assertEquals(FormValidation.Kind.OK, result.kind);
    }

    @Test
    public void doCheckProjectNullProject() {
        PowerMockito.mockStatic(Jenkins.class);
        when(Jenkins.getActiveInstance()).thenReturn(instance);
        when(item.getParent()).thenReturn(parent);
        when(instance.getItem(PROJECT_NAME, parent)).thenReturn(null);

        JobResultBlockQueueCondition.DescriptorImpl descriptor = new JobResultBlockQueueCondition.DescriptorImpl();
        FormValidation result = descriptor.doCheckProject(PROJECT_NAME, item);
        Assert.assertEquals(FormValidation.Kind.ERROR, result.kind);
    }

    private void commonExpectations(Item targetProject) {
        PowerMockito.mockStatic(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(instance);
        when(Jenkins.getActiveInstance()).thenReturn(instance);
        Whitebox.setInternalState(queueItem, "task", taskProject);
        when(taskProject.getParent()).thenReturn(parent);
        when(parent.getFullName()).thenReturn("name");
        when(instance.getItem(PROJECT_NAME, parent)).thenReturn(targetProject);
    }
}

package org.jenkinsci.plugins.blockqueuedjob.condition;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Queue;
import hudson.model.Run;
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
@PrepareForTest({Jenkins.class, ItemGroup.class, Run.class})
public class BuildingBlockQueueConditionTest {

    private static final String PROJECT_NAME = "project_name";
    private static final String BAD_PROJECT_NAME = "";

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
        BuildingBlockQueueCondition condition = new BuildingBlockQueueCondition(BAD_PROJECT_NAME);
        CauseOfBlockage result = condition.isBlocked(queueItem);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getShortDescription());

        condition = new BuildingBlockQueueCondition(null);
        result = condition.isBlocked(queueItem);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getShortDescription());
    }

    @Test
    public void isBlockedTargetProjectIsNotAbstractProject() {
        commonExpectations(item);

        BuildingBlockQueueCondition condition = new BuildingBlockQueueCondition(PROJECT_NAME);
        Assert.assertNotNull(condition.isBlocked(queueItem));
        Assert.assertEquals(PROJECT_NAME, condition.getProject());
    }

    @Test
    public void isBlockedGoodTargetProjectNullLastBuild() {
        commonExpectations(targetProject);
        when(targetProject.getLastBuild()).thenReturn(null);

        BuildingBlockQueueCondition condition = new BuildingBlockQueueCondition(PROJECT_NAME);
        CauseOfBlockage result = condition.isBlocked(queueItem);
        verify(targetProject).getLastBuild();
        Assert.assertNull(result);
    }

    @Test
    public void isBlockedGoodTargetProjectFinishedLastBuild() {
        commonExpectations(targetProject);
        when(targetProject.getLastBuild()).thenReturn(lastBuild);
        when(lastBuild.isBuilding()).thenReturn(false);

        BuildingBlockQueueCondition condition = new BuildingBlockQueueCondition(PROJECT_NAME);
        CauseOfBlockage result = condition.isBlocked(queueItem);
        verify(targetProject).getLastBuild();
        Assert.assertNull(result);
    }

    @Test
    public void isBlockedGoodTargetProjectStillRunningLastBuild() {
        commonExpectations(targetProject);
        when(targetProject.getLastBuild()).thenReturn(lastBuild);
        when(lastBuild.isBuilding()).thenReturn(true);
        when(targetProject.getParent()).thenReturn(parent);
        when(lastBuild.getDisplayName()).thenReturn("name");

        BuildingBlockQueueCondition condition = new BuildingBlockQueueCondition(PROJECT_NAME);
        CauseOfBlockage result = condition.isBlocked(queueItem);
        verify(targetProject).getLastBuild();
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getShortDescription());
    }

    @Test
    public void doCheckProjectNullProjectName() {
        BuildingBlockQueueCondition.DescriptorImpl descriptor = new BuildingBlockQueueCondition.DescriptorImpl();
        FormValidation result = descriptor.doCheckProject(null, item);
        Assert.assertEquals(FormValidation.Kind.ERROR, result.kind);
    }

    @Test
    public void doCheckProjectBadProjectName() {
        BuildingBlockQueueCondition.DescriptorImpl descriptor = new BuildingBlockQueueCondition.DescriptorImpl();
        FormValidation result = descriptor.doCheckProject(BAD_PROJECT_NAME, item);
        Assert.assertEquals(FormValidation.Kind.ERROR, result.kind);
    }

    @Test
    public void doCheckProjectGoodProject() {
        PowerMockito.mockStatic(Jenkins.class);
        when(Jenkins.getActiveInstance()).thenReturn(instance);
        when(item.getParent()).thenReturn(parent);
        when(instance.getItem(PROJECT_NAME, parent)).thenReturn(targetProject);

        BuildingBlockQueueCondition.DescriptorImpl descriptor = new BuildingBlockQueueCondition.DescriptorImpl();
        FormValidation result = descriptor.doCheckProject(PROJECT_NAME, item);
        Assert.assertEquals(FormValidation.Kind.OK, result.kind);
    }

    @Test
    public void doCheckProjectNullProject() {
        PowerMockito.mockStatic(Jenkins.class);
        when(Jenkins.getActiveInstance()).thenReturn(instance);
        when(item.getParent()).thenReturn(parent);
        when(instance.getItem(PROJECT_NAME, parent)).thenReturn(null);

        BuildingBlockQueueCondition.DescriptorImpl descriptor = new BuildingBlockQueueCondition.DescriptorImpl();
        FormValidation result = descriptor.doCheckProject(PROJECT_NAME, item);
        Assert.assertEquals(FormValidation.Kind.ERROR, result.kind);
    }

    private void commonExpectations(Item targetProject) {
        PowerMockito.mockStatic(Jenkins.class);
        when(Jenkins.getActiveInstance()).thenReturn(instance);
        Whitebox.setInternalState(queueItem, "task", taskProject);
        when(taskProject.getParent()).thenReturn(parent);
        when(parent.getFullName()).thenReturn("name");
        when(instance.getItem(PROJECT_NAME, parent)).thenReturn(targetProject);
    }
}

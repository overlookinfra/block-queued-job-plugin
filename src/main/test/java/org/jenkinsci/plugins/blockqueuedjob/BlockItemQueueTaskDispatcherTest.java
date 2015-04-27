package org.jenkinsci.plugins.blockqueuedjob;

import hudson.model.*;
import hudson.model.queue.CauseOfBlockage;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.blockqueuedjob.condition.BlockQueueCondition;
import org.jenkinsci.plugins.blockqueuedjob.condition.JobResultBlockQueueCondition;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by Alina_Karpovich on 4/27/2015.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class, ItemGroup.class, AbstractBuild.class})
public class BlockItemQueueTaskDispatcherTest {

    @Mock private Queue.Item queueItem;
    @Mock private Item item;
    @Mock private AbstractProject project;
    @Mock private BlockItemJobProperty blockProperty;
    @Mock private BlockQueueCondition unblockingCondition;
    @Mock private BlockQueueCondition blockingCondition;
    @Mock private Queue.Task task;

    @Test
    public void canRunTaskNotProject() {
        Whitebox.setInternalState(queueItem, "task", task);

        BlockItemQueueTaskDispatcher dispatcher = new BlockItemQueueTaskDispatcher();
        CauseOfBlockage result = dispatcher.canRun(queueItem);
        Assert.assertNull(result);
    }

    @Test
    public void canRunNullBlockProperty() {
        Whitebox.setInternalState(queueItem, "task", project);
        when(project.getProperty(BlockItemJobProperty.class)).thenReturn(null);

        BlockItemQueueTaskDispatcher dispatcher = new BlockItemQueueTaskDispatcher();
        CauseOfBlockage result = dispatcher.canRun(queueItem);
        verify(project).getProperty(BlockItemJobProperty.class);
        Assert.assertNull(result);
    }

    @Test
    public void canRunNullConditionsList() {
        Whitebox.setInternalState(queueItem, "task", project);
        when(project.getProperty(BlockItemJobProperty.class)).thenReturn(blockProperty);
        when(blockProperty.getConditions()).thenReturn(null);

        BlockItemQueueTaskDispatcher dispatcher = new BlockItemQueueTaskDispatcher();
        CauseOfBlockage result = dispatcher.canRun(queueItem);
        verify(blockProperty).getConditions();
        Assert.assertNull(result);
    }

    @Test
    public void canRunUnblock() {
        Whitebox.setInternalState(queueItem, "task", project);
        when(project.getProperty(BlockItemJobProperty.class)).thenReturn(blockProperty);
        when(blockProperty.getConditions()).thenReturn(makeConditions());
        when(unblockingCondition.isUnblocked(queueItem)).thenReturn(true);

        BlockItemQueueTaskDispatcher dispatcher = new BlockItemQueueTaskDispatcher();
        CauseOfBlockage result = dispatcher.canRun(queueItem);
        verify(unblockingCondition).isUnblocked(queueItem);
        Assert.assertNull(result);
    }

    @Test
    public void canRunBlock() {
        final String message = "blocked due to test reason";
        Whitebox.setInternalState(queueItem, "task", project);
        when(project.getProperty(BlockItemJobProperty.class)).thenReturn(blockProperty);
        when(blockProperty.getConditions()).thenReturn(makeConditions());
        when(blockingCondition.isBlocked(queueItem)).thenReturn(new CauseOfBlockage() {
            @Override
            public String getShortDescription() {
                return message;
            }
        });

        BlockItemQueueTaskDispatcher dispatcher = new BlockItemQueueTaskDispatcher();
        CauseOfBlockage result = dispatcher.canRun(queueItem);
        verify(blockingCondition).isBlocked(queueItem);
        Assert.assertNotNull(result);
        Assert.assertEquals(message, result.getShortDescription());
    }

    private List<BlockQueueCondition> makeConditions() {
        List<BlockQueueCondition> conditions = new ArrayList<>();
        conditions.add(unblockingCondition);
        conditions.add(blockingCondition);
        return conditions;
    }
}

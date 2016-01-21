package org.jenkinsci.plugins.blockqueuedjob;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.QueueTaskDispatcher;
import org.jenkinsci.plugins.blockqueuedjob.condition.BlockQueueCondition;

import java.util.List;

/**
 * Blocks item from execution according to configuration in JobProperty
 *
 * @author Kanstantsin Shautsou
 */
@Extension
public class BlockItemQueueTaskDispatcher extends QueueTaskDispatcher {
    @Override
    public CauseOfBlockage canRun(Queue.Item item) {
        if (item.task instanceof Job<?, ?>) {
            Job<?, ?> job = (Job<?, ?>) item.task;

            final BlockItemJobProperty property = job.getProperty(BlockItemJobProperty.class);
            if (property != null) {
                List<BlockQueueCondition> conditions = property.getConditions();
                if (conditions != null) {
                    // first matched win
                    for (BlockQueueCondition condition : conditions) {
                        if (condition.isUnblocked(item)) {
                            return null; // unblock
                        }

                        final CauseOfBlockage blocked = condition.isBlocked(item);
                        if (blocked != null) {
                            return blocked; // block
                        }
                    }
                }
            }
        }

        return super.canRun(item);
    }
}

package org.jenkinsci.plugins.blockqueuedjob.condition;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;
import jenkins.model.Jenkins;

/**
 * Abstract class for block/unblock conditions
 *
 * @author Kanstantsin Shautsou
 */
abstract public class BlockQueueCondition implements Describable<BlockQueueCondition>, ExtensionPoint {

    /**
     * @return not null for blocking job with description
     */
    public CauseOfBlockage isBlocked(Queue.Item item) {
        return null;
    }

    /**
     * condition for unblocking
     *
     * @return false - nothing, true - need unblock
     */
    public boolean isUnblocked(Queue.Item item) {
        return false;
    }

    public Descriptor<BlockQueueCondition> getDescriptor() {
        return (BlockQueueConditionDescriptor) Jenkins.getActiveInstance().getDescriptorOrDie(getClass());
    }

    /**
     * Abstract Descriptor
     */
    abstract public static class BlockQueueConditionDescriptor extends Descriptor<BlockQueueCondition> {
        /**
         * @return all available blocking conditions
         */
        public static DescriptorExtensionList<BlockQueueCondition, BlockQueueConditionDescriptor> all() {
            return Jenkins.getActiveInstance().getDescriptorList(BlockQueueCondition.class);
        }
    }
}

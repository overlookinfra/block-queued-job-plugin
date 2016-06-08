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
        if (needsAdditionalNodes(item)) {
          if (allocateAdditionalNodes(item)) {
            return null; // unblock
          } else {
            return new CauseOfBlockage() {
              @Override
              public String getShortDescription() {
                return "Unable to allocate additional nodes!";
              }
            };
          }
        } else {
          return null; // unblock
        }
        return super.canRun(item);
    }

    public boolean needsAdditionalNodes(Queue.Item item) {
      return false;
      // TODO: check job for data indicating vmpooler nodes
    }

    public boolean allocateAdditionalNodes(Queue.Item item) {
      return true;
      // TODO: attempt to allocate vmpooler nodes
      // TODO: store vmpooler node information with item
    }
}

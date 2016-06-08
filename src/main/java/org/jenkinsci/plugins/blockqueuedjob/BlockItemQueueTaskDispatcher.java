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
          return super.canRun(item);
        }
    }

    public boolean needsAdditionalNodes(Queue.Item item) {
      return true;
      // TODO: check job for data indicating vmpooler nodes
    }

    public boolean allocateAdditionalNodes(Queue.Item item) {
      return false;
      // TODO: attempt to allocate vmpooler nodes
      // TODO: store vmpooler node information with item
    }

  /**
   Given a Queue.Item and a String key, return the value associated with
   that key. This method is intended to opaquely handle the difference
   between Jenkins project/job types for the sake of determining
   build-specific values for the Queue.Item

   - For FreeStyleProject builds, check the values of build parameters by
     inspecting their build-time values in the list of Actions on the
     Queue.Item.
   - For Matrix "cell" builds, check the hudson.matrix.Combination to get
     its current hudson.matrix.Axis values.
   */
  private String getBuildVariable(Queue.Item item, String key) {
      if (item.task instanceof FreeStyleProject) {
          return (String)getBuildParameterValue(item, key);
      } else if (item.task instanceof MatrixConfiguration) {
          return getCombinationAxisValue(item, key);
      }
      return null;
  }

  /**
     Given a Queue.Item and a String key, return the value (or null) of the
     build parameter named by the key for that item. We return Object to avoid
     assumptions about the type of the actual build parameter setting and
     expectations of the calling context.
   */
  private Object getBuildParameterValue(Queue.Item item, String parameterName) {
      ParametersAction pAction = item.getAction(ParametersAction.class);
      ParameterValue pValue = pAction.getParameter(parameterName);

      return pValue.getValue();
  }

  /**
     Given a Queue.Item and a String naming a MatrixProject Axis, return the
     value of that axis variable for this particular MatrixConfiguration.
   */
  private String getCombinationAxisValue(Queue.Item item, String axisName) {
      MatrixConfiguration matrixConfig = (MatrixConfiguration)item.task;
      Combination combo = matrixConfig.getCombination();

      if (combo.containsKey(axisName)){
          return combo.get(axisName);
      }
      return null;
  }

  /**
     On a given Queue.Item, set the value for a build parameter named
     parameterName to the value specified by parameterValue. This will add a
     new build parameter if it did not previously exist and override it if it
     does.
   */
  private void setBuildParameterValue(Queue.Item item, String parameterName, String parameterValue) {
      StringParameterValue sValue = new StringParameterValue(parameterName, parameterValue);

      ParametersAction oldAction = item.getAction(ParametersAction.class);
      ParametersAction newAction = oldAction.merge(new ParametersAction(sValue));

      // Since there can be only one type of a particular action
      // (ParametersAction) on a given Actionable, calling replaceAction below
      // replaces that only instance with the new one we just created.
      item.replaceAction(newAction);
  }
}

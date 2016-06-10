package org.jenkinsci.plugins.blockqueuedjob;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.ParameterValue;
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.QueueTaskDispatcher;
import org.apache.commons.io.IOUtils;
import hudson.model.StringParameterValue;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.Combination;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Date;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Blocks item from execution according to configuration in JobProperty
 *
 * @author Kanstantsin Shautsou
 */
@Extension
public class BlockItemQueueTaskDispatcher extends QueueTaskDispatcher {
    @Override
    public CauseOfBlockage canRun(final Queue.Item item) {
        if (needsAdditionalNodes(item)) {
          if (allocateAdditionalNodes(item)) {
            return null; // unblock
          } else {
            return new CauseOfBlockage() {
              @Override
              public String getShortDescription() {
                String current_time = new Date().toString();
                return current_time + ": Looking to allocate TEST_TARGET[" + getBuildVariable(item, "TEST_TARGET") + "]";
                //
                // return "Unable to allocate additional nodes!";
              }
            };
          }
        } else {
          return super.canRun(item);
        }
    }

    public boolean needsAdditionalNodes(Queue.Item item) {

      if (item.task instanceof AbstractProject && !(item.task instanceof MatrixProject)) {
          // TODO: check job for data indicating vmpooler nodes
          return (getBuildParameterValue(item, "VMPOOLER_HOSTNAME") == null);
      } else {
        return false;
      }
    }

    public boolean allocateAdditionalNodes(Queue.Item item) {
        Map<String, String> templates = new HashMap<>();
        templates.put(getBuildVariable(item, "TEST_TARGET"), "1");

        JSONObject data = new JSONObject(templates);

        try {
            Map<String, List<String>> vms = parseResults(post(data.toString()));
            for (Iterator<List<String>> it = vms.values().iterator(); it.hasNext(); ) {
                List<String> hostnames = it.next();
                for (String hostname : hostnames) {
                    setBuildParameterValue(item, "VMPOOLER_HOSTNAME", hostname);
                }
            }
            return true;
        } catch (IOException e) {
            // TODO: differentiate between pool is empty vs requested VM doesn't exist
            System.err.println("Failed to get VM: " + e.toString());
            return false;
        }
    }

    private String post(String data) throws IOException {
        Process p = new ProcessBuilder("curl", "-f", "-d", data, "--url", "vmpooler.delivery.puppetlabs.net/api/v1/vm").start();

        InputStream in = p.getInputStream();
        try {
            return IOUtils.toString(in, "UTF-8");
        } finally {
            in.close();
        }
    }

    private static Map<String, List<String>> parseResults(String json) throws IOException {
        Map<String, List<String>> vms = new HashMap<>();

        JSONObject obj = new JSONObject(json);
        if (!obj.getBoolean("ok")) {
            throw new IOException("VMPooler request failed");
        }

        Set<String> platforms = obj.keySet();
        platforms.remove("ok");
        platforms.remove("domain");

        for (String platform : platforms) {
            List<String> hostnames = new ArrayList<>();

            JSONObject hosts = obj.getJSONObject(platform);
            try {
                String hostname = hosts.getString("hostname");
                //System.out.println("hostname: " + hostname + " (" + platform + ")");
                hostnames.add(hostname);
            } catch (JSONException e) {
                JSONArray arr = hosts.getJSONArray("hostname");
                for (Iterator it = arr.iterator(); it.hasNext(); ) {
                    String hostname = (String)it.next();
                    //System.out.println("hostname: " + hostname + " (" + platform + ")");
                    hostnames.add(hostname);
                }
            }

            vms.put(platform, hostnames);
        }

        return vms;
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
  public String getBuildVariable(Queue.Item item, String key) {
      if (item.task instanceof FreeStyleProject) {
          return (String)getBuildParameterValue(item, key);
      } else if (item.task instanceof MatrixConfiguration) {
          return getCombinationAxisValue(item, key);
      } else {
        return "unknown class: " + item.task.getClass().getName();
      }
  }

  /**
     Given a Queue.Item and a String key, return the value (or null) of the
     build parameter named by the key for that item. We return Object to avoid
     assumptions about the type of the actual build parameter setting and
     expectations of the calling context.
   */
  public Object getBuildParameterValue(Queue.Item item, String parameterName) {
      ParametersAction pAction = item.getAction(ParametersAction.class);
      if (pAction != null) {
          ParameterValue pValue = pAction.getParameter(parameterName);
          if (pValue != null) {
              return pValue.getValue();
          }
      }
      return null;
  }

  /**
     Given a Queue.Item and a String naming a MatrixProject Axis, return the
     value of that axis variable for this particular MatrixConfiguration.
   */
  public String getCombinationAxisValue(Queue.Item item, String axisName) {
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
  public void setBuildParameterValue(Queue.Item item, String parameterName, String parameterValue) {
      StringParameterValue sValue = new StringParameterValue(parameterName, parameterValue);

      ParametersAction oldAction = item.getAction(ParametersAction.class);
      ParametersAction newAction = oldAction.merge(new ParametersAction(sValue));

      // Since there can be only one type of a particular action
      // (ParametersAction) on a given Actionable, calling replaceAction below
      // replaces that only instance with the new one we just created.
      item.replaceAction(newAction);
  }
}

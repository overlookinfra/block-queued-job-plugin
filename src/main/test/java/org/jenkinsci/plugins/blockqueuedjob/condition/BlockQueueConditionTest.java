package org.jenkinsci.plugins.blockqueuedjob.condition;

import hudson.Functions;
import hudson.model.*;
import org.jenkinsci.plugins.blockqueuedjob.BlockItemJobProperty;
import org.jenkinsci.plugins.blockqueuedjob.condition.BlockQueueCondition;
import org.jenkinsci.plugins.blockqueuedjob.condition.BuildingBlockQueueCondition;
import org.jenkinsci.plugins.blockqueuedjob.condition.JobResultBlockQueueCondition;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BlockQueueConditionTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    private boolean origDefaultUseCache = true;

    @Before
    public void setUp() throws Exception {
        if(Functions.isWindows()) {
            // To avoid JENKINS-4409.
            // URLConnection caches handles to jar files by default,
            // and it prevents delete temporary directories.
            // Disable caching here.
            // Though defaultUseCache is a static field,
            // its setter and getter are provided as instance methods.
            URLConnection aConnection = new File(".").toURI().toURL().openConnection();
            origDefaultUseCache = aConnection.getDefaultUseCaches();
            aConnection.setDefaultUseCaches(false);
        }
    }

    @Test
    public void resultConditionBlockIfJobNotExist() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("test-job");
        FreeStyleProject project2 = j.createFreeStyleProject("test-job2");

        final String unexistingJob = "y67";
        final JobResultBlockQueueCondition condition = new JobResultBlockQueueCondition(unexistingJob, Result.UNSTABLE);
        final ArrayList<BlockQueueCondition> describables = new ArrayList<>();
        describables.add(condition);
        final BlockItemJobProperty blockItemJobProperty = new BlockItemJobProperty(describables);
        project.addProperty(blockItemJobProperty);

        j.getInstance().getQueue().schedule(project, 0);
        j.getInstance().getQueue().schedule(project2, 0);

        Thread.sleep(1000);

        Queue.Item item = j.getInstance().getQueue().getItem(project);
        Queue.Item item2 = j.getInstance().getQueue().getItem(project2);

        assertNotNull("Item must be in queue" , item);
        assertTrue("Item must be blocked", item.isBlocked());
        Assert.assertEquals("Expected CauseOfBlockage to be returned", "Job " + unexistingJob +" not exist", item.getWhy());

        assertNull("Item2 mustn't be in queue", item2);
    }

    @Test
    public void lastBuildConditionBlockIfJobNotExist() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("test-job");
        FreeStyleProject project2 = j.createFreeStyleProject("test-job2");

        final String unexistingJob = "y67";
        final BuildingBlockQueueCondition condition = new BuildingBlockQueueCondition(unexistingJob);
        final ArrayList<BlockQueueCondition> describables = new ArrayList<>();
        describables.add(condition);
        final BlockItemJobProperty blockItemJobProperty = new BlockItemJobProperty(describables);
        project.addProperty(blockItemJobProperty);

        j.getInstance().getQueue().schedule(project, 0);
        j.getInstance().getQueue().schedule(project2, 0);

        Thread.sleep(1000);

        Queue.Item item = j.getInstance().getQueue().getItem(project);
        Queue.Item item2 = j.getInstance().getQueue().getItem(project2);

        assertNotNull("Item must be in queue" , item);
        assertTrue("Item must be blocked", item.isBlocked());
        Assert.assertEquals("Expected CauseOfBlockage to be returned",
                BuildingBlockQueueCondition.class.getSimpleName() + ": Job " + unexistingJob +" not exist", item.getWhy());

        assertNull("Item2 mustn't be in queue", item2);
    }

    @After
    public void tearDown() throws Exception {
        if(Functions.isWindows()) {
            URLConnection aConnection = new File(".").toURI().toURL().openConnection();
            aConnection.setDefaultUseCaches(origDefaultUseCache);
        }
    }
}

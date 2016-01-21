package org.jenkinsci.plugins.blockqueuedjob.utils;

import jenkins.model.Jenkins;

/**
 * Various helpers
 *
 * @author Kanstantsin Shautsou
 */
public class Utils {

    @Deprecated
    public static Jenkins getActiveJenkinsInstance() {
        Jenkins instance = Jenkins.getInstance();
        if (instance == null) {
            throw new IllegalStateException("Jenkins instance does not exist");
        }

        return instance;
    }

}

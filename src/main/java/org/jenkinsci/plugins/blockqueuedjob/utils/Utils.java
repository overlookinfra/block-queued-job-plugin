package org.jenkinsci.plugins.blockqueuedjob.utils;

import jenkins.model.Jenkins;

/**
 * Various helpers
 *
 * @author Kanstantsin Shautsou
 */
public class Utils {

    public static Jenkins getJenkinsInstance() {
        Jenkins instance = Jenkins.getInstance();
        if (instance == null) {
            throw new IllegalStateException("Jenkins instance is not exist");
        }

        return instance;
    }

}

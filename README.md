# block-queued-job-plugin

This plugin was derived from the [block-queued-job-plugin](https://github.com/jenkinsci/block-queued-job-plugin) and modified for stopping job execution until scheduled resources are available.

Local plugin development instructions:

```
git clone https://github.com/puppetlabs/block-queued-job-plugin
cd block-queued-job-plugin
vagrant up
vagrant ssh
cd block-queued-job-plugin
mvn hpi:run
```

- hit http://localhost:8080/jenkins/
  - note that the initial setup for first-time run may take a few minutes to present you with an active Jenkins
- make sure the Dynamic Axis plugin is installed
- Jenkins > New Item
  - put in job name
  - select multi-configuration project
  - configure the job:
    - this build is parameterized
    - App Parameter > String Parameter:  TEST_TARGETS, redhat7-64a redhat6-64a
    - Configuration Matrix > Add Axis > Dynamic Axis: TEST_TARGET, TEST_TARGETS
    - Build > Add Build Step > Execute Shell: (put in an `echo "hello world"` shell script)
    - Save

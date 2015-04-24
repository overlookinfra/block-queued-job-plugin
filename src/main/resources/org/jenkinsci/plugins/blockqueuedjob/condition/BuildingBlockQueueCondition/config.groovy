package org.jenkinsci.plugins.blockqueuedjob.condition.BuildingBlockQueueCondition

def f = namespace(lib.FormTagLib);

f.entry(field: "project", title: "Depends on job" ) {
    f.textbox()
}

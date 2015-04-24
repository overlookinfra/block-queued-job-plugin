package org.jenkinsci.plugins.blockqueuedjob.condition.BuildingBlockQueueCondition

def f = namespace(lib.FormTagLib);

f.entry(title: "Depends on job", field: "project") {
    f.textbox()
}

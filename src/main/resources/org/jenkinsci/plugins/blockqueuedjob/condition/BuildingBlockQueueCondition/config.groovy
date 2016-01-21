package org.jenkinsci.plugins.blockqueuedjob.condition.BuildingBlockQueueCondition

import lib.FormTagLib

def f = namespace(FormTagLib);

f.entry(title: "Depends on job", field: "project") {
    f.textbox()
}

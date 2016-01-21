package org.jenkinsci.plugins.blockqueuedjob.condition.JobResultBlockQueueCondition

import lib.FormTagLib

def f = namespace(FormTagLib);

f.entry(field: "project", title: "Depends on job") {
    f.textbox()
}

f.entry(title: "Block when last build result is worse or equal to", field: "result") {
    f.select()
}

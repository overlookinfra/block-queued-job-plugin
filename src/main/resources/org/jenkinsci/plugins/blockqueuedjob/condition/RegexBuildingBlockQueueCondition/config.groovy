package org.jenkinsci.plugins.blockqueuedjob.condition.RegexBuildingBlockQueueCondition

def f = namespace(lib.FormTagLib);

f.entry(field: "regexStr", title: "Blocker patterns") {
    f.textarea()
}

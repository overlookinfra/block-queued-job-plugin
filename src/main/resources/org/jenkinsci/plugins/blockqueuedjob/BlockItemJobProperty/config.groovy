package org.jenkinsci.plugins.blockqueuedjob.BlockItemJobProperty

import org.jenkinsci.plugins.blockqueuedjob.condition.BlockQueueCondition

def f = namespace(lib.FormTagLib);
def conditions = (instance == null ? [] : instance.conditions)

f.optionalBlock(title: "Block/Unblock task in queue",
        name: "hasBlockedJobProperty",
        inline: true,
        checked: (instance != null)
){

    f.entry() {
        f.hetero_list(name: "conditions",
                items: conditions,
                descriptors: BlockQueueCondition.BlockQueueConditionDescriptor.all(),
                hasHeader: true,
                addCaption: "Add Condition"
        )
    }
}

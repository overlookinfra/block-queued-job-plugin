package org.jenkinsci.plugins.blockqueuedjob.BlockItemJobProperty

import lib.FormTagLib
import org.jenkinsci.plugins.blockqueuedjob.condition.BlockQueueCondition

def f = namespace(FormTagLib);
def conditions = (instance == null ? [] : instance.conditions)

f.optionalBlock(title: "Block/Unblock task in queue",
        name: "hasBlockedJobProperty",
        inline: true,
        checked: (instance != null),
        help: descriptor.helpFile
) {
    f.entry(help: descriptor.getHelpFile("conditions")) {
        f.hetero_list(name: "conditions",
                items: conditions,
                descriptors: BlockQueueCondition.BlockQueueConditionDescriptor.all(),
                hasHeader: true,
                addCaption: "Add Condition"
        )
    }
}

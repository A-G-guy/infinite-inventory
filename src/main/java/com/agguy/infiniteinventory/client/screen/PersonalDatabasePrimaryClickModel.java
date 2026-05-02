package com.agguy.infiniteinventory.client.screen;

final class PersonalDatabasePrimaryClickModel {
    private PersonalDatabasePrimaryClickModel() {
    }

    /**
     * 解析仓库面板物品条目上「主点击（左键）」的语义。
     *
     * <p>仲裁优先级：Shift &gt; Alt &gt; Ctrl，由分支顺序自然表达。
     * <ul>
     *     <li>Shift：取一组到背包（数量·批量）</li>
     *     <li>Alt：取一个到背包（数量·细粒度）</li>
     *     <li>Ctrl：增量多选（选择维度）</li>
     *     <li>无修饰键：替换式选择</li>
     * </ul>
     */
    static Action resolve(boolean shiftDown, boolean altDown, boolean controlDown) {
        if (shiftDown) {
            return Action.TAKE_STACK_TO_INVENTORY;
        }
        if (altDown) {
            return Action.TAKE_SINGLE_TO_INVENTORY;
        }
        return controlDown ? Action.START_ADDITIVE_SELECTION : Action.START_REPLACE_SELECTION;
    }

    enum Action {
        TAKE_STACK_TO_INVENTORY,
        TAKE_SINGLE_TO_INVENTORY,
        START_ADDITIVE_SELECTION,
        START_REPLACE_SELECTION
    }
}

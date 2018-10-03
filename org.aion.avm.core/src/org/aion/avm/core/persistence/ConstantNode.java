package org.aion.avm.core.persistence;

import org.aion.avm.core.NodeEnvironment;
import org.aion.avm.shadow.java.lang.Object;


/**
 * The data required to describe a constant reference, in the serialization layer.
 */
public class ConstantNode implements INode {
    public final long constantId;

    public ConstantNode(long constantId) {
        this.constantId = constantId;
    }

    @Override
    public Object getObjectInstance() {
        return NodeEnvironment.singleton.getConstantMap().get(this.constantId);
    }

    @Override
    public int getBillableReferenceSize() {
        // Encode the constant stub constant as an int.
        int constantSize = ByteSizes.INT;
        // Then encode the instanceId as a long.
        int instanceIdSize = ByteSizes.LONG;
        return constantSize + instanceIdSize;
    }
}
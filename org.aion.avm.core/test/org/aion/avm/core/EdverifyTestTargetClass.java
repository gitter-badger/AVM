package org.aion.avm.core;

import org.aion.avm.api.ABIDecoder;
import org.aion.avm.api.BlockchainRuntime;


public class EdverifyTestTargetClass {

    private EdverifyTestTargetClass(){
        // initialize to default
    }

    public boolean callEdverify(byte[] message, byte[] signature, byte[]pk) throws IllegalArgumentException {
        return BlockchainRuntime.edVerify(message, signature, pk);
    }


    private static org.aion.avm.core.EdverifyTestTargetClass testTarget;

    /**
     * Initialization code executed once at the Dapp deployment.
     */
    static {
        testTarget = new org.aion.avm.core.EdverifyTestTargetClass();
    }

    /**
     * Entry point at a transaction call.
     */
    public static byte[] main() {
        return ABIDecoder.decodeAndRunWithObject(testTarget, BlockchainRuntime.getData());
    }
}

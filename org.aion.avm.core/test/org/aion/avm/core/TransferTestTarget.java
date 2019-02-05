package org.aion.avm.core;

import org.aion.avm.api.ABIDecoder;
import org.aion.avm.api.Address;
import org.aion.avm.api.BlockchainRuntime;

import java.math.BigInteger;

public class TransferTestTarget {

    public void doTransfer(byte[] to, long balance, long energyLimit){
       Address receiverAddress = new Address(to);
       BlockchainRuntime.transfer(receiverAddress, BigInteger.valueOf(balance), energyLimit);
    }

    public void callAnotherDappToTransfer(byte[] to, byte[] args, long energyLimit){
        Address receiverAddress = new Address(to);
        BlockchainRuntime.call(receiverAddress, BigInteger.ZERO, args, energyLimit);
    }

    private static org.aion.avm.core.TransferTestTarget testTarget;

    /**
     * Initialization code executed once at the Dapp deployment.
     */
    static {
        testTarget = new org.aion.avm.core.TransferTestTarget();
    }

    /**
     * Entry point at a transaction call.
     */
    public static byte[] main() {
        return ABIDecoder.decodeAndRunWithObject(testTarget, BlockchainRuntime.getData());
    }
}

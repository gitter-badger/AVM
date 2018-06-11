package org.aion.avm.core.testWallet;

import org.aion.avm.rt.Address;
import org.aion.avm.rt.BlockchainRuntime;


/**
 * We define this as an extension of BlockchainRuntime since these mechanisms probably need to be pushed back into it, at some point.
 */
public interface IFutureRuntime extends BlockchainRuntime {
    public long getBlockEpochSeconds();

    public byte[] getMessageData();

    public long getBlockNumber();

    // Note that this response is always 32 bytes.  User-space might want to wrap it.  Should we wrap it on the runtime interface level?
    public byte[] sha3(byte[] data);

    public void selfDestruct(Address beneficiary);

    public byte[] call(Address targetAddress, long energyToSend, byte[] payload);
}

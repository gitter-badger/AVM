package org.aion.avm.core.blockchainruntime;

import org.aion.avm.core.persistence.keyvalue.StorageKeys;
import org.aion.avm.core.util.AvmRule;
import org.aion.kernel.AvmTransactionResult;
import org.aion.kernel.KernelInterfaceImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.*;


public class RevertAndInvalidTest {
    @Rule
    public AvmRule avmRule = new AvmRule(false);

    // transaction
    private org.aion.vm.api.interfaces.Address deployer = KernelInterfaceImpl.PREMINED_ADDRESS;
    private long energyLimit = 1_000_000L;
    private long energyPrice = 1L;

    private org.aion.vm.api.interfaces.Address dappAddress;

    @Before
    public void setup() {
        dappAddress = deploy();
    }

    private org.aion.vm.api.interfaces.Address deploy() {
        byte[] arguments = null;
        return avmRule.deploy(deployer, BigInteger.ZERO, avmRule.getDappBytes(RevertAndInvalidTestResource.class, arguments), energyLimit, energyPrice).getDappAddress();
    }

    @Test
    public void testRevert() {
        AvmTransactionResult txResult = (AvmTransactionResult) avmRule.call(deployer, dappAddress, BigInteger.ZERO, new byte[]{1}, energyLimit, energyPrice).getTransactionResult();

        assertEquals(AvmTransactionResult.Code.FAILED_REVERT, txResult.getResultCode());
        assertNull(txResult.getReturnData());
        assertTrue(energyLimit > txResult.getEnergyUsed());
        assertTrue(0 < txResult.getEnergyRemaining());

        assertArrayEquals(new byte[]{0,0,0,0, 0,0,0,4, 0,0,0,0}, avmRule.kernel.getStorage(dappAddress, StorageKeys.CLASS_STATICS));
    }

    @Test
    public void testInvalid() {
        AvmTransactionResult txResult = (AvmTransactionResult) avmRule.call(deployer, dappAddress, BigInteger.ZERO, new byte[]{2}, energyLimit, energyPrice).getTransactionResult();
        assertEquals(AvmTransactionResult.Code.FAILED_INVALID, txResult.getResultCode());
        assertNull(txResult.getReturnData());
        assertEquals(energyLimit, txResult.getEnergyUsed());
        assertEquals(0, txResult.getEnergyRemaining());

        assertArrayEquals(new byte[]{0,0,0,0, 0,0,0,4, 0,0,0,0}, avmRule.kernel.getStorage(dappAddress, StorageKeys.CLASS_STATICS));
    }

}

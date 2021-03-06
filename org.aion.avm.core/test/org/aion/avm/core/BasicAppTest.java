package org.aion.avm.core;

import org.aion.avm.api.ABIEncoder;
import org.aion.avm.core.util.AvmRule;
import org.aion.avm.core.util.TestingHelper;
import org.aion.avm.userlib.AionList;
import org.aion.avm.userlib.AionMap;
import org.aion.avm.userlib.AionSet;
import org.aion.kernel.KernelInterfaceImpl;
import org.aion.vm.api.interfaces.TransactionResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigInteger;


/**
 * As part of issue-77, we want to see what a more typical application can see, from inside our environment.
 * This test operates on BasicAppTestTarget to observe what we are doing, from the inside.
 * Eventually, this will change into a shape where we will use the standard AvmImpl to completely run this
 * life-cycle, but we want to prove that it works, in isolation, before changing its details to account for
 * this design (especially considering that the entry-point interface is likely temporary).
 */
public class BasicAppTest {
    @Rule
    public AvmRule avmRule = new AvmRule(false);

    private org.aion.vm.api.interfaces.Address from = KernelInterfaceImpl.PREMINED_ADDRESS;
    private org.aion.vm.api.interfaces.Address dappAddr;

    private long energyLimit = 6_000_0000;
    private long energyPrice = 1;

    @Before
    public void setup() {
        byte[] txData = avmRule.getDappBytes(BasicAppTestTarget.class, null, AionMap.class, AionSet.class, AionList.class);
        dappAddr = avmRule.deploy(from, BigInteger.ZERO, txData, energyLimit, energyPrice).getDappAddress();
    }


    @Test
    public void testIdentity() {
        byte[] txData = ABIEncoder.encodeMethodArguments("identity", new byte[] {42, 13});
        TransactionResult result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getTransactionResult();

        // These should be the same instance.
        Assert.assertEquals(42, ((byte[]) TestingHelper.decodeResult(result))[0]);
        Assert.assertEquals(13, ((byte[]) TestingHelper.decodeResult(result))[1]);
    }

    @Test
    public void testSumInput() {
        byte[] txData = ABIEncoder.encodeMethodArguments("sum", new byte[] {42, 13});
        TransactionResult result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getTransactionResult();

        // Should be just 1 byte, containing the sum.
        Assert.assertEquals(42 + 13, TestingHelper.decodeResult(result));
    }

    /**
     * This test makes multiple calls to the same contract instance, proving that static state survives between the calls.
     * It is mostly just a test to make sure that this property continues to be true, in the future, once we decide how
     * to save and resume state.
     */
    @Test
    public void testRepeatedSwaps() {
        byte[] txData = ABIEncoder.encodeMethodArguments("swapInputs", 1);
        TransactionResult result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getTransactionResult();

        Assert.assertEquals(0, TestingHelper.decodeResult(result));

        txData = ABIEncoder.encodeMethodArguments("swapInputs", 2);
        result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getTransactionResult();

        Assert.assertEquals(1, TestingHelper.decodeResult(result));

        txData = ABIEncoder.encodeMethodArguments("swapInputs", 1);
        result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getTransactionResult();

        Assert.assertEquals(2, TestingHelper.decodeResult(result));
    }

    @Test
    public void testArrayEquality() {
        byte[] txData = ABIEncoder.encodeMethodArguments("arrayEquality", new byte[] {42, 13});
        TransactionResult result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getTransactionResult();

        Assert.assertEquals(false, TestingHelper.decodeResult(result));

        txData = ABIEncoder.encodeMethodArguments("arrayEquality", new byte[] {5, 6, 7, 8});
        result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getTransactionResult();

        Assert.assertEquals(false, TestingHelper.decodeResult(result));
    }

    @Test
    public void testAllocateArray() {
        byte[] txData = ABIEncoder.encodeMethodArguments("allocateObjectArray");
        TransactionResult result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getTransactionResult();

        Assert.assertEquals(2, TestingHelper.decodeResult(result));
    }

    @Test
    public void testByteAutoboxing() {
        byte[] txData = ABIEncoder.encodeMethodArguments("byteAutoboxing", (byte) 42);
        TransactionResult result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getTransactionResult();

        Assert.assertEquals(42, ((byte[]) TestingHelper.decodeResult(result))[0]);
        Assert.assertEquals(42, ((byte[]) TestingHelper.decodeResult(result))[1]);
    }

    @Test
    public void testMapInteraction() {
        byte[] txData = ABIEncoder.encodeMethodArguments("mapPut", (byte)1, (byte)42);
        TransactionResult result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getTransactionResult();

        Assert.assertEquals((byte) 42, TestingHelper.decodeResult(result));

        txData = ABIEncoder.encodeMethodArguments("mapPut", (byte)2, (byte)13);
        result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getTransactionResult();

        Assert.assertEquals((byte) 13, TestingHelper.decodeResult(result));

        txData = ABIEncoder.encodeMethodArguments("mapGet", (byte)2);
        result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getTransactionResult();

        Assert.assertEquals((byte) 13, TestingHelper.decodeResult(result));
    }
}

package org.aion.avm.core;

import org.aion.avm.api.ABIEncoder;
import org.aion.avm.core.util.AvmRule;
import org.aion.avm.core.util.TestingHelper;
import org.aion.avm.userlib.AionList;
import org.aion.avm.userlib.AionMap;
import org.aion.avm.userlib.AionSet;
import org.aion.kernel.*;
import org.aion.vm.api.interfaces.TransactionResult;
import org.junit.*;

import java.math.BigInteger;

/**
 * As part of issue-215, we want to see if Synthetic Methods would break any of our assumptions in method
 * invocation. The Java compiler will generate two methods in the bytecode: one takes the Obejct as argument,
 * the other takes the specific type as argument. This test operates on SyntheticMethodsTestTarget to observe
 * any possible issues when we have a concrete method that overrides a generic method.
 */
public class SyntheticMethodsTest {
    @Rule
    public AvmRule avmRule = new AvmRule(false);

    private org.aion.vm.api.interfaces.Address from = KernelInterfaceImpl.PREMINED_ADDRESS;
    private org.aion.vm.api.interfaces.Address dappAddr;

    private long energyLimit = 6_000_0000;
    private long energyPrice = 1;


    @Before
    public void setup() {
        byte[] txData = avmRule.getDappBytes(SyntheticMethodsTestTarget.class, null, AionMap.class, AionSet.class, AionList.class);
        dappAddr = avmRule.deploy(from, BigInteger.ZERO, txData, energyLimit, energyPrice).getDappAddress();
    }

    @Test
    public void testDappWorking() {
        TransactionResult result = createAndRunTransaction("getCompareResult");

        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, result.getResultCode());
        Assert.assertEquals(SyntheticMethodsTestTarget.DEFAULT_VALUE, TestingHelper.decodeResult(result));
    }

    @Test
    public void testCompareTo() {
        // BigInteger
        TransactionResult result1 = createAndRunTransaction("compareSomething", 1);
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, result1.getResultCode());
        TransactionResult result1Value = createAndRunTransaction("getCompareResult");
        Assert.assertEquals(1, TestingHelper.decodeResult(result1Value));

        TransactionResult result2 = createAndRunTransaction("compareSomething", 2);
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, result2.getResultCode());
        TransactionResult result2Value = createAndRunTransaction("getCompareResult");
        Assert.assertEquals(0, TestingHelper.decodeResult(result2Value));

        TransactionResult result3 = createAndRunTransaction("compareSomething", 3);
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, result3.getResultCode());
        TransactionResult result3Value = createAndRunTransaction("getCompareResult");
        Assert.assertEquals(-1, TestingHelper.decodeResult(result3Value));

        TransactionResult result4 = createAndRunTransaction("compareSomething", 4);
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, result4.getResultCode());
        TransactionResult result4Value = createAndRunTransaction("getCompareResult");
        Assert.assertEquals(100, TestingHelper.decodeResult(result4Value));
    }

    @Test
    public void testTarget(){
        // pick target1Impl
        TransactionResult result1 = createAndRunTransaction("pickTarget", 1);
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, result1.getResultCode());

        // check for correctness in synthetic, should get impl1 name
        TransactionResult result2 = createAndRunTransaction("getName");
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, result2.getResultCode());
        Assert.assertEquals("TargetClassImplOne", TestingHelper.decodeResult(result2));

        // pick target2Impl
        TransactionResult result3 = createAndRunTransaction("pickTarget", 2);
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, result3.getResultCode());

        // check for correctness in synthetic, should get abstract name
        TransactionResult result4 = createAndRunTransaction("getName");
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, result4.getResultCode());
        Assert.assertEquals("TargetAbstractClass", TestingHelper.decodeResult(result4));
    }

    @Test
    public void testGenericMethodOverride(){
        int inputGeneric = 10;
        int inputOverrideGeneric = 20;

        // calling setup generics
        TransactionResult result1 = createAndRunTransaction("setGenerics",
                inputGeneric, inputOverrideGeneric);
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, result1.getResultCode());

        // retrieve each object
        TransactionResult result2 = createAndRunTransaction("getIntGen");
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, result2.getResultCode());
        Assert.assertEquals(inputGeneric, TestingHelper.decodeResult(result2));

        TransactionResult result3 = createAndRunTransaction("getIntGenSub");
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, result3.getResultCode());
        Assert.assertEquals(inputOverrideGeneric, TestingHelper.decodeResult(result3));

        TransactionResult result4 = createAndRunTransaction("getSubCopy");
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, result4.getResultCode());
        Assert.assertEquals(inputOverrideGeneric, TestingHelper.decodeResult(result4));
    }

    private TransactionResult createAndRunTransaction(String methodName, Object ... args){
        byte[] txData = ABIEncoder.encodeMethodArguments(methodName, args);
        return avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getTransactionResult();
    }
}

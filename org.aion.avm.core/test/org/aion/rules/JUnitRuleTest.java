package org.aion.rules;

import org.aion.avm.api.ABIEncoder;
import org.aion.avm.core.util.AvmRule;
import org.aion.avm.core.util.HashUtils;
import org.aion.avm.userlib.AionMap;
import org.aion.vm.api.interfaces.Address;
import org.aion.vm.api.interfaces.ResultCode;
import org.aion.vm.api.interfaces.TransactionResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.*;

public class JUnitRuleTest {

    // ClassRule annotation instantiates them only once for the whole test class.
    @ClassRule
    public static AvmRule avmRule = new AvmRule(true);

    private Address dappAddr;
    private Address preminedAccount = avmRule.getPreminedAccount();

    @Before
    public void deployDapp() {
        byte[] arguments = ABIEncoder.encodeMethodArguments("", 8);
        byte[] dapp = avmRule.getDappBytes(JUnitRuleTestTarget.class, arguments, AionMap.class);
        dappAddr = avmRule.deploy(preminedAccount, BigInteger.ZERO, dapp).getDappAddress();
    }

    @Test
    public void testIncreaseNumber() {
        long energyLimit = 6_000_0000;
        long energyPrice = 1;
        byte[] txData = ABIEncoder.encodeMethodArguments("increaseNumber", 10);
        Object result = avmRule.call(preminedAccount, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getDecodedReturnData();
        Assert.assertEquals(true, result);
    }

    @Test
    public void testSumInput() {
        Address sender = avmRule.getRandomAddress(BigInteger.valueOf(10_000_000L));
        byte[] txData = ABIEncoder.encodeMethodArguments("sum", 15, 10);
        Object result = avmRule.call(sender, dappAddr, BigInteger.ZERO, txData).getDecodedReturnData();
        Assert.assertEquals(15 + 10, result);
    }

    @Test
    public void testMapPut() {
        byte[] txData = ABIEncoder.encodeMethodArguments("mapPut", "1", 42);
        ResultCode result = avmRule.call(preminedAccount, dappAddr, BigInteger.ZERO, txData).getReceiptStatus();
        Assert.assertTrue(result.isFailed());
    }

    @Test
    public void testMapGet() {
        byte[] txData = ABIEncoder.encodeMethodArguments("mapPut", 1, "val1");
        ResultCode status = avmRule.call(preminedAccount, dappAddr, BigInteger.ZERO, txData).getReceiptStatus();
        Assert.assertTrue(status.isSuccess());

        txData = ABIEncoder.encodeMethodArguments("mapGet", 1);
        AvmRule.ResultWrapper result = avmRule.call(preminedAccount, dappAddr, BigInteger.ZERO, txData);
        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        Assert.assertEquals("val1", result.getDecodedReturnData());
    }

    @Test
    public void testLogEvent() {
        byte[] txData = ABIEncoder.encodeMethodArguments("logEvent");
        AvmRule.ResultWrapper result = avmRule.call(preminedAccount, dappAddr, BigInteger.ZERO, txData);

        Assert.assertTrue(result.getReceiptStatus().isSuccess());
        Assert.assertEquals(2, result.getLogs().size());
        assertEquals(dappAddr, result.getLogs().get(0).getSourceAddress());
        assertArrayEquals(new byte[]{ 0x1 }, result.getLogs().get(0).getData());
        assertArrayEquals(HashUtils.sha256(new byte[]{ 0xf, 0xe, 0xd, 0xc, 0xb, 0xa }), result.getLogs().get(1).getTopics().get(0));
    }

    @Test
    public void balanceTransfer(){
        Address to = avmRule.getRandomAddress(BigInteger.ZERO);
        TransactionResult result = avmRule.balanceTransfer(preminedAccount, to, BigInteger.valueOf(100L), 1L).getTransactionResult();

        assertTrue(result.getResultCode().isSuccess());
        assertEquals(BigInteger.valueOf(100L), avmRule.kernel.getBalance(to));

    }
}

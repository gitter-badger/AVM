package org.aion.avm.core;

import org.aion.avm.api.ABIEncoder;
import org.aion.avm.api.Address;
import org.aion.avm.core.bitcoin.Transaction;
import org.aion.avm.core.testHashes.HashTestTargetClass;
import org.aion.avm.core.util.AvmRule;
import org.aion.avm.core.util.HashUtils;
import org.aion.avm.core.util.Helpers;
import org.aion.avm.core.util.TestingHelper;
import org.aion.kernel.AvmTransactionResult;
import org.aion.kernel.KernelInterfaceImpl;
import org.aion.vm.api.interfaces.TransactionContext;
import org.aion.vm.api.interfaces.TransactionResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;

public class TransferTest {
    @Rule
    public AvmRule avmRule = new AvmRule(false);

    private long energyLimit = 10_000_000L;
    private long energyPrice = 1L;

    private org.aion.vm.api.interfaces.Address deployer = KernelInterfaceImpl.PREMINED_ADDRESS;
    private org.aion.vm.api.interfaces.Address dappAddress;

    private final String methodName = "doTransfer";
    private final long dappInitialBalance = 10000;

    @Before
    public void setup() {
        byte[] txData = avmRule.getDappBytes(TransferTestTarget.class, null);
        dappAddress = avmRule.deploy(deployer, BigInteger.ZERO, txData, energyLimit, energyPrice).getDappAddress();

        //send some balance to the dapp
        avmRule.kernel.adjustBalance(dappAddress, BigInteger.valueOf(dappInitialBalance));
    }

    @Test
    public void testTransferNormal() {
        long energyLimit = 5000000;
        long transferBalance = 500;

        // receiver address
        org.aion.vm.api.interfaces.Address receiverAddress = Helpers.randomAddress();
        avmRule.kernel.createAccount(receiverAddress);
        byte[] to = receiverAddress.toBytes();

        // Call contract
        byte[] txData = ABIEncoder.encodeMethodArguments(methodName, to, transferBalance, energyLimit);
        TransactionResult txResult = avmRule.call(deployer, dappAddress, BigInteger.ZERO, txData, this.energyLimit, this.energyPrice).getTransactionResult();
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, txResult.getResultCode());

        // check balance
        BigInteger contractBalance = avmRule.kernel.getBalance(dappAddress);
        System.out.println("contract balance is: " + contractBalance);
        Assert.assertEquals(dappInitialBalance - transferBalance, contractBalance.intValue());

        BigInteger receiverBalance = avmRule.kernel.getBalance(receiverAddress);
        System.out.println("receiver balance is: " + receiverBalance);
        Assert.assertEquals(transferBalance, receiverBalance.intValue());
    }

    @Test
    public void testTransferZero() {
        long energyLimit = 5000000;
        long transferBalance = 0;

        // receiver address
        org.aion.vm.api.interfaces.Address receiverAddress = Helpers.randomAddress();
        avmRule.kernel.createAccount(receiverAddress);
        byte[] to = receiverAddress.toBytes();

        // Call contract
        byte[] txData = ABIEncoder.encodeMethodArguments(methodName, to, transferBalance, energyLimit);
        TransactionResult txResult = avmRule.call(deployer, dappAddress, BigInteger.ZERO, txData, this.energyLimit, this.energyPrice).getTransactionResult();
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, txResult.getResultCode());

        // check balance
        BigInteger contractBalance = avmRule.kernel.getBalance(dappAddress);
        System.out.println("contract balance is: " + contractBalance);
        Assert.assertEquals(dappInitialBalance - transferBalance, contractBalance.intValue());

        BigInteger receiverBalance = avmRule.kernel.getBalance(receiverAddress);
        System.out.println("receiver balance is: " + receiverBalance);
        Assert.assertEquals(transferBalance, receiverBalance.intValue());
    }

    @Test
    public void testTransferNegative() {
        long energyLimit = 5000000;
        long transferBalance = -10;

        // receiver address
        org.aion.vm.api.interfaces.Address receiverAddress = Helpers.randomAddress();
        avmRule.kernel.createAccount(receiverAddress);
        byte[] to = receiverAddress.toBytes();

        // Call contract
        byte[] txData = ABIEncoder.encodeMethodArguments(methodName, to, transferBalance, energyLimit);
        TransactionResult txResult = avmRule.call(deployer, dappAddress, BigInteger.ZERO, txData, this.energyLimit, this.energyPrice).getTransactionResult();
        Assert.assertEquals(AvmTransactionResult.Code.FAILED_EXCEPTION, txResult.getResultCode());

        // check balance
        BigInteger contractBalance = avmRule.kernel.getBalance(dappAddress);
        System.out.println("contract balance is: " + contractBalance);
        Assert.assertEquals(dappInitialBalance, contractBalance.intValue());

        BigInteger receiverBalance = avmRule.kernel.getBalance(receiverAddress);
        System.out.println("receiver balance is: " + receiverBalance);
        Assert.assertEquals(0, receiverBalance.intValue());
    }

    @Test
    public void testTransferInternal() {
        // call a dapp that calls another dapp to transfer value
        long energyLimit = 5000000;
        long transferBalance = 1000;
        String methodNameOuter = "callAnotherDappToTransfer";

        // deploy 2nd dapp
        org.aion.vm.api.interfaces.Address dapp2Address;
        byte[] deployData = avmRule.getDappBytes(TransferTestTarget.class, null);
        dapp2Address = avmRule.deploy(deployer, BigInteger.ZERO, deployData, energyLimit, energyPrice).getDappAddress();
        avmRule.kernel.adjustBalance(dapp2Address, BigInteger.valueOf(dappInitialBalance));

        // generate receiver address
        org.aion.vm.api.interfaces.Address receiverAddress = Helpers.randomAddress();
        avmRule.kernel.createAccount(receiverAddress);

        // Call contract
        byte[] txData2 = ABIEncoder.encodeMethodArguments(methodName, receiverAddress.toBytes(), transferBalance, energyLimit);
        byte[] txData = ABIEncoder.encodeMethodArguments(methodNameOuter, dapp2Address.toBytes(), txData2, energyLimit);

        TransactionResult txResult = avmRule.call(deployer, dappAddress, BigInteger.ZERO, txData, this.energyLimit, this.energyPrice).getTransactionResult();
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, txResult.getResultCode());

        // check balance
        BigInteger contractBalance = avmRule.kernel.getBalance(dappAddress);
        BigInteger contract2Balance = avmRule.kernel.getBalance(dapp2Address);
        BigInteger receiverBalance = avmRule.kernel.getBalance(receiverAddress);

        System.out.println("contract balance is: " + contractBalance);
        Assert.assertEquals(dappInitialBalance, contractBalance.intValue());
        System.out.println("contract2 balance is: " + contract2Balance);
        Assert.assertEquals(dappInitialBalance - transferBalance, contract2Balance.intValue());
        System.out.println("receiver balance is: " + receiverBalance);
        Assert.assertEquals(transferBalance, receiverBalance.intValue());
    }
}
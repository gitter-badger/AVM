package org.aion.avm.core.blockchainruntime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import org.aion.avm.api.ABIDecoder;
import org.aion.avm.api.ABIEncoder;
import org.aion.avm.api.BlockchainRuntime;
import org.aion.avm.core.AvmImpl;
import org.aion.avm.core.CommonAvmFactory;
import org.aion.avm.core.EmptyInstrumentation;
import org.aion.avm.core.RedirectContract;
import org.aion.avm.core.dappreading.JarBuilder;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.avm.core.util.Helpers;
import org.aion.avm.internal.IInstrumentation;
import org.aion.avm.internal.InstrumentationHelpers;
import org.aion.kernel.AvmAddress;
import org.aion.kernel.Block;
import org.aion.kernel.KernelInterfaceImpl;
import org.aion.kernel.Transaction;
import org.aion.kernel.TransactionContextImpl;
import org.aion.vm.api.interfaces.Address;
import org.aion.vm.api.interfaces.KernelInterface;
import org.aion.vm.api.interfaces.TransactionContext;
import org.aion.vm.api.interfaces.TransactionResult;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the {@link BlockchainRuntime#getBalanceOfThisContract()} method for retrieving the balance
 * of a deployed contract from within that contract.
 */
public class ContractBalanceTest {
    private static Address from = KernelInterfaceImpl.PREMINED_ADDRESS;
    private static long energyLimit = 5_000_000L;
    private static long energyPrice = 5;
    private static Block block = new Block(new byte[32], 1, Helpers.randomAddress(), System.currentTimeMillis(), new byte[0]);

    private static KernelInterface kernel;
    private static AvmImpl avm;

    @BeforeClass
    public static void setup() {
        kernel = new KernelInterfaceImpl();
        avm = CommonAvmFactory.buildAvmInstance(kernel);
    }

    @AfterClass
    public static void tearDown() {
        avm.shutdown();
    }

    @Test
    public void testClinitBalanceWhenTransferringZero() {
        Address contract = deployContract(BigInteger.ZERO);
        BigInteger actualBalance = callContractToGetClinitBalance(contract);
        assertEquals(BigInteger.ZERO, actualBalance);
    }

    @Test
    public void testClinitBalanceWhenTransferringPositiveAmount() {
        BigInteger transferAmount = BigInteger.valueOf(1234567);
        Address contract = deployContract(transferAmount);
        BigInteger actualBalance = callContractToGetClinitBalance(contract);
        assertEquals(transferAmount, actualBalance);
    }

    @Test
    public void testContractBalance() {
        Address contract = deployContract(BigInteger.ZERO);

        // Contract currently has no balance.
        BigInteger balance = callContractToGetItsBalance(contract);
        assertEquals(BigInteger.ZERO, balance);

        // Increase the contract balance and check the amount.
        BigInteger delta1 = BigInteger.TWO.pow(1024);
        kernel.adjustBalance(contract, delta1);
        balance = callContractToGetItsBalance(contract);
        assertEquals(delta1, balance);

        // Decrease the contract balance and check the amount.
        BigInteger delta2 = BigInteger.TWO.pow(84).negate();
        kernel.adjustBalance(contract, delta2);
        balance = callContractToGetItsBalance(contract);
        assertEquals(delta1.add(delta2), balance);
    }

    @Test
    public void testContractBalanceViaInternalTransaction() {
        Address balanceContract = deployContract(BigInteger.ZERO);
        Address redirectContract = deployRedirectContract();

        // We give the redirect contract some balance to ensure we aren't querying the wrong contract.
        kernel.adjustBalance(redirectContract, BigInteger.valueOf(2938752));

        // Contract currently has no balance.
        BigInteger balance = callContractToGetItsBalanceViaRedirectContract(redirectContract, balanceContract);
        assertEquals(BigInteger.ZERO, balance);
    }

    /**
     * Deploys the contract and transfers value amount of Aion into it.
     */
    private Address deployContract(BigInteger value) {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(ContractBalanceTarget.class);
        jar = new CodeAndArguments(jar, new byte[0]).encodeToBytes();

        Transaction transaction = Transaction.create(from, kernel.getNonce(from), value, jar, energyLimit, energyPrice);
        TransactionContext context = new TransactionContextImpl(transaction, block);
        TransactionResult result = avm.run(new TransactionContext[] {context})[0].get();
        assertTrue(result.getResultCode().isSuccess());
        return AvmAddress.wrap(result.getReturnData());
    }

    private BigInteger callContractToGetItsBalance(Address contract) {
        byte[] callData = ABIEncoder.encodeMethodArguments("getBalanceOfThisContract");
        Transaction transaction = Transaction.call(from, contract, kernel.getNonce(from), BigInteger.ZERO, callData, energyLimit, energyPrice);
        TransactionContext context = new TransactionContextImpl(transaction, block);
        TransactionResult result = avm.run(new TransactionContext[] {context})[0].get();
        assertTrue(result.getResultCode().isSuccess());
        return new BigInteger((byte[]) ABIDecoder.decodeOneObject(result.getReturnData()));
    }

    private BigInteger callContractToGetClinitBalance(Address contract) {
        byte[] callData = ABIEncoder.encodeMethodArguments("getBalanceOfThisContractDuringClinit");
        Transaction transaction = Transaction.call(from, contract, kernel.getNonce(from), BigInteger.ZERO, callData, energyLimit, energyPrice);
        TransactionContext context = new TransactionContextImpl(transaction, block);
        TransactionResult result = avm.run(new TransactionContext[] {context})[0].get();
        assertTrue(result.getResultCode().isSuccess());
        return new BigInteger((byte[]) ABIDecoder.decodeOneObject(result.getReturnData()));
    }

    private Address deployRedirectContract() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(RedirectContract.class);
        jar = new CodeAndArguments(jar, new byte[0]).encodeToBytes();

        Transaction transaction = Transaction.create(from, kernel.getNonce(from), BigInteger.ZERO, jar, energyLimit, energyPrice);
        TransactionContext context = new TransactionContextImpl(transaction, block);
        TransactionResult result = avm.run(new TransactionContext[] {context})[0].get();
        assertTrue(result.getResultCode().isSuccess());
        return AvmAddress.wrap(result.getReturnData());
    }

    private BigInteger callContractToGetItsBalanceViaRedirectContract(Address redirectContract, Address balanceContract) {
        org.aion.avm.api.Address contract = getContractAsAbiAddress(balanceContract);
        byte[] args = ABIEncoder.encodeMethodArguments("getBalanceOfThisContract");
        byte[] callData = ABIEncoder.encodeMethodArguments("callOtherContractAndRequireItIsSuccess", contract, 0L, args);
        return runTransactionAndInterpretOutputAsBigInteger(redirectContract, callData);
    }

    private BigInteger runTransactionAndInterpretOutputAsBigInteger(Address contract, byte[] callData) {
        Transaction transaction = Transaction.call(from, contract, kernel.getNonce(from), BigInteger.ZERO, callData, energyLimit, energyPrice);
        TransactionContext context = new TransactionContextImpl(transaction, block);
        TransactionResult result = avm.run(new TransactionContext[] {context})[0].get();
        assertTrue(result.getResultCode().isSuccess());
        return new BigInteger((byte[]) ABIDecoder.decodeOneObject(result.getReturnData()));
    }

    private org.aion.avm.api.Address getContractAsAbiAddress(Address contract) {
        IInstrumentation instrumentation = new EmptyInstrumentation();
        InstrumentationHelpers.attachThread(instrumentation);
        org.aion.avm.api.Address converted = new org.aion.avm.api.Address(contract.toBytes());
        InstrumentationHelpers.detachThread(instrumentation);
        return converted;
    }

}

package org.aion.avm.core;

import org.aion.avm.api.ABIEncoder;
import org.aion.avm.api.Address;
import org.aion.avm.core.dappreading.JarBuilder;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.avm.core.util.Helpers;
import org.aion.kernel.Block;
import org.aion.kernel.KernelInterfaceImpl;
import org.aion.kernel.TransactionContextImpl;
import org.aion.kernel.Transaction;
import org.aion.kernel.TransactionResult;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


public class ShadowSerializationTest {
    private static Block block;
    private static final long DEPLOY_ENERGY_LIMIT = 10_000_000L;
    private static final long ENERGY_PRICE = 1L;

    // Note that these numbers change pretty frequently, based on constants in the test, etc.
    private static final int HASH_JAVA_LANG = 94290346;
    private static final int HASH_JAVA_MATH = -602588053;
    private static final int HASH_JAVA_NIO = 757806641;
    private static final int HASH_API = 496;


    byte[] deployer = KernelInterfaceImpl.PREMINED_ADDRESS;
    KernelInterfaceImpl kernel = new KernelInterfaceImpl();
    Avm avm = NodeEnvironment.singleton.buildAvmInstance(kernel);

    @BeforeClass
    public static void setupClass() {
        block = new Block(new byte[32], 1, Helpers.randomBytes(Address.LENGTH), System.currentTimeMillis(), new byte[0]);
    }


    @Test
    public void testPersistJavaLang() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(ShadowCoverageTarget.class);
        byte[] txData = new CodeAndArguments(jar, new byte[0]).encodeToBytes();
        
        // deploy
        Transaction tx1 = Transaction.create(deployer, kernel.getNonce(deployer), 0L, txData, DEPLOY_ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult result1 = avm.run(new TransactionContextImpl(tx1, block));
        Assert.assertEquals(TransactionResult.Code.SUCCESS, result1.getStatusCode());
        Address contractAddr = TestingHelper.buildAddress(result1.getReturnData());
        
        // Populate initial data.
        int firstHash = populate(avm, contractAddr, "JavaLang");
        // For now, just do the basic verification based on knowing the number.
        Assert.assertEquals(HASH_JAVA_LANG, firstHash);
        
        // Get the state of this data.
        int hash = getHash(avm, contractAddr, "JavaLang");
        Assert.assertEquals(firstHash, hash);
    }

    @Test
    public void testReentrantJavaLang() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(ShadowCoverageTarget.class);
        byte[] txData = new CodeAndArguments(jar, new byte[0]).encodeToBytes();
        
        // deploy
        Transaction tx1 = Transaction.create(deployer, kernel.getNonce(deployer), 0L, txData, DEPLOY_ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult result1 = avm.run(new TransactionContextImpl(tx1, block));
        Assert.assertEquals(TransactionResult.Code.SUCCESS, result1.getStatusCode());
        Address contractAddr = TestingHelper.buildAddress(result1.getReturnData());
        
        // Populate initial data.
        int firstHash = populate(avm, contractAddr, "JavaLang");
        // For now, just do the basic verification based on knowing the number.
        Assert.assertEquals(HASH_JAVA_LANG, firstHash);
        
        // Verify that things are consistent across reentrant modifications.
        verifyReentrantChange(avm, contractAddr, "JavaLang");
        
        // Call to verify, again, to detect the bug where reentrant serializing was incorrectly injecting constant stubs.
        verifyReentrantChange(avm, contractAddr, "JavaLang");
    }

    @Test
    public void testPersistJavaMath() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(ShadowCoverageTarget.class);
        byte[] txData = new CodeAndArguments(jar, new byte[0]).encodeToBytes();
        
        // deploy
        Transaction tx1 = Transaction.create(deployer, kernel.getNonce(deployer), 0L, txData, DEPLOY_ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult result1 = avm.run(new TransactionContextImpl(tx1, block));
        Assert.assertEquals(TransactionResult.Code.SUCCESS, result1.getStatusCode());
        Address contractAddr = TestingHelper.buildAddress(result1.getReturnData());
        
        // Populate initial data.
        int firstHash = populate(avm, contractAddr, "JavaMath");
        // For now, just do the basic verification based on knowing the number.
        Assert.assertEquals(HASH_JAVA_MATH, firstHash);
        
        // Get the state of this data.
        int hash = getHash(avm, contractAddr, "JavaMath");
        Assert.assertEquals(firstHash, hash);
    }

    @Test
    public void testReentrantJavaMath() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(ShadowCoverageTarget.class);
        byte[] txData = new CodeAndArguments(jar, new byte[0]).encodeToBytes();
        
        // deploy
        Transaction tx1 = Transaction.create(deployer, kernel.getNonce(deployer), 0L, txData, DEPLOY_ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult result1 = avm.run(new TransactionContextImpl(tx1, block));
        Assert.assertEquals(TransactionResult.Code.SUCCESS, result1.getStatusCode());
        Address contractAddr = TestingHelper.buildAddress(result1.getReturnData());
        
        // Populate initial data.
        int firstHash = populate(avm, contractAddr, "JavaMath");
        // For now, just do the basic verification based on knowing the number.
        Assert.assertEquals(HASH_JAVA_MATH, firstHash);
        
        // Verify that things are consistent across reentrant modifications.
        verifyReentrantChange(avm, contractAddr, "JavaMath");
        
        // Call to verify, again, to detect the bug where reentrant serializing was incorrectly injecting constant stubs.
        verifyReentrantChange(avm, contractAddr, "JavaMath");
    }

    @Test
    public void testPersistJavaNio() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(ShadowCoverageTarget.class);
        byte[] txData = new CodeAndArguments(jar, new byte[0]).encodeToBytes();
        
        // deploy
        Transaction tx1 = Transaction.create(deployer, kernel.getNonce(deployer), 0L, txData, DEPLOY_ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult result1 = avm.run(new TransactionContextImpl(tx1, block));
        Assert.assertEquals(TransactionResult.Code.SUCCESS, result1.getStatusCode());
        Address contractAddr = TestingHelper.buildAddress(result1.getReturnData());
        
        // Populate initial data.
        int firstHash = populate(avm, contractAddr, "JavaNio");
        // For now, just do the basic verification based on knowing the number.
        Assert.assertEquals(HASH_JAVA_NIO, firstHash);
        
        // Get the state of this data.
        int hash = getHash(avm, contractAddr, "JavaNio");
        Assert.assertEquals(firstHash, hash);
    }

    @Test
    public void testReentrantJavaNio() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(ShadowCoverageTarget.class);
        byte[] txData = new CodeAndArguments(jar, new byte[0]).encodeToBytes();
        
        // deploy
        Transaction tx1 = Transaction.create(deployer, kernel.getNonce(deployer), 0L, txData, DEPLOY_ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult result1 = avm.run(new TransactionContextImpl(tx1, block));
        Assert.assertEquals(TransactionResult.Code.SUCCESS, result1.getStatusCode());
        Address contractAddr = TestingHelper.buildAddress(result1.getReturnData());
        
        // Populate initial data.
        int firstHash = populate(avm, contractAddr, "JavaNio");
        // For now, just do the basic verification based on knowing the number.
        Assert.assertEquals(HASH_JAVA_NIO, firstHash);
        
        // Verify that things are consistent across reentrant modifications.
        verifyReentrantChange(avm, contractAddr, "JavaNio");
        
        // Call to verify, again, to detect the bug where reentrant serializing was incorrectly injecting constant stubs.
        verifyReentrantChange(avm, contractAddr, "JavaNio");
    }

    @Test
    public void testPersistApi() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(ShadowCoverageTarget.class);
        byte[] txData = new CodeAndArguments(jar, new byte[0]).encodeToBytes();
        
        // deploy
        Transaction tx1 = Transaction.create(deployer, kernel.getNonce(deployer), 0L, txData, DEPLOY_ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult result1 = avm.run(new TransactionContextImpl(tx1, block));
        Assert.assertEquals(TransactionResult.Code.SUCCESS, result1.getStatusCode());
        Address contractAddr = TestingHelper.buildAddress(result1.getReturnData());
        
        // Populate initial data.
        int firstHash = populate(avm, contractAddr, "Api");
        // For now, just do the basic verification based on knowing the number.
        Assert.assertEquals(HASH_API, firstHash);
        
        // Get the state of this data.
        int hash = getHash(avm, contractAddr, "Api");
        Assert.assertEquals(firstHash, hash);
    }

    @Test
    public void testReentrantApi() {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(ShadowCoverageTarget.class);
        byte[] txData = new CodeAndArguments(jar, new byte[0]).encodeToBytes();
        
        // deploy
        Transaction tx1 = Transaction.create(deployer, kernel.getNonce(deployer), 0L, txData, DEPLOY_ENERGY_LIMIT, ENERGY_PRICE);
        TransactionResult result1 = avm.run(new TransactionContextImpl(tx1, block));
        Assert.assertEquals(TransactionResult.Code.SUCCESS, result1.getStatusCode());
        Address contractAddr = TestingHelper.buildAddress(result1.getReturnData());
        
        // Populate initial data.
        int firstHash = populate(avm, contractAddr, "Api");
        // For now, just do the basic verification based on knowing the number.
        Assert.assertEquals(HASH_API, firstHash);
        
        // Verify that things are consistent across reentrant modifications.
        verifyReentrantChange(avm, contractAddr, "Api");
        
        // Call to verify, again, to detect the bug where reentrant serializing was incorrectly injecting constant stubs.
        verifyReentrantChange(avm, contractAddr, "Api");
    }


    private int populate(Avm avm, Address contractAddr, String segmentName) {
        long energyLimit = 1_000_000L;
        byte[] argData = ABIEncoder.encodeMethodArguments("populate_" + segmentName);
        Transaction call = Transaction.call(deployer, contractAddr.unwrap(), kernel.getNonce(deployer), 0,  argData, energyLimit, ENERGY_PRICE);
        TransactionResult result = avm.run(new TransactionContextImpl(call, block));
        Assert.assertEquals(TransactionResult.Code.SUCCESS, result.getStatusCode());
        return ((Integer)TestingHelper.decodeResult(result)).intValue();
    }

    private int getHash(Avm avm, Address contractAddr, String segmentName) {
        long energyLimit = 1_000_000L;
        byte[] argData = ABIEncoder.encodeMethodArguments("getHash_" + segmentName);
        Transaction call = Transaction.call(deployer, contractAddr.unwrap(), kernel.getNonce(deployer), 0,  argData, energyLimit, ENERGY_PRICE);
        TransactionResult result = avm.run(new TransactionContextImpl(call, block));
        Assert.assertEquals(TransactionResult.Code.SUCCESS, result.getStatusCode());
        return ((Integer)TestingHelper.decodeResult(result)).intValue();
    }

    private void verifyReentrantChange(Avm avm, Address contractAddr, String segmentName) {
        long energyLimit = 2_000_000L;
        byte[] argData = ABIEncoder.encodeMethodArguments("verifyReentrantChange_" + segmentName);
        Transaction call = Transaction.call(deployer, contractAddr.unwrap(), kernel.getNonce(deployer), 0,  argData, energyLimit, ENERGY_PRICE);
        TransactionResult result = avm.run(new TransactionContextImpl(call, block));
        Assert.assertEquals(TransactionResult.Code.SUCCESS, result.getStatusCode());
        Assert.assertTrue((Boolean)TestingHelper.decodeResult(result));
    }
}

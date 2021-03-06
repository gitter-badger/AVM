package org.aion.avm.core;

import java.math.BigInteger;

import org.aion.avm.core.util.AvmRule;
import org.aion.avm.core.util.TestingHelper;
import org.aion.kernel.AvmAddress;
import org.aion.kernel.AvmTransactionResult;
import org.aion.kernel.KernelInterfaceImpl;
import org.aion.avm.api.ABIEncoder;
import org.aion.avm.api.Address;
import org.aion.vm.api.interfaces.TransactionResult;
import org.junit.*;


/**
 * Tests the JCL and API types which can be sub-classed.
 * Also ensures that they can be persisted and reloaded correctly.
 */
public class SubclassPersistenceIntegrationTest {
    @Rule
    public AvmRule avmRule = new AvmRule(false);

    private static final long ENERGY_LIMIT = 10_000_000L;
    private static final long ENERGY_PRICE = 1L;

    @Test
    public void testUserSubclass() throws Exception {
        Address dapp = installTestDApp(SubclassPersistenceIntegrationTestTarget.class);
        
        // Setup the environment.
        int startValue = callStaticReturnInteger(dapp, "setup_user");
        
        // Re-run it to make sure that save/load worked correctly.
        int endValue = callStaticReturnInteger(dapp, "check_user");
        Assert.assertEquals(startValue, endValue);
    }

    @Test
    public void testEnumSubclass() throws Exception {
        Address dapp = installTestDApp(SubclassPersistenceIntegrationTestTarget.class);
        
        // Setup the environment.
        int startValue = callStaticReturnInteger(dapp, "setup_enum");
        
        // Re-run it to make sure that save/load worked correctly.
        int endValue = callStaticReturnInteger(dapp, "check_enum");
        Assert.assertEquals(startValue, endValue);
    }

    @Test
    public void testExceptionSubclass() throws Exception {
        Address dapp = installTestDApp(SubclassPersistenceIntegrationTestTarget.class);
        
        // Setup the environment.
        int startValue = callStaticReturnInteger(dapp, "setup_exception");
        
        // Re-run it to make sure that save/load worked correctly.
        int endValue = callStaticReturnInteger(dapp, "check_exception");
        Assert.assertEquals(startValue, endValue);
    }

    @Test
    public void testObjectSubclass() throws Exception {
        Address dapp = installTestDApp(SubclassPersistenceIntegrationTestTarget.class);
        
        // Setup the environment.
        int startValue = callStaticReturnInteger(dapp, "setup_object");
        
        // Re-run it to make sure that save/load worked correctly.
        int endValue = callStaticReturnInteger(dapp, "check_object");
        Assert.assertEquals(startValue, endValue);
    }

    @Test
    public void testRuntimeExceptionSubclass() throws Exception {
        Address dapp = installTestDApp(SubclassPersistenceIntegrationTestTarget.class);
        
        // Setup the environment.
        int startValue = callStaticReturnInteger(dapp, "setup_runtimeException");
        
        // Re-run it to make sure that save/load worked correctly.
        int endValue = callStaticReturnInteger(dapp, "check_runtimeException");
        Assert.assertEquals(startValue, endValue);
    }

    @Test
    public void testThrowableSubclass() throws Exception {
        Address dapp = installTestDApp(SubclassPersistenceIntegrationTestTarget.class);
        
        // Setup the environment.
        int startValue = callStaticReturnInteger(dapp, "setup_throwable");
        
        // Re-run it to make sure that save/load worked correctly.
        int endValue = callStaticReturnInteger(dapp, "check_throwable");
        Assert.assertEquals(startValue, endValue);
    }

    @Test
    public void testFailedDeploy_Exception() throws Exception {
        failedInstall(SubclassPersistenceIntegrationTestFailException.class);
    }

    @Test
    public void testFailedDeploy_Api() throws Exception {
        failedInstall(SubclassPersistenceIntegrationTestFailApi.class);
    }


    private Address installTestDApp(Class<?> testClass) {
        byte[] txData = avmRule.getDappBytes(testClass, new byte[0]);
        TransactionResult createResult = avmRule.deploy(KernelInterfaceImpl.PREMINED_ADDRESS, BigInteger.ZERO, txData, ENERGY_LIMIT, ENERGY_PRICE).getTransactionResult();
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, createResult.getResultCode());
        return TestingHelper.buildAddress(createResult.getReturnData());
    }

    private int callStaticReturnInteger(Address dapp, String methodName) {
        byte[] argData = ABIEncoder.encodeMethodArguments(methodName);
        TransactionResult result = avmRule.call(KernelInterfaceImpl.PREMINED_ADDRESS, AvmAddress.wrap(dapp.unwrap()), BigInteger.ZERO, argData, ENERGY_LIMIT, ENERGY_PRICE).getTransactionResult();
        Assert.assertEquals(AvmTransactionResult.Code.SUCCESS, result.getResultCode());
        return ((Integer)TestingHelper.decodeResult(result)).intValue();
    }

    private void failedInstall(Class<?> testClass) {
        byte[] txData = avmRule.getDappBytes(testClass, new byte[0]);

        // Deploy.
        TransactionResult createResult = avmRule.deploy(KernelInterfaceImpl.PREMINED_ADDRESS, BigInteger.ZERO, txData, ENERGY_LIMIT, ENERGY_PRICE).getTransactionResult();
        Assert.assertEquals(AvmTransactionResult.Code.FAILED_REJECTED, createResult.getResultCode());
    }
}

package org.aion.avm.core;

import java.math.BigInteger;
import org.aion.avm.api.ABIDecoder;
import org.aion.avm.api.ABIEncoder;
import org.aion.avm.api.BlockchainRuntime;
import org.aion.avm.api.Result;


/**
 * A test created as part of issue-167 to test out re-entrance concerns when a DApp calls itself.
 */
public class ReentrantCrossCallResource {
    // We use these to verify the commit/rollback of object graph state, during reentrant calls.
    private static int direct = 1;
    private static ReentrantCrossCallResource constant = new ReentrantCrossCallResource();
    private static Nested nestedInstance;

    private int near = 1;
    private int[] far = new int[] {1};


    public static byte[] main() {
        byte[] input = BlockchainRuntime.getData();
        return ABIDecoder.decodeAndRunWithClass(ReentrantCrossCallResource.class, input);
    }

    public static Object callSelfForNull() {
        // Call this method via the runtime.
        BigInteger value = BigInteger.ZERO;
        byte[] data = ABIEncoder.encodeMethodArguments("returnNull");
        long energyLimit = 500000;
        byte[] response = BlockchainRuntime.call(BlockchainRuntime.getAddress(), value, data, energyLimit).getReturnData();
        return (null != response)
                ? ABIDecoder.decodeOneObject(response)
                : response;
    }

    public static Object returnNull() {
        return null;
    }

    public static int getRecursiveHashCode(int iterationsRemaining) {
        Object object = new Object();
        int toReturn = 0;
        if (0 == iterationsRemaining) {
            toReturn = object.hashCode();
        } else {
            // Call this method via the runtime.
            BigInteger value = BigInteger.ZERO;
            byte[] data = ABIEncoder.encodeMethodArguments("getRecursiveHashCode", iterationsRemaining - 1);
            long energyLimit = 500000;
            byte[] response = BlockchainRuntime.call(BlockchainRuntime.getAddress(), value, data, energyLimit).getReturnData();
            toReturn = (Integer)ABIDecoder.decodeOneObject(response);
        }
        return toReturn;
    }

    // The get(Direct/Near/Far) all act by calling the corresponding "inc*" method, as a reentrant runtime call, and checking
    // the change in the caller's local state, upon return.
    // An expectation is determined, ahead-of-time, and either the new number (on success) or zero (on failure) is returned.
    public static int getDirect(boolean shouldFail) {
        // Cache the original answer to make sure the increment happens correctly.
        int expected = shouldFail
                ? direct
                : direct + 1;
        
        // Call ourselves.
        reentrantCall("incDirect", shouldFail);
        
        // If this matches expectation, return the new value, otherwise we return 0;
        return (expected == direct)
                ? direct
                : 0;
    }

    public static int getNear(boolean shouldFail) {
        // Cache the original answer to make sure the increment happens correctly.
        int expected = shouldFail
                ? constant.near
                : constant.near + 1;
        
        // Call ourselves.
        reentrantCall("incNear", shouldFail);
        
        // If this matches expectation, return the new value, otherwise we return 0;
        return (expected == constant.near)
                ? constant.near
                : 0;
    }

    public static int getFar(boolean shouldFail) {
        // Cache the original answer to make sure the increment happens correctly.
        int expected = shouldFail
                ? constant.far[0]
                : constant.far[0] + 1;
        
        // Call ourselves.
        reentrantCall("incFar", shouldFail);
        
        // If this matches expectation, return the new value, otherwise we return 0;
        return (expected == constant.far[0])
                ? constant.far[0]
                : 0;
    }

    /**
     * This case calls incFar, as a successful reentrant call, then fails in itself.
     * @return False if the reentrant call didn't observably change state (otherwise, fails - never returns true).
     */
    public static boolean localFailAfterReentrant() {
        // Cache the original answer to make sure the increment happens correctly.
        int expected = constant.far[0] + 1;
        
        // Call ourselves.
        reentrantCall("incFar", false);
        
        boolean doesMatch = (expected == constant.far[0]);
        if (doesMatch) {
            // This is the expected case so fail.
            causeFailure();
        }
        // We only get this far is something incorrect happened in the reentrant call (doesMatch being false).
        return doesMatch;
    }

    public static boolean getFarWithEnergy(long energyLimit) {
        // Note that we ALWAYS expect this call to fail so we expect no change in the value.
        int expected = constant.far[0];
        
        // Call ourselves.
        BigInteger value = BigInteger.ZERO;
        boolean calleeShouldFail = false;
        byte[] data = ABIEncoder.encodeMethodArguments("incFar", calleeShouldFail);
        BlockchainRuntime.call(BlockchainRuntime.getAddress(), value, data, energyLimit);
        
        // Return true if this value changed.
        return (expected != constant.far[0]);
    }

    public static void incDirect(boolean shouldFail) {
        direct += 2;
        if (shouldFail) {
            causeFailure();
        }
        direct -= 1;
    }

    public static void incNear(boolean shouldFail) {
        constant.near += 2;
        if (shouldFail) {
            causeFailure();
        }
        constant.near -= 1;
    }

    public static void incFar(boolean shouldFail) {
        constant.far[0] += 2;
        if (shouldFail) {
            causeFailure();
        }
        constant.far[0] -= 1;
    }

    /**
     * This case calls incFar, as a successful reentrant call, then fails in itself.
     * @return False if the reentrant call didn't observably change state (otherwise, fails - never returns true).
     */
    public static int recursiveChangeNested(int hashToCheck, int iterationsToCall) {
        // Check the state.
        if (0 == hashToCheck) {
            if (null != nestedInstance) {
                BlockchainRuntime.println("expected null");
                throw new AssertionError();
            }
        } else {
            if (hashToCheck != nestedInstance.hashCode()) {
                BlockchainRuntime.println("expected match");
                throw new AssertionError();
            }
            // We can also verify the state.
            if ((iterationsToCall + 1) != nestedInstance.data) {
                BlockchainRuntime.println("incorrect data");
                throw new AssertionError();
            }
        }
        
        // Install new state.
        Nested ourState = new Nested(iterationsToCall);
        nestedInstance = ourState;
        
        // Call recursive with our expectations based on the new state.
        if (iterationsToCall > 0) {
            // Make the reentrant call.
            BigInteger value = BigInteger.ZERO;
            byte[] data = ABIEncoder.encodeMethodArguments("recursiveChangeNested", ourState.hashCode(), iterationsToCall - 1);
            long energyLimit = 2_000_000L;
            Result txResult = BlockchainRuntime.call(BlockchainRuntime.getAddress(), value, data, energyLimit);
            byte[] result = txResult.getReturnData();
            if (txResult.isSuccess()) {
                int responseHash = ((Integer) ABIDecoder.decodeOneObject(result)).intValue();

                // Verify that we see this hash for the nestedInstance.
                if (responseHash != nestedInstance.hashCode()) {
                  BlockchainRuntime.println("response hash mismatch");
                  throw new AssertionError();
                }
                // Verify the data in the nestedInstance.
                if ((iterationsToCall - 1) != nestedInstance.data) {
                  BlockchainRuntime.println("response data mismatch");
                  throw new AssertionError();
                }
            }
            else {
                throw new IllegalStateException("Call depth limit is exceeded");
            }
            
            // Re-install our instance.
            nestedInstance = ourState;
        }
        
        // Return the hash of our state.
        return ourState.hashCode();
    }

    private static void reentrantCall(String methodName, boolean shouldFail) {
        BigInteger value = BigInteger.ZERO;
        byte[] data = ABIEncoder.encodeMethodArguments(methodName, shouldFail);
        // WARNING:  This number is finicky since some tests want to barely pass and others barely fail.
        long energyLimit = 100_000L;
        BlockchainRuntime.call(BlockchainRuntime.getAddress(), value, data, energyLimit);
    }

    // We should probably replace this with some kind of explicit failure mechanism, once one exists.
    // For now, our only want to ensure failure is to drain our energy.
    private static void causeFailure() {
        while (true) {
            new Object();
        }
    }


    public static class Nested {
        public Integer data;
        public Nested(Integer data) {
            this.data = data;
        }
    }
}

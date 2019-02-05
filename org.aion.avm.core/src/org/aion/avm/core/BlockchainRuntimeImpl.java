package org.aion.avm.core;

import org.aion.avm.api.Address;
import org.aion.avm.api.Result;
import org.aion.avm.core.crypto.CryptoUtil;
import org.aion.avm.internal.*;
import org.aion.avm.arraywrapper.ByteArray;
import org.aion.avm.core.types.InternalTransaction;
import org.aion.avm.core.util.HashUtils;
import org.aion.kernel.*;
import org.aion.kernel.Transaction.Type;
import org.aion.parallel.TransactionTask;

import java.util.List;
import org.aion.vm.api.interfaces.KernelInterface;
import org.aion.vm.api.interfaces.TransactionContext;


/**
 * The implementation of IBlockchainRuntime which is appropriate for exposure as a shadow Object instance within a DApp.
 */
public class BlockchainRuntimeImpl implements IBlockchainRuntime {
    private final KernelInterface kernel;
    private final AvmInternal avm;
    private final ReentrantDAppStack.ReentrantState reentrantState;

    private TransactionContext ctx;
    private final byte[] dAppData;
    private AvmTransactionResult result;
    private TransactionTask task;
    private final IRuntimeSetup thisDAppSetup;

    public BlockchainRuntimeImpl(KernelInterface kernel, AvmInternal avm, ReentrantDAppStack.ReentrantState reentrantState, TransactionTask task, TransactionContext ctx, byte[] dAppData, AvmTransactionResult result, IRuntimeSetup thisDAppSetup) {
        this.kernel = kernel;
        this.avm = avm;
        this.reentrantState = reentrantState;
        this.ctx = ctx;
        this.dAppData = dAppData;
        this.result = result;
        this.task = task;
        this.thisDAppSetup = thisDAppSetup;
    }

    @Override
    public Address avm_getAddress() {
        org.aion.vm.api.interfaces.Address address = (ctx.getTransactionKind() == Type.CREATE.toInt()) ? ctx.getContractAddress() : ctx.getDestinationAddress();
        return new Address(address.toBytes());
    }

    @Override
    public Address avm_getCaller() {
        return new Address(ctx.getSenderAddress().toBytes());
    }

    @Override
    public Address avm_getOrigin() {
        return new Address(ctx.getOriginAddress().toBytes());
    }

    @Override
    public long avm_getEnergyLimit() {
        return ctx.getTransaction().getEnergyLimit();
    }

    @Override
    public long avm_getEnergyPrice() {
        return ctx.getTransactionEnergyPrice();
    }

    @Override
    public org.aion.avm.shadow.java.math.BigInteger avm_getValue() {
        return new org.aion.avm.shadow.java.math.BigInteger(ctx.getTransferValue());
    }

    @Override
    public ByteArray avm_getData() {
        return (null != this.dAppData)
                ? new ByteArray(this.dAppData)
                : null;
    }


    @Override
    public long avm_getBlockTimestamp() {
        return ctx.getBlockTimestamp();
    }

    @Override
    public long avm_getBlockNumber() {
        return ctx.getBlockNumber();
    }

    @Override
    public long avm_getBlockEnergyLimit() {
        return ctx.getBlockEnergyLimit();
    }

    @Override
    public Address avm_getBlockCoinbase() {
        return new Address(ctx.getMinerAddress().toBytes());
    }

    @Override
    public org.aion.avm.shadow.java.math.BigInteger avm_getBlockDifficulty() {
        return org.aion.avm.shadow.java.math.BigInteger.avm_valueOf(ctx.getBlockDifficulty());
    }

    @Override
    public org.aion.avm.shadow.java.math.BigInteger avm_getBalance(Address address) {
        require(null != address, "Address can't be NULL");

        // Acquire resource before reading
        avm.getResourceMonitor().acquire(address.unwrap(), this.task);
        return new org.aion.avm.shadow.java.math.BigInteger(this.kernel.getBalance(AvmAddress.wrap(address.unwrap())));
    }

    @Override
    public org.aion.avm.shadow.java.math.BigInteger avm_getBalanceOfThisContract() {
        // This method can be called inside clinit so CREATE is a valid context.
        org.aion.vm.api.interfaces.Address contractAddress = (ctx.getTransaction().isContractCreationTransaction())
            ? ctx.getContractAddress()
            : ctx.getDestinationAddress();

        // Acquire resource before reading
        avm.getResourceMonitor().acquire(contractAddress.toBytes(), this.task);
        return new org.aion.avm.shadow.java.math.BigInteger(this.kernel.getBalance(contractAddress));
    }

    @Override
    public int avm_getCodeSize(Address address) {
        require(null != address, "Address can't be NULL");

        // Acquire resource before reading
        avm.getResourceMonitor().acquire(address.unwrap(), this.task);
        byte[] vc = this.kernel.getCode(AvmAddress.wrap(address.unwrap()));
        return vc == null ? 0 : vc.length;
    }

    @Override
    public long avm_getRemainingEnergy() {
        return IInstrumentation.attachedThreadInstrumentation.get().energyLeft();
    }

    @Override
    public Result avm_call(Address targetAddress, org.aion.avm.shadow.java.math.BigInteger value, ByteArray data, long energyLimit) {
        org.aion.vm.api.interfaces.Address internalSender = (ctx.getTransactionKind() == Type.CREATE.toInt()) ? ctx.getContractAddress() : ctx.getDestinationAddress();

        java.math.BigInteger underlyingValue = value.getUnderlying();
        require(targetAddress != null, "Destination can't be NULL");
        require(underlyingValue.compareTo(java.math.BigInteger.ZERO) >= 0 , "Value can't be negative");
        require(underlyingValue.compareTo(kernel.getBalance(internalSender)) <= 0, "Insufficient balance");
        require(data != null, "Data can't be NULL");
        require(energyLimit >= 0, "Energy limit can't be negative");

        if (ctx.getTransactionStackDepth() == 10) {
            throw new CallDepthLimitExceededException("Internal call depth cannot be more than 10");
        }

        AvmAddress target = AvmAddress.wrap(targetAddress.unwrap());
        if (!kernel.destinationAddressIsSafeForThisVM(target)) {
            throw new IllegalArgumentException("Attempt to execute code using a foreign virtual machine");
        }

        // construct the internal transaction
        InternalTransaction internalTx = new InternalTransaction(Transaction.Type.CALL,
                internalSender,
                target,
                this.kernel.getNonce(internalSender),
                underlyingValue,
                data.getUnderlying(),
                restrictEnergyLimit(energyLimit),
                ctx.getTransactionEnergyPrice());
        
        // Call the common run helper.
        return runInternalCall(internalTx);
    }

    @Override
    public Result avm_transfer(Address targetAddress, org.aion.avm.shadow.java.math.BigInteger value, long energyLimit) throws IllegalArgumentException {
        org.aion.vm.api.interfaces.Address internalSender = (ctx.getTransactionKind() == Type.CREATE.toInt()) ? ctx.getContractAddress() : ctx.getDestinationAddress();

        java.math.BigInteger underlyingValue = value.getUnderlying();
        require(targetAddress != null, "Destination can't be NULL");
        require(underlyingValue.compareTo(java.math.BigInteger.ZERO) >= 0 , "Value can't be negative");
        require(underlyingValue.compareTo(kernel.getBalance(internalSender)) <= 0, "Insufficient balance");
        require(energyLimit >= 0, "Energy limit can't be negative");

        if (ctx.getTransactionStackDepth() == 10) {
            throw new CallDepthLimitExceededException("Internal call depth cannot be more than 10");
        }

        AvmAddress target = AvmAddress.wrap(targetAddress.unwrap());
        if (!kernel.destinationAddressIsSafeForThisVM(target)) {
            throw new IllegalArgumentException("Attempt to execute code using a foreign virtual machine");
        }

        // construct the internal transaction
        InternalTransaction internalTx = new InternalTransaction(Transaction.Type.CALL,
                internalSender,
                target,
                this.kernel.getNonce(internalSender),
                underlyingValue,
                new byte[0],
                restrictEnergyLimit(energyLimit),
                ctx.getTransactionEnergyPrice());

        // Call the common run helper.
        return runInternalCall(internalTx);
    }


    @Override
    public Result avm_create(org.aion.avm.shadow.java.math.BigInteger value, ByteArray data, long energyLimit) {
        org.aion.vm.api.interfaces.Address internalSender = (ctx.getTransactionKind() == Type.CREATE.toInt()) ? ctx.getContractAddress() : ctx.getDestinationAddress();

        java.math.BigInteger underlyingValue = value.getUnderlying();
        require(underlyingValue.compareTo(java.math.BigInteger.ZERO) >= 0 , "Value can't be negative");
        require(underlyingValue.compareTo(kernel.getBalance(internalSender)) <= 0, "Insufficient balance");
        require(data != null, "Data can't be NULL");
        require(energyLimit >= 0, "Energy limit can't be negative");

        if (ctx.getTransactionStackDepth() == 10) {
            throw new CallDepthLimitExceededException("Internal call depth cannot be more than 10");
        }

        // construct the internal transaction
        InternalTransaction internalTx = new InternalTransaction(Transaction.Type.CREATE,
                internalSender,
                null,
                this.kernel.getNonce(internalSender),
                underlyingValue,
                data.getUnderlying(),
                restrictEnergyLimit(energyLimit),
                ctx.getTransactionEnergyPrice());
        
        // Call the common run helper.
        return runInternalCall(internalTx);
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    @Override
    public void avm_selfDestruct(Address beneficiary) {
        require(null != beneficiary, "Beneficiary can't be NULL");

        org.aion.vm.api.interfaces.Address contractAddr = (ctx.getTransactionKind() == Type.CREATE.toInt())
            ? ctx.getContractAddress()
            : ctx.getDestinationAddress();

        // Acquire beneficiary address, the address of current contract is already locked at this stage.
        this.avm.getResourceMonitor().acquire(beneficiary.unwrap(), this.task);

        // Value transfer
        java.math.BigInteger balanceToTransfer = this.kernel.getBalance(contractAddr);
        this.kernel.adjustBalance(contractAddr, balanceToTransfer.negate());
        this.kernel.adjustBalance(AvmAddress.wrap(beneficiary.unwrap()), balanceToTransfer);

        // Delete Account
        // Note that the account being deleted means it will still run but no DApp which sees this delete
        // (the current one and any callers, or any later transactions, assuming this commits) will be able
        // to invoke it (the code will be missing).
        this.kernel.deleteAccount(contractAddr);
    }

    @Override
    public void avm_log(ByteArray data) {
        require(null != data, "data can't be NULL");

        Log log = new Log(ctx.getDestinationAddress().toBytes(), List.of(), data.getUnderlying());
        ctx.getSideEffects().addLog(log);
    }

    @Override
    public void avm_log(ByteArray topic1, ByteArray data) {
        require(null != topic1, "topic1 can't be NULL");
        require(null != data, "data can't be NULL");

        Log log = new Log(ctx.getDestinationAddress().toBytes(), List.of(HashUtils.sha256(topic1.getUnderlying())), data.getUnderlying());
        ctx.getSideEffects().addLog(log);
    }

    @Override
    public void avm_log(ByteArray topic1, ByteArray topic2, ByteArray data) {
        require(null != topic1, "topic1 can't be NULL");
        require(null != topic2, "topic2 can't be NULL");
        require(null != data, "data can't be NULL");

        Log log = new Log(ctx.getDestinationAddress().toBytes(), List.of(HashUtils.sha256(topic1.getUnderlying()), HashUtils.sha256(topic2.getUnderlying())),
                data.getUnderlying());
        ctx.getSideEffects().addLog(log);
    }

    @Override
    public void avm_log(ByteArray topic1, ByteArray topic2, ByteArray topic3, ByteArray data) {
        require(null != topic1, "topic1 can't be NULL");
        require(null != topic2, "topic2 can't be NULL");
        require(null != topic3, "topic3 can't be NULL");
        require(null != data, "data can't be NULL");

        Log log = new Log(ctx.getDestinationAddress().toBytes(), List.of(HashUtils.sha256(topic1.getUnderlying()), HashUtils.sha256(topic2.getUnderlying()),
                HashUtils.sha256(topic3.getUnderlying())), data.getUnderlying());
        ctx.getSideEffects().addLog(log);
    }

    @Override
    public void avm_log(ByteArray topic1, ByteArray topic2, ByteArray topic3, ByteArray topic4, ByteArray data) {
        require(null != topic1, "topic1 can't be NULL");
        require(null != topic2, "topic2 can't be NULL");
        require(null != topic3, "topic3 can't be NULL");
        require(null != topic4, "topic4 can't be NULL");
        require(null != data, "data can't be NULL");

        Log log = new Log(ctx.getDestinationAddress().toBytes(), List.of(HashUtils.sha256(topic1.getUnderlying()), HashUtils.sha256(topic2.getUnderlying()),
                HashUtils.sha256(topic3.getUnderlying()), HashUtils.sha256(topic4.getUnderlying())), data.getUnderlying());
        ctx.getSideEffects().addLog(log);
    }

    @Override
    public ByteArray avm_blake2b(ByteArray data) {
        require(null != data, "Input data can't be NULL");

        return new ByteArray(HashUtils.blake2b(data.getUnderlying()));
    }

    @Override
    public ByteArray avm_sha256(ByteArray data){
        require(null != data, "Input data can't be NULL");

        return new ByteArray(HashUtils.sha256(data.getUnderlying()));
    }

    @Override
    public ByteArray avm_keccak256(ByteArray data){
        require(null != data, "Input data can't be NULL");

        return new ByteArray(HashUtils.keccak256(data.getUnderlying()));
    }

    @Override
    public void avm_revert() {
        throw new RevertException();
    }

    @Override
    public void avm_invalid() {
        throw new InvalidException();
    }

    @Override
    public void avm_require(boolean condition) {
        if (!condition) {
            throw new RevertException();
        }
    }

    @Override
    public void avm_print(org.aion.avm.shadow.java.lang.String message) {
        task.outputPrint(message.toString());
    }

    @Override
    public void avm_println(org.aion.avm.shadow.java.lang.String message) {
        task.outputPrintln(message.toString());
    }

    @Override
    public boolean avm_edVerify(ByteArray data, ByteArray signature, ByteArray publicKey) throws IllegalArgumentException {
        require(null != data, "Input data can't be NULL");
        require(null != signature, "Input signature can't be NULL");
        require(null != publicKey, "Input public key can't be NULL");

        return CryptoUtil.verifyEdDSA(data.getUnderlying(), signature.getUnderlying(), publicKey.getUnderlying());
    }

    private long restrictEnergyLimit(long energyLimit) {
        long remainingEnergy = IInstrumentation.attachedThreadInstrumentation.get().energyLeft();
        long maxAllowed = remainingEnergy - (remainingEnergy >> 6);
        return Math.min(maxAllowed, energyLimit);
    }

    private Result runInternalCall(InternalTransaction internalTx) {
        // add the internal transaction to result
        ctx.getSideEffects().addInternalTransaction(internalTx);

        IInstrumentation currentThreadInstrumentation = IInstrumentation.attachedThreadInstrumentation.get();
        if (null != this.reentrantState) {
            // Note that we want to save out the current nextHashCode.
            int nextHashCode = currentThreadInstrumentation.peekNextHashCode();
            this.reentrantState.updateEnvironment(nextHashCode);
        }
        // Temporarily detach from the DApp we were in.
        InstrumentationHelpers.temporarilyExitFrame(this.thisDAppSetup);

        TransactionContext internalCTX = new TransactionContextImpl(this.ctx, internalTx);

        // Acquire the target of the internal transaction
        org.aion.vm.api.interfaces.Address target = (internalCTX.getTransactionKind() == Type.CREATE.toInt())
            ? internalCTX.getContractAddress()
            : internalCTX.getDestinationAddress();
        avm.getResourceMonitor().acquire(target.toBytes(), task);

        // execute the internal transaction
        AvmTransactionResult newResult = null;
        try {
            newResult = this.avm.runInternalTransaction(this.kernel, this.task, internalCTX);
            
            // merge the results
            this.ctx.getSideEffects().merge(internalCTX.getSideEffects());
        } finally {
            // Re-attach.
            InstrumentationHelpers.returnToExecutingFrame(this.thisDAppSetup);
        }
        
        if (null != this.reentrantState) {
            // Update the next hashcode counter, in case this was a reentrant call and it was changed.
            currentThreadInstrumentation.forceNextHashCode(this.reentrantState.getEnvironment().nextHashCode);
        }

        // charge energy consumed
        currentThreadInstrumentation.chargeEnergy(newResult.getEnergyUsed());

        return new Result(newResult.getResultCode().isSuccess(),
                newResult.getReturnData() == null ? null : new ByteArray(newResult.getReturnData()));
    }
}

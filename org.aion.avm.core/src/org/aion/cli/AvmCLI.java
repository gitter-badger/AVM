package org.aion.cli;

import java.math.BigInteger;
import org.aion.avm.api.ABIEncoder;
import org.aion.avm.api.Address;
import org.aion.avm.core.CommonAvmFactory;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.avm.core.util.Helpers;
import org.aion.avm.core.util.StorageWalker;
import org.aion.avm.internal.RuntimeAssertionError;
import org.aion.cli.ArgumentParser.Action;
import org.aion.kernel.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.aion.vm.api.interfaces.SimpleFuture;
import org.aion.vm.api.interfaces.TransactionContext;
import org.aion.vm.api.interfaces.TransactionResult;
import org.aion.vm.api.interfaces.VirtualMachine;


public class AvmCLI {
    static Block block = new Block(new byte[32], 1, Helpers.randomAddress(), System.currentTimeMillis(), new byte[0]);

    public static TransactionContext setupOneDeploy(IEnvironment env, String storagePath, String jarPath, org.aion.vm.api.interfaces.Address sender, long energyLimit, BigInteger balance) {

        reportDeployRequest(env, storagePath, jarPath, sender);

        if (sender.toBytes().length != Address.LENGTH){
            throw env.fail("deploy : Invalid sender address");
        }

        File storageFile = new File(storagePath);

        KernelInterfaceImpl kernel = new KernelInterfaceImpl(storageFile);

        Path path = Paths.get(jarPath);
        byte[] jar;
        try {
            jar = Files.readAllBytes(path);
        }catch (IOException e){
            throw env.fail("deploy : Invalid location of Dapp jar");
        }

        Transaction createTransaction = Transaction.create(sender, kernel.getNonce(sender), balance, new CodeAndArguments(jar, null).encodeToBytes(), energyLimit, 1L);

        return new TransactionContextImpl(createTransaction, block);
    }

    public static void reportDeployRequest(IEnvironment env, String storagePath, String jarPath, org.aion.vm.api.interfaces.Address sender) {
        lineSeparator(env);
        env.logLine("DApp deployment request");
        env.logLine("Storage      : " + storagePath);
        env.logLine("Dapp Jar     : " + jarPath);
        env.logLine("Sender       : " + sender);
    }

    public static void reportDeployResult(IEnvironment env, TransactionResult createResult){
        String dappAddress = Helpers.bytesToHexString(createResult.getReturnData());
        env.noteRelevantAddress(dappAddress);
        
        lineSeparator(env);
        env.logLine("DApp deployment status");
        env.logLine("Result status: " + createResult.getResultCode().name());
        env.logLine("Dapp Address : " + dappAddress);
        env.logLine("Energy cost  : " + ((AvmTransactionResult) createResult).getEnergyUsed());
    }

    public static TransactionContext setupOneCall(IEnvironment env, String storagePath, org.aion.vm.api.interfaces.Address contract, org.aion.vm.api.interfaces.Address sender, String method, Object[] args, long energyLimit, long nonceBias, BigInteger balance) {
        reportCallRequest(env, storagePath, contract, sender, method, args);

        if (contract.toBytes().length != Address.LENGTH){
            throw env.fail("call : Invalid Dapp address ");
        }

        if (sender.toBytes().length != Address.LENGTH){
            throw env.fail("call : Invalid sender address");
        }

        byte[] arguments = ABIEncoder.encodeMethodArguments(method, args);

        File storageFile = new File(storagePath);

        KernelInterfaceImpl kernel = new KernelInterfaceImpl(storageFile);

        // TODO:  Remove this bias when/if we change this to no longer send all transactions from the same account.
        BigInteger biasedNonce = kernel.getNonce(sender).add(BigInteger.valueOf(nonceBias));
        Transaction callTransaction = Transaction.call(sender, contract, biasedNonce, balance, arguments, energyLimit, 1L);
        return new TransactionContextImpl(callTransaction, block);
    }

    public static TransactionContext setupOneTransfer(IEnvironment env, String storagePath, org.aion.vm.api.interfaces.Address address, org.aion.vm.api.interfaces.Address sender, long energyLimit, long nonceBias, BigInteger balance) {
        reportTransferRequest(env, storagePath, address, sender, balance);

        if (address.toBytes().length != Address.LENGTH){
            throw env.fail("call : Invalid receiver address ");
        }

        if (sender.toBytes().length != Address.LENGTH){
            throw env.fail("call : Invalid sender address");
        }

        File storageFile = new File(storagePath);

        KernelInterfaceImpl kernel = new KernelInterfaceImpl(storageFile);

        // TODO:  Remove this bias when/if we change this to no longer send all transactions from the same account.
        BigInteger biasedNonce = kernel.getNonce(sender).add(BigInteger.valueOf(nonceBias));
        Transaction callTransaction = Transaction.call(sender, address, biasedNonce, balance, new byte[0], energyLimit, 1L);
        return new TransactionContextImpl(callTransaction, block);
    }

    private static void reportCallRequest(IEnvironment env, String storagePath, org.aion.vm.api.interfaces.Address contract, org.aion.vm.api.interfaces.Address sender, String method, Object[] args){
        lineSeparator(env);
        env.logLine("DApp call request");
        env.logLine("Storage      : " + storagePath);
        env.logLine("Dapp Address : " + contract);
        env.logLine("Sender       : " + sender);
        env.logLine("Method       : " + method);
        env.logLine("Arguments    : ");
        for (int i = 0; i < args.length; i += 2){
            env.logLine("             : " + args[i]);
        }
    }

    private static void reportCallResult(IEnvironment env, TransactionResult callResult){
        lineSeparator(env);
        env.logLine("DApp call result");
        env.logLine("Result status: " + callResult.getResultCode().name());
        env.logLine("Return value : " + Helpers.bytesToHexString(callResult.getReturnData()));
        env.logLine("Energy cost  : " + ((AvmTransactionResult) callResult).getEnergyUsed());

        if (callResult.getResultCode() == AvmTransactionResult.Code.FAILED_EXCEPTION) {
            env.dumpThrowable(((AvmTransactionResult) callResult).getUncaughtException());
        }
    }

    private static void reportTransferRequest(IEnvironment env, String storagePath, org.aion.vm.api.interfaces.Address address, org.aion.vm.api.interfaces.Address sender, BigInteger balance){
        lineSeparator(env);
        env.logLine("Balance transfer request");
        env.logLine("Storage      : " + storagePath);
        env.logLine("address      : " + address);
        env.logLine("Sender       : " + sender);
        env.logLine("Balance      : " + balance);
    }

    private static void reportTransferResult(IEnvironment env, TransactionResult transferResult){
        lineSeparator(env);
        env.logLine("DApp balance transfer result");
        env.logLine("Result status: " + transferResult.getResultCode().name());
        env.logLine("Return value : " + Helpers.bytesToHexString(transferResult.getReturnData()));
        env.logLine("Energy cost  : " + ((AvmTransactionResult) transferResult).getEnergyUsed());

        if (transferResult.getResultCode() == AvmTransactionResult.Code.FAILED_EXCEPTION) {
            env.dumpThrowable(((AvmTransactionResult) transferResult).getUncaughtException());
        }
    }

    private static void lineSeparator(IEnvironment env){
        env.logLine("*******************************************************************************************");
    }

    public static void openAccount(IEnvironment env, String storagePath, org.aion.vm.api.interfaces.Address toOpen){
        lineSeparator(env);

        if (toOpen.toBytes().length != Address.LENGTH){
            throw env.fail("open : Invalid address to open");
        }

        env.logLine("Creating Account " + toOpen);

        File storageFile = new File(storagePath);
        KernelInterfaceImpl kernel = new KernelInterfaceImpl(storageFile);

        kernel.createAccount(toOpen);
        kernel.adjustBalance(toOpen, BigInteger.valueOf(100000000000L));

        env.logLine("Account Balance : " + kernel.getBalance(toOpen));
    }

    public static void exploreStorage(IEnvironment env, String storagePath, org.aion.vm.api.interfaces.Address dappAddress) {
        // Create the PrintStream abstraction that walkAllStaticsForDapp expects.
        // (ideally, we would incrementally filter this but that could be a later improvement - current Dapps are very small).
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        PrintStream printer = new PrintStream(stream);
        
        // Create the directory-backed kernel.
        KernelInterfaceImpl kernel = new KernelInterfaceImpl(new File(storagePath));
        
        // Walk everything, treating unexpected exceptions as fatal.
        try {
            StorageWalker.walkAllStaticsForDapp(printer, kernel, dappAddress);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException | IOException e) {
            // This tool can fail out if something goes wrong.
            throw env.fail(e.getMessage());
        }
        
        // Flush all the captured data and feed it to the env.
        printer.flush();
        String raw = new String(stream.toByteArray());
        env.logLine(raw);
    }

    public static void testingMain(IEnvironment env, String[] args) {
        internalMain(env, args);
    }

    static public void main(String[] args) {
        IEnvironment env = new IEnvironment() {
            @Override
            public RuntimeException fail(String message) {
                if (null != message) {
                    System.err.println(message);
                }
                System.exit(1);
                throw new RuntimeException();
            }
            @Override
            public void noteRelevantAddress(String address) {
                // This implementation doesn't care.
            }
            @Override
            public void logLine(String line) {
                System.out.println(line);
            }

            @Override
            public void dumpThrowable(Throwable throwable) {
                throwable.printStackTrace();
            }
        };
        internalMain(env, args);
    }

    private static void internalMain(IEnvironment env, String[] args) {
        ArgumentParser.Invocation invocation = ArgumentParser.parseArgs(args);

        if (null == invocation.errorString) {
            // There must be at least one command or there should have been a parse error (usually just defaulting to usage).
            RuntimeAssertionError.assertTrue(invocation.commands.size() > 0);

            if (!invocation.commands.get(0).action.equals(Action.BYTES) && !invocation.commands.get(0).action.equals(Action.ENCODE_CALL)) {
                // This logging line is largely just for test verification so it might be removed in the future.
                env.logLine("Running block with " + invocation.commands.size() + " transactions");
            }
            
            // Do the thing.
            // Before we run any command, make sure that the specified storage directory exists.
            // (we want the underlying storage engine to remain very passive so it should always expect that the directory was created for it).
            verifyStorageExists(env, invocation.storagePath);
            
            // See if this is a non-batching case or if we are just going to roll these into an AVM invocation.
            if (null != invocation.nonBatchingAction) {
                ArgumentParser.Command command = invocation.commands.get(0);
                switch (command.action) {
                case CALL:
                case DEPLOY:
                case TRANSFER:
                    // This should be in the batching path.
                    RuntimeAssertionError.unreachable("This should be in the batching path");
                    break;
                case EXPLORE:
                    exploreStorage(env, invocation.storagePath, AvmAddress.wrap(Helpers.hexStringToBytes(command.contractAddress)));
                    break;
                case OPEN:
                    openAccount(env, invocation.storagePath, AvmAddress.wrap(Helpers.hexStringToBytes(command.contractAddress)));
                    break;
                case BYTES:
                    try {
                        Path path = Paths.get(command.jarPath);
                        byte[] jar = Files.readAllBytes(path);
                        System.out.println(Helpers.bytesToHexString(
                            new CodeAndArguments(jar, new byte[0]).encodeToBytes()));
                    } catch (IOException e) {
                        System.out.println(e.toString());
                        System.exit(1);
                    }
                    break;
                case ENCODE_CALL:
                    Object[] callArgs = new Object[command.args.size()];
                    command.args.toArray(callArgs);
                    System.out.println(Helpers.bytesToHexString(ABIEncoder.encodeMethodArguments(command.method, callArgs)));
                    break;
                default:
                    throw new AssertionError("Unknown option");
                }
            } else {
                // Setup the transactions.
                TransactionContext[] transactions = new TransactionContext[invocation.commands.size()];
                for (int i = 0; i < invocation.commands.size(); ++i) {
                    ArgumentParser.Command command = invocation.commands.get(i);
                    switch (command.action) {
                    case CALL:
                        Object[] callArgs = new Object[command.args.size()];
                        command.args.toArray(callArgs);
                        transactions[i] = setupOneCall(
                            env,
                            invocation.storagePath,
                            AvmAddress.wrap(Helpers.hexStringToBytes(command.contractAddress)),
                            AvmAddress.wrap(Helpers.hexStringToBytes(command.senderAddress)),
                            command.method, callArgs,
                            command.energyLimit,
                            i,
                            command.balance);
                        break;
                    case DEPLOY:
                        transactions[i] = setupOneDeploy(
                            env,
                            invocation.storagePath,
                            command.jarPath,
                            AvmAddress.wrap(Helpers.hexStringToBytes(command.senderAddress)),
                            command.energyLimit,
                            command.balance);
                        break;
                    case TRANSFER:
                        Object[] transferArgs = new Object[command.args.size()];
                        command.args.toArray(transferArgs);
                        transactions[i] = setupOneTransfer(
                            env,
                            invocation.storagePath,
                            AvmAddress.wrap(Helpers.hexStringToBytes(command.contractAddress)),
                            AvmAddress.wrap(Helpers.hexStringToBytes(command.senderAddress)),
                            command.energyLimit,
                            i,
                            command.balance);
                        break;
                    case EXPLORE:
                    case OPEN:
                        // This should be in the non-batching path.
                        RuntimeAssertionError.unreachable("This should be in the batching path");
                        break;
                    default:
                        throw new AssertionError("Unknown option");
                    }
                }
                
                // Run them in a single batch.
                File storageFile = new File(invocation.storagePath);
                KernelInterfaceImpl kernel = new KernelInterfaceImpl(storageFile);
                VirtualMachine avm = CommonAvmFactory.buildAvmInstance(kernel);
                SimpleFuture<TransactionResult>[] futures = avm.run(transactions);
                TransactionResult[] results = new AvmTransactionResult[futures.length];
                for (int i = 0; i < futures.length; ++i) {
                    results[i] = futures[i].get();
                }
                avm.shutdown();
                
                // Finish up with reporting.
                for (int i = 0; i < invocation.commands.size(); ++i) {
                    ArgumentParser.Command command = invocation.commands.get(i);
                    switch (command.action) {
                    case CALL:
                        reportCallResult(env, results[i]);
                        break;
                    case DEPLOY:
                        reportDeployResult(env, results[i]);
                        break;
                    case TRANSFER:
                        reportTransferResult(env, results[i]);
                        break;
                    case EXPLORE:
                    case OPEN:
                        // This should be in the non-batching path.
                        RuntimeAssertionError.unreachable("This should be in the batching path");
                        break;
                    default:
                        throw new AssertionError("Unknown option");
                    }
                }
            }
        } else {
            env.fail(invocation.errorString);
        }
    }

    private static void verifyStorageExists(IEnvironment env, String storageRoot) {
        File directory = new File(storageRoot);
        if (!directory.isDirectory()) {
            boolean didCreate = directory.mkdirs();
            // Is this the best way to handle this failure?
            if (!didCreate) {
                // System.exit isn't ideal but we are very near the top of the entry-point so this shouldn't cause confusion.
                throw env.fail("Failed to create storage root: \"" + storageRoot + "\"");
            }
        }
    }
}

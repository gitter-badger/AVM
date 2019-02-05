package org.aion.cli;

import java.io.File;
import java.io.IOException;

import org.aion.avm.api.Address;
import org.aion.avm.core.ShadowCoverageTarget;
import org.aion.avm.core.dappreading.JarBuilder;
import org.aion.avm.core.util.Helpers;
import org.aion.kernel.AvmAddress;
import org.aion.kernel.KernelInterfaceImpl;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


public class AvmCLIIntegrationTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void usage() {
        TestEnvironment env = new TestEnvironment("Usage: AvmCLI [options] [command] [command options]");
        // We expect this to count as a failure so extract the string.
        String errorMessage = null;
        try {
            AvmCLI.testingMain(env, new String[0]);
        } catch (RuntimeException e) {
            errorMessage = e.getMessage();
        }
        Assert.assertEquals("Usage: AvmCLI [options] [command] [command options]", errorMessage);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddressArgumentInvalid() {
        String[] args = new String[]{"call", "0xFFFF", "--method", "method", "--args", "-A", "0x112233"};
        // We should fail on the next line.
        ArgumentParser.parseArgs(args);
    }

    @Test
    public void testAddressArgument() {
        String[] args = new String[]{"call", "0xFFFF", "--method", "method", "--args", "-A", "0x1122334455667788112233445566778811223344556677881122334455667788"};
        ArgumentParser.Invocation invocation = ArgumentParser.parseArgs(args);
        // Verify that the argument is an Address.
        Assert.assertTrue(invocation.commands.get(0).args.get(0) instanceof Address);
    }

    @Test
    public void exploreShadowCoverageTarget() throws Exception {
        // Create the JAR and write it to a location we can parse from the command-line.
        byte[] jar = JarBuilder.buildJarForMainAndClasses(ShadowCoverageTarget.class);
        File temp = this.folder.newFile();
        Helpers.writeBytesToFile(jar, temp.getAbsolutePath());

        // Create the testing environment to look for the successful deployment.
        TestEnvironment deployEnv = new TestEnvironment("Result status: SUCCESS");
        AvmCLI.testingMain(deployEnv, new String[] {"deploy", temp.getAbsolutePath()});
        Assert.assertTrue(deployEnv.didScrapeString);
        String dappAddress = deployEnv.capturedAddress;
        Assert.assertNotNull(dappAddress);

        // Now, issue a call.
        TestEnvironment callEnv = new TestEnvironment("Result status: SUCCESS");
        AvmCLI.testingMain(callEnv, new String[] {"call", dappAddress, "--method", "populate_JavaLang"});
        Assert.assertTrue(callEnv.didScrapeString);

        // Now, check the storage.
        // (note that this NPE is just something in an instance field, as an example of deep data).
        TestEnvironment exploreEnv = new TestEnvironment("NullPointerException(27):");
        AvmCLI.testingMain(exploreEnv, new String[] {"explore", dappAddress});
        Assert.assertTrue(exploreEnv.didScrapeString);
    }

    @Test
    public void callWithAddressArgument() throws Exception {
        // Create the JAR and write it to a location we can parse from the command-line.
        byte[] jar = JarBuilder.buildJarForMainAndClasses(LengthOfAddressTarget.class);
        File temp = this.folder.newFile();
        Helpers.writeBytesToFile(jar, temp.getAbsolutePath());
        
        // Create the testing environment to look for the successful deployment.
        TestEnvironment deployEnv = new TestEnvironment("Result status: SUCCESS");
        AvmCLI.testingMain(deployEnv, new String[] {"deploy", temp.getAbsolutePath()});
        Assert.assertTrue(deployEnv.didScrapeString);
        String dappAddress = deployEnv.capturedAddress;
        Assert.assertNotNull(dappAddress);
        
        // Now, issue a call.
        // (this value requires decoding since it is ABI-encoded but means:  "int(32)").
        TestEnvironment callEnv = new TestEnvironment("Return value : 0500000020");
        AvmCLI.testingMain(callEnv, new String[] {"call", dappAddress, "--method", "getAddressLength", "--args", "-A", dappAddress});
        Assert.assertTrue(callEnv.didScrapeString);
    }

    @Test
    public void callSimpleStackDemo() throws Exception {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleStackDemo.class);
        File temp = this.folder.newFile();
        Helpers.writeBytesToFile(jar, temp.getAbsolutePath());
        
        // Create the testing environment to look for the successful deployment.
        TestEnvironment deployEnv = new TestEnvironment("Result status: SUCCESS");
        AvmCLI.testingMain(deployEnv, new String[] {"deploy", temp.getAbsolutePath()});
        Assert.assertTrue(deployEnv.didScrapeString);
        String dappAddress = deployEnv.capturedAddress;
        Assert.assertNotNull(dappAddress);
        
        // Note that we are running this in-process, so we can't scoop the stdout.  Therefore, we just verify it was correct.
        // (this is only here until the other command-line is working).
        TestEnvironment callEnv = new TestEnvironment("Return value : void");
        AvmCLI.testingMain(callEnv, new String[] {"call", dappAddress, "--method", "addNewTuple", "--args", "-T", "test1"});
        Assert.assertTrue(callEnv.didScrapeString);
        AvmCLI.testingMain(callEnv, new String[] {"call", dappAddress, "--method", "addNewTuple", "--args", "-T", "test2"});
        Assert.assertTrue(callEnv.didScrapeString);
        AvmCLI.testingMain(callEnv, new String[] {"call", dappAddress, "--method", "addNewTuple", "--args", "-T", "test3"});
        Assert.assertTrue(callEnv.didScrapeString);
    }

    @Test
    public void parseFailOnExtraArgs() {
        String[] args = new String[]{"call", "0xFFFF", "--method", "method", "--args", "-A", "0x1122334455667788112233445566778811223344556677881122334455667788", "bogus_arg"};
        // Make sure that we see the complaint about the unknown arg.
        String message = null;
        try {
            ArgumentParser.parseArgs(args);
        } catch (IllegalArgumentException e) {
            message = e.getMessage();
        }
        Assert.assertEquals("Unknown argument: bogus_arg", message);
    }

    @Test
    public void multiSimpleStackDemo() throws Exception {
        byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleStackDemo.class);
        File temp = this.folder.newFile();
        Helpers.writeBytesToFile(jar, temp.getAbsolutePath());
        
        // Create the testing environment to look for the successful deployment.
        TestEnvironment deployEnv = new TestEnvironment("Running block with 1 transactions");
        AvmCLI.testingMain(deployEnv, new String[] {
                "deploy", temp.getAbsolutePath(),
        });
        Assert.assertTrue(deployEnv.didScrapeString);
        String dappAddress = deployEnv.capturedAddress;
        Assert.assertNotNull(dappAddress);
        
        // Note that we are running this in-process, so we can't scoop the stdout.  Therefore, we just verify it was correct.
        // (this is only here until the other command-line is working).
        TestEnvironment callEnv = new TestEnvironment("Running block with 2 transactions");
        AvmCLI.testingMain(callEnv, new String[] {
                "call", dappAddress, "--method", "addNewTuple", "--args", "-T", "test1_1",
                "call", dappAddress, "--method", "addNewTuple", "--args", "-T", "test1_2",
        });
        Assert.assertTrue(callEnv.didScrapeString);
        callEnv = new TestEnvironment("Running block with 1 transactions");
        AvmCLI.testingMain(callEnv, new String[] {
                "call", dappAddress, "--method", "addNewTuple", "--args", "-T", "test2_1",
        });
        Assert.assertTrue(callEnv.didScrapeString);
        callEnv = new TestEnvironment("Running block with 3 transactions");
        AvmCLI.testingMain(callEnv, new String[] {
                "call", dappAddress, "--method", "addNewTuple", "--args", "-T", "test3_1",
                "call", dappAddress, "--method", "addNewTuple", "--args", "-T", "test3_2",
                "call", dappAddress, "--method", "addNewTuple", "--args", "-T", "test3_3",
        });
        Assert.assertTrue(callEnv.didScrapeString);
    }

    @Test
    public void parseFailOnInvalidBatch() {
        String[] args = new String[] {
                "call", "0xFFFF", "--method", "addNewTuple", "--args", "-T", "test1_1",
                "explore", "0xFFFF",
        };
        // Make sure that we see the complaint about the use of batching on explore.
        String message = null;
        try {
            ArgumentParser.parseArgs(args);
        } catch (IllegalArgumentException e) {
            message = e.getMessage();
        }
        Assert.assertEquals("EXPLORE cannot be in a batch of commands", message);
        args = new String[] {
                "open", "--address", "0xFFFF",
                "call", "0xFFFF", "--method", "addNewTuple", "--args", "-T", "test1_1",
        };
        // Make sure that we see the complaint about the use of batching on open.
        message = null;
        try {
            ArgumentParser.parseArgs(args);
        } catch (IllegalArgumentException e) {
            message = e.getMessage();
        }
        Assert.assertEquals("OPEN cannot be in a batch of commands", message);
    }


    private static class TestEnvironment implements IEnvironment {
        public final String requiredScrape;
        public String capturedAddress;
        public boolean didScrapeString;

        public TestEnvironment(String requiredScrape) {
            this.requiredScrape = requiredScrape;
        }

        @Override
        public RuntimeException fail(String message) {
            throw new RuntimeException(message);
        }
        @Override
        public void noteRelevantAddress(String address) {
            this.capturedAddress = address;
        }
        @Override
        public void logLine(String line) {
            if (null != this.requiredScrape) {
                if (line.contains(this.requiredScrape)) {
                    this.didScrapeString = true;
                }
            }
        }

        @Override
        public void dumpThrowable(Throwable throwable) {

        }
    }

    @Test
    public void callWithTransfer() throws IOException {
        final int deployBalance = 100000;
        final int transferBalance = 5000;
        String storagePath = "./storage";
        File storageFile = new File(storagePath);
        KernelInterfaceImpl kernelInterface = new KernelInterfaceImpl(storageFile);
        java.math.BigInteger contractBalance;

        //deploy a contract first
        byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleStackDemo.class);
        File temp = this.folder.newFile();
        Helpers.writeBytesToFile(jar, temp.getAbsolutePath());

        // Create the testing environment to look for the successful deployment.
        TestEnvironment deployEnv = new TestEnvironment("Result status: SUCCESS");
        AvmCLI.testingMain(deployEnv, new String[] {"deploy", temp.getAbsolutePath(), "--value", Integer.toString(deployBalance)});
        String dappAddress = deployEnv.capturedAddress;

        // check that contract has been deployed and balance has been transferred
        contractBalance = kernelInterface.getBalance(AvmAddress.wrap(Helpers.hexStringToBytes(dappAddress)));
        System.out.println("new balance after deploy with transfer: " + contractBalance);
        Assert.assertEquals(deployBalance, contractBalance.intValue());

        // now lets try to make some calls
        TestEnvironment callEnv = new TestEnvironment("Return value : void");
        AvmCLI.testingMain(callEnv, new String[] {"call", dappAddress, "--method", "addNewTuple", "--args", "-T", "test1", "--value", Integer.toString(transferBalance)});
        Assert.assertTrue(callEnv.didScrapeString);

        // check that the call was successful and more balance has been added to the contract
        contractBalance = kernelInterface.getBalance(AvmAddress.wrap(Helpers.hexStringToBytes(dappAddress)));
        System.out.println("new balance after call with transfer: " + contractBalance);
        Assert.assertEquals(deployBalance + transferBalance, contractBalance.intValue());

        System.out.println("Contract balance: " + contractBalance);
    }

    @Test
    public void testDeployAndCallWithNoTransfer() throws IOException {
        String storagePath = "./storage";
        File storageFile = new File(storagePath);
        KernelInterfaceImpl kernelInterface = new KernelInterfaceImpl(storageFile);

        byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleStackDemo.class);
        File temp = this.folder.newFile();
        Helpers.writeBytesToFile(jar, temp.getAbsolutePath());

        TestEnvironment deployEnv = new TestEnvironment("Result status: SUCCESS");
        AvmCLI.testingMain(deployEnv, new String[] {"deploy", temp.getAbsolutePath()});
        Assert.assertTrue(deployEnv.didScrapeString);

        String dappAddress = deployEnv.capturedAddress;

        // check for balance of 0
        java.math.BigInteger contractBalance = kernelInterface.getBalance(AvmAddress.wrap(Helpers.hexStringToBytes(dappAddress)));
        System.out.println("Contract balance: " + contractBalance);
        Assert.assertEquals(0,contractBalance.intValue());

        // now lets try to make some calls
        TestEnvironment callEnv = new TestEnvironment("Return value : void");
        AvmCLI.testingMain(callEnv, new String[] {"call", dappAddress, "--method", "addNewTuple", "--args", "-T", "test1"});
        Assert.assertTrue(callEnv.didScrapeString);

        // check that the call was successful and more balance has been added to the contract
        contractBalance = kernelInterface.getBalance(AvmAddress.wrap(Helpers.hexStringToBytes(dappAddress)));
        System.out.println("new balance after call with transfer: " + contractBalance);
        Assert.assertEquals(0, contractBalance.intValue());

        System.out.println("Contract balance: " + contractBalance);
    }

    @Test
    public void testDeployTransfer() throws IOException {
        final int deployBalance = 100000;
        String storagePath = "./storage";
        File storageFile = new File(storagePath);
        KernelInterfaceImpl kernelInterface = new KernelInterfaceImpl(storageFile);

        byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleStackDemo.class);
        File temp = this.folder.newFile();
        Helpers.writeBytesToFile(jar, temp.getAbsolutePath());

        TestEnvironment deployEnv = new TestEnvironment("Result status: SUCCESS");
        AvmCLI.testingMain(deployEnv, new String[] {"deploy", temp.getAbsolutePath(), "--value", Integer.toString(deployBalance)});
        Assert.assertTrue(deployEnv.didScrapeString);

        String dappAddress = deployEnv.capturedAddress;

        // check for balance of deployBalance
        java.math.BigInteger contractBalance = kernelInterface.getBalance(AvmAddress.wrap(Helpers.hexStringToBytes(dappAddress)));
        System.out.println("Contract balance: " + contractBalance);
        Assert.assertEquals(deployBalance,contractBalance.intValue());
    }

    @Test (expected = NumberFormatException.class)
    public void testDeployWithInvalidBalanceTransfer() throws IOException {
        String invalidBalance = "123abc";

        byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleStackDemo.class);
        File temp = this.folder.newFile();
        Helpers.writeBytesToFile(jar, temp.getAbsolutePath());

        TestEnvironment deployEnv = new TestEnvironment("Result status: SUCCESS");
        AvmCLI.testingMain(deployEnv, new String[] {"deploy", temp.getAbsolutePath(), "--value", invalidBalance});
        Assert.assertTrue(deployEnv.didScrapeString);
    }

    @Test
    public void testTransferToContract() throws IOException {
        final int transferBalance = 20000;
        String storagePath = "./storage";
        File storageFile = new File(storagePath);
        KernelInterfaceImpl kernelInterface = new KernelInterfaceImpl(storageFile);

        byte[] jar = JarBuilder.buildJarForMainAndClasses(SimpleStackDemo.class);
        File temp = this.folder.newFile();
        Helpers.writeBytesToFile(jar, temp.getAbsolutePath());

        // deploy first, with no balance
        TestEnvironment deployEnv = new TestEnvironment("Result status: SUCCESS");
        AvmCLI.testingMain(deployEnv, new String[] {"deploy", temp.getAbsolutePath()});
        Assert.assertTrue(deployEnv.didScrapeString);

        String dappAddress = deployEnv.capturedAddress;

        // check for balance of 0
        java.math.BigInteger contractBalance = kernelInterface.getBalance(AvmAddress.wrap(Helpers.hexStringToBytes(dappAddress)));
        System.out.println("Contract balance: " + contractBalance);
        Assert.assertEquals(0,contractBalance.intValue());

        // do transfer only
        TestEnvironment callEnv = new TestEnvironment("Return value : void");
        AvmCLI.testingMain(callEnv, new String[] {"transfer", dappAddress, "--value", Integer.toString(transferBalance)});
        Assert.assertTrue(callEnv.didScrapeString);

        // check for balance of transferBalance
        contractBalance = kernelInterface.getBalance(AvmAddress.wrap(Helpers.hexStringToBytes(dappAddress)));
        System.out.println("Contract balance: " + contractBalance);
        Assert.assertEquals(transferBalance,contractBalance.intValue());
    }

    @Test
    public void testTransferToAccount() {
        final int transferBalance = 20000;
        String storagePath = "./storage";
        File storageFile = new File(storagePath);
        KernelInterfaceImpl kernelInterface = new KernelInterfaceImpl(storageFile);

        org.aion.vm.api.interfaces.Address address = Helpers.randomAddress();
        kernelInterface.createAccount(address);

        // do transfer only
        TestEnvironment callEnv = new TestEnvironment("Return value : void");
        AvmCLI.testingMain(callEnv, new String[] {"transfer", address.toString(), "--value", Integer.toString(transferBalance)});
        Assert.assertTrue(callEnv.didScrapeString);

        // check for balance of transferBalance
        java.math.BigInteger contractBalance = kernelInterface.getBalance(AvmAddress.wrap(Helpers.hexStringToBytes(address.toString())));
        System.out.println("Contract balance: " + contractBalance);
        Assert.assertEquals(transferBalance,contractBalance.intValue());
    }
}

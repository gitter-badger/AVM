package org.aion.avm.core;

import org.aion.avm.core.dappreading.JarBuilder;
import org.aion.avm.core.testExchange.IAionToken;
import org.aion.avm.core.testWallet.Abi;
import org.aion.avm.core.testWallet.ByteArrayHelpers;
import org.aion.avm.core.testWallet.ByteArrayWrapper;
import org.aion.avm.core.testWallet.BytesKey;
import org.aion.avm.core.testWallet.Daylimit;
import org.aion.avm.core.testWallet.EventLogger;
import org.aion.avm.core.testWallet.Multiowned;
import org.aion.avm.core.testWallet.Operation;
import org.aion.avm.core.testWallet.RequireFailedException;
import org.aion.avm.core.testWallet.Wallet;
import org.aion.avm.core.testExchange.PepeCoin;
import org.aion.avm.core.testExchange.PepeController;
import org.aion.avm.core.testExchange.MemeCoin;
import org.aion.avm.core.testExchange.MemeController;
import org.aion.avm.core.testExchange.AionTokenAbi;
import org.aion.avm.core.util.Helpers;
import org.aion.avm.userlib.AionList;
import org.aion.avm.userlib.AionMap;
import org.aion.avm.userlib.AionSet;
import org.aion.avm.api.Address;
import org.aion.kernel.Block;
import org.aion.kernel.TransactionContext;
import org.aion.kernel.TransactionContextImpl;
import org.aion.kernel.Transaction;
import org.aion.kernel.TransactionResult;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;


/**
 * Our current thinking is that we will use a JUnit launcher for the proof-of-concept demonstration.  This is that entry-point.
 * See issue-124 for more of the background.
 */
@RunWith(Enclosed.class)
public class ProofOfConceptTest {

    public static class POCWallet {

        // For now, we will just reuse the from, to, and block for each call (in the future, this will change).
        private byte[] from = Helpers.randomBytes(Address.LENGTH);
        private byte[] to = Helpers.randomBytes(Address.LENGTH);
        private Block block = new Block(1, Helpers.randomBytes(Address.LENGTH), System.currentTimeMillis(), new byte[0]);
        private long energyLimit = 5_000_000;

        private byte[] buildTestWalletJar() {
            return JarBuilder.buildJarForMainAndClasses(Wallet.class
                    , Multiowned.class
                    , AionMap.class
                    , AionSet.class
                    , AionList.class
                    , ByteArrayWrapper.class
                    , Operation.class
                    , ByteArrayHelpers.class
                    , BytesKey.class
                    , RequireFailedException.class
                    , Daylimit.class
                    , EventLogger.class
                    , Abi.class
            );
        }

        /**
         * Tests that a deploy call will store the code for the Wallet JAR.
         * This means that it transformed it correctly and nothing was missing.
         */
        @Test
        public void testDeployWritesCode() {
            byte[] testWalletJar = buildTestWalletJar();

            Transaction createTransaction = new Transaction(Transaction.Type.CREATE, from, to, 0, testWalletJar, energyLimit);
            TransactionContext createContext = new TransactionContextImpl(createTransaction, block);
            TransactionResult createResult = new AvmImpl().run(createContext);

            Assert.assertEquals(TransactionResult.Code.SUCCESS, createResult.getStatusCode());
            Assert.assertNotNull(createContext.getTransformedCode(to));
        }

        /**
         * Tests that we can run init on the deployed code, albeit as a second transaction (since we haven't yet decided how to invoke init on deploy).
         */
        @Test
        public void testDeployAndCallInit() {
            // Constructor args.
            byte[] extra1 = Helpers.randomBytes(Address.LENGTH);
            byte[] extra2 = Helpers.randomBytes(Address.LENGTH);
            int requiredVotes = 2;
            long dailyLimit = 5000;

            byte[] testWalletJar = buildTestWalletJar();
            Transaction createTransaction = new Transaction(Transaction.Type.CREATE, from, to, 0, testWalletJar, energyLimit);
            TransactionContext createContext = new TransactionContextImpl(createTransaction, block);
            TransactionResult createResult = new AvmImpl().run(createContext);
            Assert.assertEquals(TransactionResult.Code.SUCCESS, createResult.getStatusCode());

            // contract address is stored in return data
            byte[] contractAddress = createResult.getReturnData();

            byte[] initArgs = encodeInit(extra1, extra2, requiredVotes, dailyLimit);
            Transaction initTransaction = new Transaction(Transaction.Type.CALL, from, contractAddress, 0, initArgs, energyLimit);
            TransactionContext initContext = new TransactionContextImpl(initTransaction, block);
            TransactionResult initResult = new AvmImpl().run(initContext);
            Assert.assertEquals(TransactionResult.Code.SUCCESS, initResult.getStatusCode());
        }


        /**
         * Note that this is copied from CallEncoder to allow us to create the input without needing to instantiate Address objects.
         */
        public static byte[] encodeInit(byte[] extra1, byte[] extra2, int requiredVotes, long dailyLimit) {
            byte[] onto = new byte[1 + Integer.BYTES + Address.LENGTH + Address.LENGTH + Integer.BYTES + Long.BYTES];
            Abi.Encoder encoder = Abi.buildEncoder(onto);
            // We are encoding the Addresses as a 2-element array, so describe it that way to the encoder.
            encoder
                    .encodeByte(Abi.kWallet_init)
                    .encodeInt(2)
                    .encodeRemainder(extra1)
                    .encodeRemainder(extra2)
                    .encodeInt(requiredVotes)
                    .encodeLong(dailyLimit);
            return onto;
        }
    }

    public static class POCExchange {

        private Block block = new Block(1, Helpers.randomBytes(Address.LENGTH), System.currentTimeMillis(), new byte[0]);
        private long energyLimit = 5_000_000;

        private byte[] pepeCoinAddr = Helpers.randomBytes(Address.LENGTH);
        private byte[] memeCoinAddr = Helpers.randomBytes(Address.LENGTH);
        private byte[] pepeMinter = Helpers.randomBytes(Address.LENGTH);
        private byte[] memeMinter = Helpers.randomBytes(Address.LENGTH);
        private byte[] owner = Helpers.randomBytes(Address.LENGTH);
        private byte[] usr1 = Helpers.randomBytes(Address.LENGTH);
        private byte[] usr2 = Helpers.randomBytes(Address.LENGTH);
        private byte[] usr3 = Helpers.randomBytes(Address.LENGTH);

        class CoinContract{
            private byte[] addr;
            private byte[] minter;

            CoinContract(byte[] contractAddr, byte[] minter, byte[] jar){
                this.addr = contractAddr;
                this.minter = minter;
                initCoin(jar);
            }

            private byte[] initCoin(byte[] jar){
                Transaction createTransaction = new Transaction(Transaction.Type.CREATE, minter, pepeCoinAddr, 0, jar, energyLimit);
                TransactionContext createContext = new TransactionContextImpl(createTransaction, block);
                TransactionResult createResult = new AvmImpl().run(createContext);
                Assert.assertEquals(TransactionResult.Code.SUCCESS, createResult.getStatusCode());
                return createResult.getReturnData();
            }

            public TransactionResult callTotalSupply() {
                byte[] args = new byte[1];
                AionTokenAbi.Encoder encoder = AionTokenAbi.buildEncoder(args);
                encoder.encodeByte(AionTokenAbi.kICO_totalSupply);

                Transaction callTransaction = new Transaction(Transaction.Type.CALL, minter, addr, 0, args, energyLimit);
                TransactionContext callContext = new TransactionContextImpl(callTransaction, block);
                TransactionResult callResult = new AvmImpl().run(callContext);
                Assert.assertEquals(TransactionResult.Code.SUCCESS, callResult.getStatusCode());
                return callResult;
            }

            private TransactionResult callBalanceOf(byte[] toQuery) {
                byte[] args = new byte[1 + Address.LENGTH];
                AionTokenAbi.Encoder encoder = AionTokenAbi.buildEncoder(args);
                encoder.encodeByte(AionTokenAbi.kICO_balanceOf);
                encoder.encodeAddress(new Address(toQuery));

                Transaction callTransaction = new Transaction(Transaction.Type.CALL, minter, addr, 0, args, energyLimit);
                TransactionContext callContext = new TransactionContextImpl(callTransaction, block);
                TransactionResult callResult = new AvmImpl().run(callContext);
                Assert.assertEquals(TransactionResult.Code.SUCCESS, callResult.getStatusCode());
                return callResult;
            }

            private TransactionResult callOpenAccount(byte[] toOpen) {
                byte[] args = new byte[1 + Address.LENGTH];
                AionTokenAbi.Encoder encoder = AionTokenAbi.buildEncoder(args);
                encoder.encodeByte(AionTokenAbi.kICO_openAccount);
                encoder.encodeAddress(new Address(toOpen));

                Transaction callTransaction = new Transaction(Transaction.Type.CALL, minter, addr, 0, args, energyLimit);
                TransactionContext callContext = new TransactionContextImpl(callTransaction, block);
                TransactionResult callResult = new AvmImpl().run(callContext);
                Assert.assertEquals(TransactionResult.Code.SUCCESS, callResult.getStatusCode());
                return callResult;
            }

            private TransactionResult callMint(byte[] receiver) {
                byte[] args = new byte[1 + Address.LENGTH + Long.BYTES];
                AionTokenAbi.Encoder encoder = AionTokenAbi.buildEncoder(args);
                encoder.encodeByte(AionTokenAbi.kICO_mint);
                encoder.encodeAddress(new Address(receiver));
                encoder.encodeLong(5000L);

                Transaction callTransaction = new Transaction(Transaction.Type.CALL, minter, addr, 0, args, energyLimit);
                TransactionContext callContext = new TransactionContextImpl(callTransaction, block);
                TransactionResult callResult = new AvmImpl().run(callContext);
                Assert.assertEquals(TransactionResult.Code.SUCCESS, callResult.getStatusCode());
                return callResult;
            }

            private TransactionResult callTransfer(byte[] sender, byte[] receiver, long amount) {
                byte[] args = new byte[1 + Address.LENGTH + Long.BYTES];
                AionTokenAbi.Encoder encoder = AionTokenAbi.buildEncoder(args);
                encoder.encodeByte(AionTokenAbi.kICO_transfer);
                encoder.encodeAddress(new Address(receiver));
                encoder.encodeLong(amount);

                Transaction callTransaction = new Transaction(Transaction.Type.CALL, sender, addr, 0, args, energyLimit);
                TransactionContext callContext = new TransactionContextImpl(callTransaction, block);
                TransactionResult callResult = new AvmImpl().run(callContext);
                Assert.assertEquals(TransactionResult.Code.SUCCESS, callResult.getStatusCode());
                return callResult;
            }

            private TransactionResult callAllowance(byte[] owner, byte[] spender) {
                byte[] args = new byte[1 + Address.LENGTH + Address.LENGTH];
                AionTokenAbi.Encoder encoder = AionTokenAbi.buildEncoder(args);
                encoder.encodeByte(AionTokenAbi.kICO_allowance);
                encoder.encodeAddress(new Address(owner));
                encoder.encodeAddress(new Address(spender));

                Transaction callTransaction = new Transaction(Transaction.Type.CALL, minter, addr, 0, args, energyLimit);
                TransactionContext callContext = new TransactionContextImpl(callTransaction, block);
                TransactionResult callResult = new AvmImpl().run(callContext);
                Assert.assertEquals(TransactionResult.Code.SUCCESS, callResult.getStatusCode());
                return callResult;
            }

            private TransactionResult callApprove(byte[] owner, byte[] spender, long amount) {
                byte[] args = new byte[1 + Address.LENGTH + Long.BYTES];
                AionTokenAbi.Encoder encoder = AionTokenAbi.buildEncoder(args);
                encoder.encodeByte(AionTokenAbi.kICO_approve);
                encoder.encodeAddress(new Address(spender));
                encoder.encodeLong(amount);

                Transaction callTransaction = new Transaction(Transaction.Type.CALL, owner, addr, 0, args, energyLimit);
                TransactionContext callContext = new TransactionContextImpl(callTransaction, block);
                TransactionResult callResult = new AvmImpl().run(callContext);
                Assert.assertEquals(TransactionResult.Code.SUCCESS, callResult.getStatusCode());
                return callResult;
            }

            private TransactionResult callTransferFrom(byte[] executor, byte[] from, byte[] to, long amount) {
                byte[] args = new byte[1 + Address.LENGTH + Address.LENGTH + Long.BYTES];
                AionTokenAbi.Encoder encoder = AionTokenAbi.buildEncoder(args);
                encoder.encodeByte(AionTokenAbi.kICO_transferFrom);
                encoder.encodeAddress(new Address(from));
                encoder.encodeAddress(new Address(to));
                encoder.encodeLong(amount);

                Transaction callTransaction = new Transaction(Transaction.Type.CALL, executor, addr, 0, args, energyLimit);
                TransactionContext callContext = new TransactionContextImpl(callTransaction, block);
                TransactionResult callResult = new AvmImpl().run(callContext);
                Assert.assertEquals(TransactionResult.Code.SUCCESS, callResult.getStatusCode());
                return callResult;
            }


        }

        private byte[] buildPepeJar() {
            return JarBuilder.buildJarForMainAndClasses(PepeController.class,
                    IAionToken.class,
                    PepeCoin.class,
                    AionTokenAbi.class,
                    AionMap.class,
                    ByteArrayHelpers.class
            );
        }

        private byte[] buildMemeJar() {
            return JarBuilder.buildJarForMainAndClasses(MemeController.class,
                    IAionToken.class,
                    MemeCoin.class,
                    AionTokenAbi.class,
                    AionMap.class,
                    ByteArrayHelpers.class
            );
        }

        @Test
        public void testERC20() {

            TransactionResult res;

            CoinContract pepe = new CoinContract(pepeCoinAddr, pepeMinter, buildPepeJar());

            res = pepe.callTotalSupply();

            Assert.assertEquals(PepeCoin.TOTAL_SUPPLY, ByteArrayHelpers.decodeLong(res.getReturnData()));

            res = pepe.callBalanceOf(usr1);

            Assert.assertEquals(-1L, ByteArrayHelpers.decodeLong(res.getReturnData()));

            res = pepe.callOpenAccount(usr1);

            Assert.assertEquals(true, ByteArrayHelpers.decodeBoolean(res.getReturnData()));

            res = pepe.callBalanceOf(usr1);

            Assert.assertEquals(0L, ByteArrayHelpers.decodeLong(res.getReturnData()));

            res = pepe.callOpenAccount(usr1);

            Assert.assertEquals(false, ByteArrayHelpers.decodeBoolean(res.getReturnData()));

            res = pepe.callBalanceOf(usr2);

            Assert.assertEquals(-1L, ByteArrayHelpers.decodeLong(res.getReturnData()));

            res = pepe.callOpenAccount(usr2);

            Assert.assertEquals(true, ByteArrayHelpers.decodeBoolean(res.getReturnData()));

            res = pepe.callOpenAccount(usr3);

            Assert.assertEquals(true, ByteArrayHelpers.decodeBoolean(res.getReturnData()));

            res = pepe.callMint(usr1);

            Assert.assertEquals(true, ByteArrayHelpers.decodeBoolean(res.getReturnData()));

            res = pepe.callBalanceOf(usr1);

            Assert.assertEquals(5000L, ByteArrayHelpers.decodeLong(res.getReturnData()));

            res = pepe.callMint(usr2);

            Assert.assertEquals(true, ByteArrayHelpers.decodeBoolean(res.getReturnData()));

            res = pepe.callMint(usr2);

            Assert.assertEquals(true, ByteArrayHelpers.decodeBoolean(res.getReturnData()));

            res = pepe.callBalanceOf(usr2);

            Assert.assertEquals(10000L, ByteArrayHelpers.decodeLong(res.getReturnData()));

            res = pepe.callTransfer(usr1, usr2, 2000L);

            Assert.assertEquals(true, ByteArrayHelpers.decodeBoolean(res.getReturnData()));

            res = pepe.callBalanceOf(usr1);

            Assert.assertEquals(3000L, ByteArrayHelpers.decodeLong(res.getReturnData()));

            res = pepe.callBalanceOf(usr2);

            Assert.assertEquals(12000L, ByteArrayHelpers.decodeLong(res.getReturnData()));

            res = pepe.callAllowance(usr1, usr2);

            Assert.assertEquals(0L, ByteArrayHelpers.decodeLong(res.getReturnData()));

            res = pepe.callApprove(usr1, usr3, 1000L);

            Assert.assertEquals(true, ByteArrayHelpers.decodeBoolean(res.getReturnData()));

            res = pepe.callAllowance(usr1, usr3);

            Assert.assertEquals(1000L, ByteArrayHelpers.decodeLong(res.getReturnData()));

            res = pepe.callTransferFrom(usr3, usr1, usr2, 500L);

            Assert.assertEquals(true, ByteArrayHelpers.decodeBoolean(res.getReturnData()));

            res = pepe.callAllowance(usr1, usr3);

            Assert.assertEquals(500L, ByteArrayHelpers.decodeLong(res.getReturnData()));

            res = pepe.callBalanceOf(usr1);

            Assert.assertEquals(2500L, ByteArrayHelpers.decodeLong(res.getReturnData()));

            res = pepe.callBalanceOf(usr2);

            Assert.assertEquals(12500L, ByteArrayHelpers.decodeLong(res.getReturnData()));
        }

        @Test
        public void testExchange() {
            CoinContract pepe = new CoinContract(pepeCoinAddr, pepeMinter, buildPepeJar());
            CoinContract meme = new CoinContract(memeCoinAddr, memeMinter, buildMemeJar());
        }
    }

}

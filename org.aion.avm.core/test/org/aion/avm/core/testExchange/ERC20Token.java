package org.aion.avm.core.testExchange;

import org.aion.avm.api.Address;
import org.aion.avm.api.BlockchainRuntime;
import org.aion.avm.userlib.AionMap;

public class ERC20Token implements ERC20 {

    private final String name;
    private final String symbol;
    private final int decimals;

    private final Address minter;

    private AionMap<Address, Long> ledger;

    private AionMap<Address, AionMap<Address, Long>> allowance;

    private long totalSupply;

    public ERC20Token(String name, String symbol, int decimals, Address minter) {
        this.name = name;
        this.symbol = symbol;
        this.decimals = decimals;
        this.minter = minter;
        this.ledger = new AionMap<>();
        this.allowance = new AionMap<>();
    }

    public String name() {
        return name;
    }

    public String symbol() {
        return symbol;
    }

    public int decimals() {
        return decimals;
    }

    public long totalSupply() {
        return totalSupply;
    }

    public long balanceOf(Address tokenOwner) {
        return this.ledger.getOrDefault(tokenOwner, 0L);
    }

    public long allowance(Address tokenOwner, Address spender) {
        if (!this.allowance.containsKey(tokenOwner)) {
            return 0L;
        }

        return this.allowance.get(tokenOwner).getOrDefault(spender, 0L);
    }

    public boolean transfer(Address receiver, long tokens) {
        Address sender = BlockchainRuntime.getCaller();

        long senderBalance = this.ledger.getOrDefault(sender, 0L);
        long receiverBalance = this.ledger.getOrDefault(receiver, 0L);

        if ((senderBalance >= tokens) && (tokens > 0) && (receiverBalance + tokens > 0)) {
            this.ledger.put(sender, senderBalance - tokens);
            this.ledger.put(receiver, receiverBalance + tokens);
            BlockchainRuntime.log("Transfer".getBytes(), sender.unwrap(), receiver.unwrap(), Long.toString(tokens).getBytes());
            return true;
        }

        return false;
    }

    public boolean approve(Address spender, long tokens) {
        Address sender = BlockchainRuntime.getCaller();

        if (!this.allowance.containsKey(sender)) {
            AionMap<Address, Long> newEntry = new AionMap<>();
            this.allowance.put(sender, newEntry);
        }

        BlockchainRuntime.log("Approval".getBytes(), sender.unwrap(), spender.unwrap(), Long.toString(tokens).getBytes());
        this.allowance.get(sender).put(spender, tokens);

        return true;
    }

    public boolean transferFrom(Address from, Address to, long tokens) {
        Address sender = BlockchainRuntime.getCaller();

        long fromBalance = this.ledger.getOrDefault(from, 0L);
        long toBalance = this.ledger.getOrDefault(to, 0L);

        long limit = allowance(from, sender);

        if ((fromBalance > tokens) && (limit > tokens) && (toBalance + tokens > 0)) {
            BlockchainRuntime.log("Transfer".getBytes(), from.unwrap(), to.unwrap(), Long.toString(tokens).getBytes());
            this.ledger.put(from, fromBalance - tokens);
            this.allowance.get(from).put(sender, limit - tokens);
            this.ledger.put(to, toBalance + tokens);
            return true;
        }

        return false;
    }

    public boolean mint(Address receiver, long tokens) {
        if (BlockchainRuntime.getCaller().equals(this.minter)) {
            long receiverBalance = this.ledger.getOrDefault(receiver, 0L);
            if ((tokens > 0) && (receiverBalance + tokens > 0)) {
                BlockchainRuntime.log("Mint".getBytes(), receiver.unwrap(), Long.toString(tokens).getBytes());
                this.ledger.put(receiver, receiverBalance + tokens);
                this.totalSupply += tokens;
                return true;
            }
        }
        return false;
    }
}

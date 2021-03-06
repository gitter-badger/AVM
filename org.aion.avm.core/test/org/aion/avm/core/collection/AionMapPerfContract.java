package org.aion.avm.core.collection;

import org.aion.avm.api.ABIDecoder;
import org.aion.avm.api.BlockchainRuntime;
import org.aion.avm.userlib.AionMap;
import org.aion.avm.userlib.AionPlainMap;

public class AionMapPerfContract {

    public static int SIZE = 5000;

    public static AionMap<Integer, Integer> target;

    public static AionPlainMap<Integer, Integer> targetB;

    static{
        target = new AionMap<>();
        targetB = new AionPlainMap<>();
    }

    public static byte[] main() {
        return ABIDecoder.decodeAndRunWithClass(AionMapPerfContract.class, BlockchainRuntime.getData());
    }

    public static void callInit(){
        for (int i = 0; i < SIZE; i++){
            target.put(Integer.valueOf(i * 2), Integer.valueOf(i));
        }
    }

    public static void callPut(){
        for (int i = 0; i < SIZE; i++) {
            target.put(Integer.valueOf(i * 2 + 1), Integer.valueOf(i));
        }
    }

    public static void callGet(){
        for (int i = 0; i < SIZE; i++) {
            target.get(Integer.valueOf(i));
        }
    }

    public static void callRemove(){
        for (int i = 0; i < SIZE; i++) {
            target.remove(Integer.valueOf(i * 2));
        }
    }

    public static void callInitB(){
        for (int i = 0; i < SIZE; i++){
            targetB.put(Integer.valueOf(i * 2), Integer.valueOf(i));
        }
    }

    public static void callPutB(){
        for (int i = 0; i < SIZE; i++) {
            targetB.put(Integer.valueOf(i * 2 + 1), Integer.valueOf(i));
        }
    }

    public static void callGetB(){
        for (int i = 0; i < SIZE; i++) {
            targetB.get(Integer.valueOf(i));
        }
    }

    public static void callRemoveB(){
        for (int i = 0; i < SIZE; i++) {
            targetB.remove(Integer.valueOf(i * 2));
        }
    }
}

package org.aion.avm.shadow.java.lang;

import org.aion.avm.internal.IDeserializer;
import org.aion.avm.internal.IInstrumentation;
import org.aion.avm.internal.IObject;
import org.aion.avm.internal.IPersistenceToken;
import org.aion.avm.RuntimeMethodFeeSchedule;

public final class Byte extends Object implements Comparable<Byte> {
    static {
        // Shadow classes MUST be loaded during bootstrap phase.
        IInstrumentation.attachedThreadInstrumentation.get().bootstrapOnly();
    }

    public static final byte avm_MIN_VALUE = java.lang.Byte.MIN_VALUE;

    public static final byte avm_MAX_VALUE = java.lang.Byte.MAX_VALUE;

    public static final Class<Byte> avm_TYPE = new Class(java.lang.Byte.TYPE);;

    public static String avm_toString(byte b) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_toString);
        return Integer.avm_toString((int)b, 10);
    }

    public static Byte avm_valueOf(byte b) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_valueOf);
        return new Byte(b);
    }

    public static byte avm_parseByte(String s, int radix){
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_parseByte);
        return java.lang.Byte.parseByte(s.getUnderlying(), radix);
    }

    public static byte avm_parseByte(String s) throws NumberFormatException {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_parseByte_1);
        return avm_parseByte(s, 10);
    }

    public static Byte avm_valueOf(String s, int radix)
            throws NumberFormatException {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_valueOf_1);
        return avm_valueOf(avm_parseByte(s, radix));
    }

    public static Byte avm_valueOf(String s) throws NumberFormatException {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_valueOf_2);
        return avm_valueOf(s, 10);
    }

    public static Byte avm_decode(String nm) throws NumberFormatException {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_decode);
        return new Byte(java.lang.Byte.decode(nm.getUnderlying()).byteValue());
    }

    private Byte(byte v) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_constructor);
        this.v = v;
    }

    private Byte(String s) throws NumberFormatException {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_constructor_1);
        this.v = avm_parseByte(s, 10);
    }

    public byte avm_byteValue() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_byteValue);
        return v;
    }

    public short avm_shortValue() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_shortValue);
        return (short) v;
    }

    public int avm_intValue() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_intValue);
        return (int) v;
    }

    public long avm_longValue() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_longValue);
        return (long) v;
    }

    public float avm_floatValue() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_floatValue);
        return (float) v;
    }

    public double avm_doubleValue() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_doubleValue);
        return (double) v;
    }

    public String avm_toString() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_toString_1);
        return Integer.avm_toString((int) v);
    }

    @Override
    public int avm_hashCode() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_hashCode);
        return Byte.avm_hashCode(v);
    }

    public static int avm_hashCode(byte value) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_hashCode_1);
        return (int)value;
    }

    public boolean avm_equals(IObject obj) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_equals);
        if (obj instanceof Byte) {
            return v == ((Byte)obj).avm_byteValue();
        }
        return false;
    }

    public int avm_compareTo(Byte anotherByte) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_compareTo);
        return avm_compare(this.v, anotherByte.v);
    }

    public static int avm_compare(byte x, byte y) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_compare);
        return x - y;
    }

    public static int avm_compareUnsigned(byte x, byte y) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_compareUnsigned);
        return Byte.avm_toUnsignedInt(x) - Byte.avm_toUnsignedInt(y);
    }

    public static int avm_toUnsignedInt(byte x) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_toUnsignedInt);
        return ((int) x) & 0xff;
    }

    public static long avm_toUnsignedLong(byte x) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Byte_avm_toUnsignedLong);
        return ((long) x) & 0xffL;
    }

    public static final int avm_SIZE = java.lang.Byte.SIZE;

    public static final int avm_BYTES = java.lang.Byte.BYTES;

    //=======================================================
    // Methods below are used by runtime and test code only!
    //========================================================

    public Byte(IDeserializer deserializer, IPersistenceToken persistenceToken) {
        super(deserializer, persistenceToken);
        lazyLoad();
    }

    private byte v;

    public byte getUnderlying() {
        return this.v;
    }

    //========================================================
    // Methods below are excluded from shadowing
    //========================================================

}

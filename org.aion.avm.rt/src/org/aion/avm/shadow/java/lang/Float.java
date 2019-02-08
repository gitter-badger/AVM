package org.aion.avm.shadow.java.lang;

import org.aion.avm.internal.IDeserializer;
import org.aion.avm.internal.IInstrumentation;
import org.aion.avm.internal.IObject;
import org.aion.avm.internal.IPersistenceToken;
import org.aion.avm.RuntimeMethodFeeSchedule;

public class Float extends Number implements Comparable<Float> {
    static {
        // Shadow classes MUST be loaded during bootstrap phase.
        IInstrumentation.attachedThreadInstrumentation.get().bootstrapOnly();
    }

    public static final float avm_POSITIVE_INFINITY = java.lang.Float.POSITIVE_INFINITY;

    public static final float avm_NEGATIVE_INFINITY = java.lang.Float.NEGATIVE_INFINITY;

    public static final float avm_NaN = java.lang.Float.NaN;

    public static final float avm_MAX_VALUE = java.lang.Float.MAX_VALUE;

    public static final float avm_MIN_NORMAL = java.lang.Float.MIN_NORMAL;

    public static final float avm_MIN_VALUE = java.lang.Float.MIN_VALUE;

    public static final int avm_MAX_EXPONENT = java.lang.Float.MAX_EXPONENT;

    public static final int avm_MIN_EXPONENT = java.lang.Float.MIN_EXPONENT;

    public static final int avm_SIZE = java.lang.Float.SIZE;

    public static final int avm_BYTES = java.lang.Float.BYTES;

    public static final Class<Float> avm_TYPE = new Class(java.lang.Float.TYPE);

    private Float(float f) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_constructor);
        this.v = f;
    }

    private Float(double f) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_constructor);
        this.v = (float)f;
    }

    private Float(String f) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_constructor);
        this.v = java.lang.Float.parseFloat(f.getUnderlying());
    }

    public static String avm_toString(float f){
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_toString);
        return new String(java.lang.Float.toString(f));
    }

    public static String avm_toHexString(float a){
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_toHexString);
        return new String(java.lang.Float.toHexString(a));
    }

    public static Float avm_valueOf(String s) throws NumberFormatException {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_valueOf);
        return new Float(avm_parseFloat(s));
    }

    public static Float avm_valueOf(float f) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_valueOf_1);
        return new Float(f);
    }

    public static float avm_parseFloat(String s) throws NumberFormatException {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_parseFloat);
        return java.lang.Float.parseFloat(s.getUnderlying());
    }

    public static boolean avm_isNaN(float v) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_isNaN);
        return (v != v);
    }

    public static boolean avm_isInfinite(float v) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_isInfinite);
        return (v == avm_POSITIVE_INFINITY) || (v == avm_NEGATIVE_INFINITY);
    }

    public static boolean avm_isFinite(float f) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_isFinite);
        return Math.avm_abs(f) <= Float.avm_MAX_VALUE;
    }

    public boolean avm_isNaN() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_isNaN_1);
        return avm_isNaN(v);
    }

    public boolean avm_isInfinite() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_isInfinite_1);
        return avm_isInfinite(v);
    }

    public String avm_toString() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_toString_1);
        return Float.avm_toString(v);
    }

    public byte avm_byteValue() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_byteValue);
        return (byte) v;
    }

    public short avm_shortValue() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_shortValue);
        return (short) v;
    }

    public int avm_intValue() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_intValue);
        return (int) v;
    }

    public long avm_longValue() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_longValue);
        return (long) v;
    }

    public float avm_floatValue() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_floatValue);
        return v;
    }

    public double avm_doubleValue() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_doubleValue);
        return (double) v;
    }

    public int avm_hashCode() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_hashCode);
        return Float.avm_hashCode(v);
    }

    public static int avm_hashCode(float value) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_hashCode_1);
        return avm_floatToIntBits(value);
    }

    public boolean avm_equals(IObject obj) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_equals);
        return (obj instanceof Float)
                && (avm_floatToIntBits(((Float)obj).v) == avm_floatToIntBits(v));
    }

    public static int avm_floatToIntBits(float value) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_floatToIntBits);
        return java.lang.Float.floatToIntBits(value);
    }

    public static int avm_floatToRawIntBits(float value){
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_floatToRawIntBits);
        return java.lang.Float.floatToRawIntBits(value);
    }

    public static float avm_intBitsToFloat(int bits){
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_intBitsToFloat);
        return java.lang.Float.intBitsToFloat(bits);
    }

    public int avm_compareTo(Float anotherFloat) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_compareTo);
        return Float.avm_compare(v, anotherFloat.v);
    }

    public static int avm_compare(float f1, float f2) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_compare);
        return java.lang.Float.compare(f1, f2);
    }

    public static float avm_sum(float a, float b) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_sum);
        return a + b;
    }

    public static float avm_max(float a, float b) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_max);
        return Math.avm_max(a, b);
    }

    public static float avm_min(float a, float b) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.Float_avm_min);
        return Math.avm_min(a, b);
    }


    //========================================================
    // Methods below are used by runtime and test code only!
    //========================================================

    public Float(IDeserializer deserializer, IPersistenceToken persistenceToken) {
        super(deserializer, persistenceToken);
    }

    private float v;

    public float getUnderlying() {
        return this.v;
    }

    //========================================================
    // Methods below are excluded from shadowing
    //========================================================


}

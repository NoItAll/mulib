package de.wwu.mulib.transform_and_execute.examples.generated_methods_check;


// We need to make sure, primitive Mulib wrappers are initialized: https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html
public class NeedsToPreinitializeFields {
    private long l;
    private byte b;

    public NeedsToPreinitializeFields() {}

    public NeedsToPreinitializeFields(int i0) {
        l = 25;
    }

    public NeedsToPreinitializeFields(int i0, int i1) {
        b = 25;
    }

    public NeedsToPreinitializeFields(int i0, int i1, int i3) {
        l = 25;
        b = 25;
    }

    public static NeedsToPreinitializeFields calc0() {
        NeedsToPreinitializeFields result = new NeedsToPreinitializeFields();
        result.l++;
        result.b++;
        return result;
    }

    public static NeedsToPreinitializeFields calc1(NeedsToPreinitializeFields needsToPreinitializeFields) {
        NeedsToPreinitializeFields result = new NeedsToPreinitializeFields();
        result.l += needsToPreinitializeFields.b;
        result.b += needsToPreinitializeFields.l;
        return result;
    }

    public static NeedsToPreinitializeFields calc2() {
        NeedsToPreinitializeFields result = new NeedsToPreinitializeFields(1);
        result.l++;
        result.b++;
        return result;
    }

    public static NeedsToPreinitializeFields calc3() {
        NeedsToPreinitializeFields result = new NeedsToPreinitializeFields(1, 2);
        result.l++;
        result.b++;
        return result;
    }

    public static NeedsToPreinitializeFields calc4() {
        NeedsToPreinitializeFields result = new NeedsToPreinitializeFields(1, 2, 3);
        result.l++;
        result.b++;
        return result;
    }
}

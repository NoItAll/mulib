package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.substitutions.PartnerClass;
import de.wwu.mulib.substitutions.Sarray;

public abstract class AbstractValueFactory implements ValueFactory {

    public AbstractValueFactory(MulibConfig config) {}

    /// TODO For now, symbolic arrays are always treated the same for concolic and symbolic
    @Override
    public Sarray.SintSarray sintSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        return new Sarray.SintSarray(len, se, freeElements);
    }

    @Override
    public Sarray.SdoubleSarray sdoubleSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        return new Sarray.SdoubleSarray(len, se, freeElements);
    }

    @Override
    public Sarray.SfloatSarray sfloatSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        return new Sarray.SfloatSarray(len, se, freeElements);
    }

    @Override
    public Sarray.SlongSarray slongSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        return new Sarray.SlongSarray(len, se, freeElements);
    }

    @Override
    public Sarray.SshortSarray sshortSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        return new Sarray.SshortSarray(len, se, freeElements);
    }

    @Override
    public Sarray.SbyteSarray sbyteSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        return new Sarray.SbyteSarray(len, se, freeElements);
    }

    @Override
    public Sarray.SboolSarray sboolSarray(SymbolicExecution se, Sint len, boolean freeElements) {
        return new Sarray.SboolSarray(len, se, freeElements);
    }

    @Override
    public Sarray.PartnerClassSarray partnerClassSarray(SymbolicExecution se, Sint len, Class<PartnerClass> clazz, boolean freeElements) {
        return new Sarray.PartnerClassSarray(clazz, len, se, freeElements);
    }

    @Override
    public Sarray.SarraySarray sarraySarray(SymbolicExecution se, Sint len, Class<Sarray> clazz, boolean freeElements) {
        return new Sarray.SarraySarray(clazz, len, se, freeElements);
    }

    @Override
    public Sint.ConcSint concSint(int i) {
        return (Sint.ConcSint) Sint.concSint(i);
    }

    @Override
    public Sdouble.ConcSdouble concSdouble(double d) {
        return (Sdouble.ConcSdouble) Sdouble.concSdouble(d);
    }

    @Override
    public Sfloat.ConcSfloat concSfloat(float f) {
        return (Sfloat.ConcSfloat) Sfloat.concSfloat(f);
    }

    @Override
    public Sbool.ConcSbool concSbool(boolean b) {
        return (Sbool.ConcSbool) Sbool.concSbool(b);
    }

    @Override
    public Slong.ConcSlong concSlong(long l) {
        return (Slong.ConcSlong) Slong.concSlong(l);
    }

    @Override
    public Sshort.ConcSshort concSshort(short s) {
        return (Sshort.ConcSshort) Sshort.concSshort(s);
    }

    @Override
    public Sbyte.ConcSbyte concSbyte(byte b) {
        return (Sbyte.ConcSbyte) Sbyte.concSbyte(b);
    }
}

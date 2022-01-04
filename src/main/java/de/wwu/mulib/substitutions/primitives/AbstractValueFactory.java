package de.wwu.mulib.substitutions.primitives;

import de.wwu.mulib.MulibConfig;

public abstract class AbstractValueFactory implements ValueFactory {

    public AbstractValueFactory(MulibConfig config) {

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

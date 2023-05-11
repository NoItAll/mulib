package de.wwu.mulib.annotations;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes(
        {"de.wwu.mulib.annotations.Maximize", "de.wwu.mulib.annotations.Minimize",
        "de.wwu.mulib.annotations.SearchRegion", "de.wwu.mulib.annotations.TestDriver"})
public class MulibAnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        return false; //// TODO
    }

}

/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright 2019-2019 the original author or authors.
 */

package org.quickperf;

import org.quickperf.annotation.DisableGlobalAnnotations;
import org.quickperf.config.SpecifiableAnnotations;
import org.quickperf.config.library.SetOfAnnotationConfigs;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public class AnnotationsExtractor {

    public static final AnnotationsExtractor INSTANCE = new AnnotationsExtractor();

    private final AnnotationsMerger annotationsMerger = AnnotationsMerger.INSTANCE;

    private final Collection<Annotation> globalAnnotations;

    private AnnotationsExtractor() {
        globalAnnotations = retrieveDefaultAnnotations();
    }

    private Collection<Annotation> retrieveDefaultAnnotations() {

        try {
            SpecifiableAnnotations specifiableAnnotations =
                    classSpecifyingGlobalAnnotation();
            if (specifiableAnnotations == null) {
                return Collections.emptyList();
            }
            return specifiableAnnotations.specifyAnnotationsAppliedOnEachTest();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

    }

    public SpecifiableAnnotations classSpecifyingGlobalAnnotation(){

        QuickPerfUserConfigClasses quickPerfUserConfigClasses = QuickPerfUserConfigClasses.INSTANCE;

        Class[] classes;
        try {
            classes = quickPerfUserConfigClasses.findClasses();
        } catch (ClassNotFoundException | IOException e) {
            throw new IllegalStateException(e);
        }
        for (Class clazz : classes) {
            Class[] interfaces = clazz.getInterfaces();
            for (Class interfaceClass : interfaces) {
                if (isSpecifiableAnnotationsClass(interfaceClass)) {
                    try {
                        return (SpecifiableAnnotations) clazz.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }

                }
            }
        }
        return null;
    }

    private boolean isSpecifiableAnnotationsClass(Class interfaceClass) {
        return interfaceClass.getCanonicalName().equals(SpecifiableAnnotations.class.getCanonicalName());
    }

    public Annotation[] extractAnnotationsFor(Method testMethod, SetOfAnnotationConfigs testAnnotationConfigs) {
        Annotation[] classAnnotations = testMethod.getDeclaringClass().getAnnotations();
        Annotation[] methodAnnotations = testMethod.getAnnotations();
        Annotation[] mergedAnnotations = annotationsMerger.merge(globalAnnotations
                                                                , classAnnotations
                                                                , methodAnnotations);

        Collection<Annotation> quickPerfAnnotations = testAnnotationConfigs.keepQuickPerfAnnotationsIn(mergedAnnotations);

        List<Annotation> resultAsList
                = testAnnotationConfigs.removeDisabledAndAndDisablingAnnotationsIn(quickPerfAnnotations);

        return resultAsList.toArray(new Annotation[0]);
    }

    static class AnnotationsMerger {

        static final AnnotationsMerger INSTANCE = new AnnotationsMerger();

        private AnnotationsMerger() {}

        Annotation[] merge(Collection<? extends Annotation> globalAnnotations
                , Annotation[] classAnnotations
                , Annotation[] methodAnnotations
        ) {

            Collection<? extends Annotation> filteredDefaultAnnotations;
            if(  disableDefaultAnnotationContainedIn(methodAnnotations)
              || disableDefaultAnnotationContainedIn(classAnnotations) ) {
                filteredDefaultAnnotations = Collections.emptyList();
            } else {
                filteredDefaultAnnotations = globalAnnotations;
            }

            List<Annotation> methodAnnotationsAsList = Arrays.asList(methodAnnotations);
            List<Annotation> mergedAnnotationsAsList = new ArrayList<>(methodAnnotationsAsList);

            List<Annotation> classAnnotationAsList = Arrays.asList(classAnnotations);
            List<Annotation> classAnnotationsNotInMethodAnnotations =
                    retrieveAnnotationsOfFirstParamNotInAnnotationsOfSecondParam(classAnnotationAsList
                            , methodAnnotationsAsList);
            mergedAnnotationsAsList.addAll(classAnnotationsNotInMethodAnnotations);

            List<Annotation> globalAnnotationsToKeep =
                    retrieveAnnotationsOfFirstParamNotInAnnotationsOfSecondParam(filteredDefaultAnnotations
                            , mergedAnnotationsAsList);
            mergedAnnotationsAsList.addAll(globalAnnotationsToKeep);

            return convertToArray(removeDisableDefaultAnnotationIn(mergedAnnotationsAsList));

        }

        private List<Annotation> removeDisableDefaultAnnotationIn(List<Annotation> annotations) {
            List<Annotation> methodAnnotationsWithoutDisableAnnotation = new ArrayList<>();
            for (Annotation annotation : annotations) {
                if(!isDisableGlobalAnnotation(annotation)) {
                    methodAnnotationsWithoutDisableAnnotation.add(annotation);
                }
            }
            return methodAnnotationsWithoutDisableAnnotation;
        }

        private boolean disableDefaultAnnotationContainedIn(Annotation[] methodAnnotations) {
            boolean methodAnnotationsContainDisableDefaultAnnotation = false;
            for (Annotation annotation : methodAnnotations) {
                if(isDisableGlobalAnnotation(annotation)) {
                    methodAnnotationsContainDisableDefaultAnnotation = true;
                }
            }
            return methodAnnotationsContainDisableDefaultAnnotation;
        }

        private boolean isDisableGlobalAnnotation(Annotation annotation) {
            return annotation.annotationType().equals(DisableGlobalAnnotations.class);
        }

        private List<Annotation> retrieveAnnotationsOfFirstParamNotInAnnotationsOfSecondParam(Collection<? extends Annotation> annotationsOfFirstParam
                , Collection<? extends Annotation> annotationsOfSecondParam) {
            List<Annotation> result = new ArrayList<>();

            Collection<Class<? extends Annotation>> classesOfAnnotationsOfSecondParam =
                    retrieveClassesOf(annotationsOfSecondParam);

            for (Annotation oneAnnotationOfFirstParam : annotationsOfFirstParam) {
                Class<? extends Annotation> classOfOneAnnotationOfFirstParam = oneAnnotationOfFirstParam.annotationType();
                if (!classesOfAnnotationsOfSecondParam.contains(classOfOneAnnotationOfFirstParam)) {
                    result.add(oneAnnotationOfFirstParam);
                }
            }

            return result;

        }

        private Collection<Class<? extends Annotation>> retrieveClassesOf(Collection<? extends Annotation> annotations) {
            List<Class<? extends Annotation>> classes = new ArrayList<>(annotations.size());
            for (Annotation annotation : annotations) {
                classes.add(annotation.annotationType());
            }
            return classes;
        }

        private Annotation[] convertToArray(List mergedAnnotationsAsList) {
            int size = mergedAnnotationsAsList.size();
            return (Annotation[]) mergedAnnotationsAsList.toArray(new Annotation[size]);
        }

    }

}

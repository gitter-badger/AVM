package org.aion.avm.core;

import org.aion.avm.core.classgeneration.CommonGenerators;
import org.aion.avm.core.classloading.AvmClassLoader;
import org.aion.avm.core.classloading.AvmSharedClassLoader;
import org.aion.avm.core.util.Assert;
import org.aion.avm.core.util.Helpers;
import org.aion.avm.rt.BlockchainRuntime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class SimpleAvm {

    private final BlockchainRuntime rt;
    private final Map<String, byte[]> classes;

    private final AvmClassLoader loader;
    private final Set<String> transformedClassNames;

    public SimpleAvm(BlockchainRuntime rt, Class<?>... classes) {
        this.rt = rt;
        this.classes = new HashMap<>();

        Stream.of(classes).forEach(clazz -> this.classes.put(clazz.getName(),
                Helpers.loadRequiredResourceAsBytes(clazz.getName().replaceAll("\\.", "/") + ".class")));

        // build shared class loader
        AvmSharedClassLoader sharedClassLoader = new AvmSharedClassLoader(CommonGenerators.generateExceptionShadowsAndWrappers());

        // build class hierarchy
        HierarchyTreeBuilder builder = new HierarchyTreeBuilder();
        this.classes.entrySet().stream().forEach(e -> {
            try {
                // NOTE: we load the class to figure out the super class instead of by static analysis.
                Class<?> clazz = SimpleAvm.class.getClassLoader().loadClass(e.getKey());

                builder.addClass(e.getKey(), clazz.getSuperclass().getName(), e.getValue());
            } catch (ClassNotFoundException ex) {
                Assert.unexpected(ex);
            }
        });
        Forest<String, byte[]> classHierarchy = builder.asMutableForest();

        // create a new AVM
        AvmImpl avm = new AvmImpl(sharedClassLoader);

        // compute object sizes
        Map<String, Integer> runtimeObjectSizes = AvmImpl.computeRuntimeObjectSizes();
        Map<String, Integer> allObjectSizes = AvmImpl.computeObjectSizes(classHierarchy, runtimeObjectSizes);

        // transform classes
        Map<String, byte[]> transformedClasses = avm.transformClasses(this.classes, classHierarchy, allObjectSizes);
        Map<String, byte[]> finalContractClasses = Helpers.mapIncludingHelperBytecode(transformedClasses);
        this.loader = new AvmClassLoader(sharedClassLoader, finalContractClasses);
        this.transformedClassNames = Collections.unmodifiableSet(transformedClasses.keySet());

        // set up helper
        Helpers.instantiateHelper(loader, rt);
    }


    public BlockchainRuntime getBlockchainRuntime() {
        return rt;
    }

    public Map<String, byte[]> getClasses() {
        return classes;
    }

    public AvmClassLoader getClassLoader() {
        return loader;
    }

    public Set<String> getTransformedClassNames() {
        return this.transformedClassNames;
    }
}
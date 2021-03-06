package org.aion.avm.core;

import org.aion.avm.internal.CommonInstrumentation;
import org.aion.avm.internal.IInstrumentation;
import org.aion.avm.internal.IInstrumentationFactory;
import org.aion.vm.api.interfaces.KernelInterface;
import org.aion.vm.api.interfaces.VirtualMachine;


/**
 * Since issue-303 required the creation of IInstrumentationFactory, this helper class exists to cover the common case of our tests:  just using CommonInstrumentation.
 */
public class CommonAvmFactory {
    public static AvmImpl buildAvmInstance(KernelInterface kernelInterface) {
        // We use the common instrumentation for this case.
        IInstrumentationFactory factory = new CommonInstrumentationFactory();
        return NodeEnvironment.singleton.buildAvmInstance(factory, kernelInterface, false);
    }

    public static AvmImpl buildAvmInstanceInDebugMode(KernelInterface kernelInterface) {
        IInstrumentationFactory factory = new CommonInstrumentationFactory();
        return NodeEnvironment.singleton.buildAvmInstance(factory, kernelInterface, true);
    }

    private static class CommonInstrumentationFactory implements IInstrumentationFactory {
        @Override
        public IInstrumentation createInstrumentation() {
            return new CommonInstrumentation();
        }
        @Override
        public void destroyInstrumentation(IInstrumentation instance) {
            // Implementation requires no cleanup.
        }
    }
}

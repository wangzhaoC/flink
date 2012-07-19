package eu.stratosphere.sopremo.function;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.stratosphere.sopremo.AbstractSopremoType;
import eu.stratosphere.sopremo.Bindings;
import eu.stratosphere.sopremo.EvaluationContext;
import eu.stratosphere.sopremo.EvaluationException;
import eu.stratosphere.sopremo.ISerializableSopremoType;
import eu.stratosphere.sopremo.packages.FunctionRegistryCallback;
import eu.stratosphere.sopremo.packages.Registry;
import eu.stratosphere.sopremo.type.ArrayNode;
import eu.stratosphere.sopremo.type.IJsonNode;
import eu.stratosphere.util.reflect.ReflectUtil;

public class MethodRegistry extends AbstractSopremoType implements ISerializableSopremoType, Registry<SopremoMethod> {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8399369017331739066L;

	private final Bindings bindings;

	public MethodRegistry(final Bindings bindings) {
		this.bindings = bindings;
	}

	public MethodRegistry() {
		this(new Bindings());
	}

	/*
	 * (non-Javadoc)
	 * @see eu.stratosphere.sopremo.Registry#find(java.lang.String)
	 */
	@Override
	public SopremoMethod find(String functionName) {
		return this.bindings.get(functionName, SopremoMethod.class);
	}

	Map<String, SopremoMethod> getRegisteredFunctions() {
		return this.bindings.getAll(SopremoMethod.class);
	}

	private static boolean isCompatibleSignature(final Method method) {
		if (!IJsonNode.class.isAssignableFrom(method.getReturnType()))
			return false;

		boolean compatibleSignature;
		final Class<?>[] parameterTypes = method.getParameterTypes();
		if (parameterTypes.length == 1 && parameterTypes[0].isArray()
			&& IJsonNode.class.isAssignableFrom(parameterTypes[0].getComponentType()))
			compatibleSignature = true;
		else {
			compatibleSignature = true;
			for (int index = 0; index < parameterTypes.length; index++)
				if (!IJsonNode.class.isAssignableFrom(parameterTypes[index])
					&& !(index == parameterTypes.length - 1 && method.isVarArgs() &&
					IJsonNode.class.isAssignableFrom(parameterTypes[index].getComponentType()))) {
					compatibleSignature = false;
					break;
				}
		}
		return compatibleSignature;
	}

	public void register(final Class<?> javaFunctions) {
		final List<Method> functions = getCompatibleMethods(
			ReflectUtil.getMethods(javaFunctions, null, Modifier.STATIC | Modifier.PUBLIC));

		for (final Method method : functions)
			this.registerInternal(method);

		if (FunctionRegistryCallback.class.isAssignableFrom(javaFunctions))
			((FunctionRegistryCallback) ReflectUtil.newInstance(javaFunctions)).registerFunctions(this);
	}

	public void register(final Method method) {
		this.registerInternal(method);
	}

	private void registerInternal(final Method method) {
		JavaMethod javaFunction = this.bindings.get(method.getName(), JavaMethod.class);
		if (javaFunction == null)
			this.bindings.set(method.getName(), javaFunction = new JavaMethod(method.getName()));
		javaFunction.addSignature(method);
	}

	public static List<Method> getCompatibleMethods(final List<Method> methods) {
		final List<Method> functions = new ArrayList<Method>();
		for (final Method method : methods)
			if (isCompatibleSignature(method))
				functions.add(method);
		return functions;
	}

	@Override
	public void register(String name, final SopremoMethod function) {
		this.bindings.set(name, function);
	}

	public void register(final SopremoMethod function) {
		register(function.getName(), function);
	}

	/*
	 * (non-Javadoc)
	 * @see eu.stratosphere.sopremo.SopremoType#toString(java.lang.StringBuilder)
	 */
	@Override
	public void toString(final StringBuilder builder) {
		this.bindings.toString(builder);
	}
}

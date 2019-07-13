package com.moka.lib.internals;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.moka.lib.internals.ExceptionSugar.propagate;

public final class Reflection {

    private static final Class[] EMPTY_CLASS_ARRAY = new Class[]{};

    private Reflection() {
    }

    @SuppressWarnings({"TypeParameterUnusedInFormals"})
    public static <T> T getStaticFieldValue(String clazz, final String fieldName) {
        return getFieldValue(null, clazz(clazz, Thread.currentThread().getContextClassLoader()), fieldName);
    }

    @SuppressWarnings({"TypeParameterUnusedInFormals"})
    public static <T> T getFieldValue(Object o, String clazz, final String fieldName) {
        return getFieldValue(o, clazz(clazz, o.getClass().getClassLoader()), fieldName);
    }

    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    public static <T> T getFieldValue(Object o, Class<?> clazz, final String fieldName) {
        try {
            Field get = clazz.getDeclaredField(fieldName);
            get.setAccessible(true);
            return (T) get.get(o);
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException e) {
            throw propagate(e);
        }
    }

    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    public static <T> T getFieldValue(Object o, final String fieldName) {
        return getFieldValue(o, o.getClass(), fieldName);
    }

    @SuppressWarnings("unchecked")
    public static void setFieldValue(Object o, final String fieldName, Object value) {
        setFieldValue(o, o.getClass(), fieldName, value);
    }

    @SuppressWarnings("unchecked")
    public static void setFieldValue(Object o, Class<?> clazz, final String fieldName, Object value) {
        try {
            Field set = clazz.getDeclaredField(fieldName);
            set.setAccessible(true);
            set.set(o, value);
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException e) {
            throw propagate(e);
        }
    }

    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    public static <T> T invokeStatic(final String aClass, final String methodName) {
        try {
            return invokeStatic(Reflection.class.getClassLoader().loadClass(aClass), methodName);
        } catch (ClassNotFoundException | IllegalArgumentException e) {
            throw propagate(e);
        }
    }

    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    public static <T> T invokeStatic(final Class<?> aClass, final String methodName) {
        try {
            Method get = aClass.getDeclaredMethod(methodName);
            get.setAccessible(true);
            return (T) get.invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw propagate(e);
        }
    }

    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    public static <T> T invokeStatic(final Class<?> aClass, final String methodName, final Class[] parameterTypes, final Object... args) {
        try {
            Method get = aClass.getDeclaredMethod(methodName, parameterTypes);
            get.setAccessible(true);
            return (T) get.invoke(null, args);
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw propagate(e);
        }
    }

    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    public static <T> T invoke(Object instance, final String methodName, final Class[] parameterTypes, final Object... args) {
        return invoke(instance, instance.getClass(), methodName, parameterTypes, args);
    }

    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    public static <T> T invoke(Object instance, final Class<?> aClass, final String methodName, final Class[] parameterTypes, final Object... args) {
        try {
            Method get = aClass.getDeclaredMethod(methodName, parameterTypes);
            get.setAccessible(true);
            return (T) get.invoke(instance, args);
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw propagate(e);
        }
    }

    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    public static <T> T newInstance(String s, final ClassLoader classLoader) {
        return newInstance(s, classLoader, EMPTY_CLASS_ARRAY);
    }

    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    public static <T> T newInstance(String s, final ClassLoader classLoader, final Class[] parameterTypes, final Object... args) {
        try {
            Class<?> clazz = clazz(s, classLoader);
            Constructor constructor = clazz.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return (T) constructor.newInstance(args);
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw propagate(e);
        }
    }

    public static Class<?> clazz(String s, final ClassLoader classLoader) {
        try {
            return Class.forName(s, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw propagate(e);
        }
    }
}

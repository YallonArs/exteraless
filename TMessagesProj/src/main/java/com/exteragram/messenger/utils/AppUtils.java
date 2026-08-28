package com.exteragram.messenger.utils;

import androidx.annotation.Keep;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.telegram.messenger.FileLog;

import java.lang.reflect.Field;

/** Шим {@code com.exteragram.messenger.utils.AppUtils}: то, что плагины зовут по имени exteraGram. */
public final class AppUtils {

    private static volatile Gson gson;

    private AppUtils() {
    }

    public static Gson getGson() {
        if (gson == null) {
            synchronized (AppUtils.class) {
                if (gson == null) {
                    gson = new GsonBuilder()
                            .setPrettyPrinting()
                            .serializeSpecialFloatingPointValues()
                            .addSerializationExclusionStrategy(new ExclusionStrategy() {
                                @Override
                                public boolean shouldSkipClass(Class<?> clazz) {
                                    return isPlatformPackage(clazz);
                                }

                                @Override
                                public boolean shouldSkipField(FieldAttributes attributes) {
                                    return isPlatformPackage(attributes.getDeclaringClass());
                                }
                            })
                            .create();
                }
            }
        }
        return gson;
    }

    private static boolean isPlatformPackage(Class<?> clazz) {
        if (clazz == null || clazz.getPackage() == null) {
            return false;
        }
        String name = clazz.getPackage().getName();
        return name.startsWith("android.") || name.startsWith("androidx.");
    }

    @Keep
    public static void log(String message) {
        FileLog.d(message);
    }

    @Keep
    public static void log(String message, Throwable throwable) {
        FileLog.e(message, throwable);
    }

    @Keep
    public static void log(Throwable throwable) {
        FileLog.e(throwable);
    }

    @Keep
    public static Object getPrivateField(Object object, String name)
            throws NoSuchFieldException, SecurityException {
        if (object == null) {
            return null;
        }
        try {
            Field field = findField(object.getClass(), name);
            if (field != null) {
                field.setAccessible(true);
                return field.get(object);
            }
        } catch (Exception e) {
            FileLog.e(object.getClass().getName(), e);
        }
        return null;
    }

    @Keep
    public static Object getPrivateStaticField(Class<?> clazz, String name)
            throws NoSuchFieldException, SecurityException {
        if (clazz == null) {
            return null;
        }
        try {
            Field field = findField(clazz, name);
            if (field != null) {
                field.setAccessible(true);
                return field.get(null);
            }
        } catch (Exception e) {
            FileLog.e(clazz.getName(), e);
        }
        return null;
    }

    @Keep
    public static void setPrivateField(Object object, String name, Object value)
            throws IllegalAccessException, NoSuchFieldException, SecurityException,
            IllegalArgumentException {
        if (object == null) {
            return;
        }
        try {
            Field field = findField(object.getClass(), name);
            if (field != null) {
                field.setAccessible(true);
                field.set(object, value);
            }
        } catch (Exception e) {
            FileLog.e(object.getClass().getName(), e);
        }
    }

    @Keep
    public static void setPrivateStaticField(Class<?> clazz, String name, Object value)
            throws IllegalAccessException, NoSuchFieldException, SecurityException,
            IllegalArgumentException {
        if (clazz == null) {
            return;
        }
        try {
            Field field = findField(clazz, name);
            if (field != null) {
                field.setAccessible(true);
                field.set(null, value);
            }
        } catch (Exception e) {
            FileLog.e(clazz.getName(), e);
        }
    }

    @Keep
    public static String stackTraceToString(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        java.io.StringWriter writer = new java.io.StringWriter();
        throwable.printStackTrace(new java.io.PrintWriter(writer));
        return writer.toString();
    }

    private static Field findField(Class<?> clazz, String name) {
        for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }
}

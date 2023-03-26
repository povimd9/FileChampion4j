package com.blumo.FileSentry4J;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class CustomFileLoader {
    public static void main(String[] args) {
        try {
            // Get the jar path, class name, and file path from command-line arguments
            String jarPath = args[0];
            String className = args[1];
            String methodName = args[2];
            String filePath = args[3];

            // Load the class using the custom class loader
            URL[] urls = new URL[] { new File(jarPath).toURI().toURL() };

            MyClassLoader loader = new MyClassLoader(urls);
            Class<?> clazz = loader.loadClass(className);

            // Instantiate the class
            Constructor<?> constructor = clazz.getConstructor();
            Object instance = constructor.newInstance();

            // Call a method on the class
            Method method = clazz.getMethod(methodName, File.class);
            File file = new File(filePath);
            method.invoke(instance, file);
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Define a custom class loader
    static class MyClassLoader extends URLClassLoader {
        public MyClassLoader(URL[] urls) {
            super(urls, null);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            // Load the specified class with this class loader
            Class<?> c = findClass(name);
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }
}
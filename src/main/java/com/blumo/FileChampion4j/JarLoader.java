package com.blumo.FileChampion4j;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class JarLoader {

    public static void main(String[] args) throws Exception {
        URL jarUrl = new URL("file:/path/to/your/jarfile.jar");
        URLClassLoader classLoader = new URLClassLoader(new URL[] { jarUrl });

        Class<?> clazz = classLoader.loadClass("ClamAVScanner");

        Method mainMethod = clazz.getDeclaredMethod("main", String[].class);
        mainMethod.invoke(null, (Object) args);

        classLoader.close();
    }
}

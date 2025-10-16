package org.acme.evolv.utils;

import jakarta.ws.rs.Path;
import java.lang.reflect.Method;
import java.util.Optional;

public final class PathUtils {
  private PathUtils() {}

  public static String getFullPath(Class<?> resourceClass, String methodName, Class<?>... paramTypes) {
    try {
      String classPath = Optional.ofNullable(resourceClass.getAnnotation(Path.class))
          .map(Path::value).orElse("");

      Method m = (paramTypes != null && paramTypes.length > 0)
          ? resourceClass.getDeclaredMethod(methodName, paramTypes)
          : resourceClass.getDeclaredMethod(methodName);

      String methodPath = Optional.ofNullable(m.getAnnotation(Path.class))
          .map(Path::value).orElse("");

      return ("/" + classPath + "/" + methodPath).replaceAll("//+", "/");
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }
}


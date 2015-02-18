package org.javenstudio.raptor.io.retry;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.util.StringUtils;


class RetryInvocationHandler implements InvocationHandler {
  public static final Logger LOG = Logger.getLogger(RetryInvocationHandler.class);
  private Object implementation;
  
  private RetryPolicy defaultPolicy;
  private Map<String,RetryPolicy> methodNameToPolicyMap;
  
  public RetryInvocationHandler(Object implementation, RetryPolicy retryPolicy) {
    this.implementation = implementation;
    this.defaultPolicy = retryPolicy;
    this.methodNameToPolicyMap = Collections.emptyMap();
  }
  
  public RetryInvocationHandler(Object implementation, Map<String, RetryPolicy> methodNameToPolicyMap) {
    this.implementation = implementation;
    this.defaultPolicy = RetryPolicies.TRY_ONCE_THEN_FAIL;
    this.methodNameToPolicyMap = methodNameToPolicyMap;
  }

  public Object invoke(Object proxy, Method method, Object[] args)
    throws Throwable {
    RetryPolicy policy = methodNameToPolicyMap.get(method.getName());
    if (policy == null) {
      policy = defaultPolicy;
    }
    
    int retries = 0;
    while (true) {
      try {
        return invokeMethod(method, args);
      } catch (Exception e) {
        if (!policy.shouldRetry(e, retries++)) {
          LOG.info("Exception while invoking " + method.getName()
                   + " of " + implementation.getClass() + ". Not retrying."
                   + StringUtils.stringifyException(e));
          if (!method.getReturnType().equals(Void.TYPE)) {
            throw e; // non-void methods can't fail without an exception
          }
          return null;
        }
        LOG.debug("Exception while invoking " + method.getName()
                 + " of " + implementation.getClass() + ". Retrying."
                 + StringUtils.stringifyException(e));
      }
    }
  }

  private Object invokeMethod(Method method, Object[] args) throws Throwable {
    try {
      if (!method.isAccessible()) {
        method.setAccessible(true);
      }
      return method.invoke(implementation, args);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

}


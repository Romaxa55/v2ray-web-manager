package com.jhl.framework.proxy.exception;

/**
 * Указание на необходимость сборки мусора (освобождения памяти).
 *
 */
public class ReleaseDirectMemoryException extends  Exception {
private static  String str ="Это исключение игнорируется, указывается необходимость освобождения прямой памяти.";
    public ReleaseDirectMemoryException() {
        this(str);
    }

    public ReleaseDirectMemoryException(String message) {
        super(str+message);
    }

    public ReleaseDirectMemoryException(String message, Throwable cause) {
        super(str+message, cause);
    }

    public ReleaseDirectMemoryException(Throwable cause) {
        this(str,cause);
    }

    public ReleaseDirectMemoryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(str+message, cause, enableSuppression, writableStackTrace);
    }
}

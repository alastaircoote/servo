/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package org.servo.servoview;

/**
 * Opaque wrapper around a Rust-heap-allocated JSValue.
 * Must be released via {@link #release()} when no longer needed.
 */
public class JSValue implements AutoCloseable {
    private long nativePtr;

    JSValue(long nativePtr) {
        this.nativePtr = nativePtr;
    }

    @Override
    public void close() {
        release();
    }

    public void release() {
        if (nativePtr != 0) {
            nativeRelease(nativePtr);
            nativePtr = 0;
        }
    }

    long getNativePtr() {
        return nativePtr;
    }

    private static native void nativeRelease(long ptr);
}

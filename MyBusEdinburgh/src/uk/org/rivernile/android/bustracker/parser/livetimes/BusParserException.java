/*
 * Copyright (C) 2011 - 2012 Niall 'Rivernile' Scott
 *
 * This software is provided 'as-is', without any express or implied
 * warranty.  In no event will the authors or contributors be held liable for
 * any damages arising from the use of this software.
 *
 * The aforementioned copyright holder(s) hereby grant you a
 * non-transferrable right to use this software for any purpose (including
 * commercial applications), and to modify it and redistribute it, subject to
 * the following conditions:
 *
 *  1. This notice may not be removed or altered from any file it appears in.
 *
 *  2. Any modifications made to this software, except those defined in
 *     clause 3 of this agreement, must be released under this license, and
 *     the source code of any modifications must be made available on a
 *     publically accessible (and locateable) website, or sent to the
 *     original author of this software.
 *
 *  3. Software modifications that do not alter the functionality of the
 *     software but are simply adaptations to a specific environment are
 *     exempt from clause 2.
 */

package uk.org.rivernile.android.bustracker.parser.livetimes;

/**
 * This exception is thrown when there is another known exception caught by the
 * parser. This exception makes it easier to pass exceptions down as the
 * exception thrown depends entirely on the implementation, which BusTimes is
 * not aware about.
 * 
 * @author Niall Scott
 */
public class BusParserException extends Exception {
    
    private final byte code;
    
    /**
     * Create a new BusParserException instance with the default message.
     */
    public BusParserException() {
        super();
        code = -1;
    }
    
    /**
     * Create a new BusParserException with the supplied message.
     * 
     * @param message The message to set.
     */
    public BusParserException(final String message) {
        super(message);
        code = -1;
    }
    
    /**
     * Create a new BusParserException with the given code.
     * 
     * @param code The code related to this exception.
     */
    public BusParserException(final byte code) {
        super();
        this.code = code;
    }
    
    /**
     * Create a new BusParserException with the given message and code.
     * 
     * @param message The message related to this exception.
     * @param code The code related to this exception.
     */
    public BusParserException(final String message, final byte code) {
        super(message);
        this.code = code;
    }
    
    /**
     * Get the code of this exception.
     * 
     * @return The code of this exception.
     */
    public byte getCode() {
        return code;
    }
}
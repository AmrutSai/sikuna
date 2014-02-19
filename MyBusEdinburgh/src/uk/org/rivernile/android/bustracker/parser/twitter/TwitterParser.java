/*
 * Copyright (C) 2014 Niall 'Rivernile' Scott
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

package uk.org.rivernile.android.bustracker.parser.twitter;

import java.util.List;
import uk.org.rivernile.android.fetchers.Fetcher;

/**
 * The TwitterParser is an interface that the concrete parser implements which
 * deals with the fetching and parsing of Twitter data.
 * 
 * @author Niall Scott
 */
public interface TwitterParser {
    
    /**
     * Get the list of ordered Tweets from the endpoint.
     * 
     * @param fetcher The Fetcher to use to get data.
     * @return A List of Tweets. Will be empty if there are no Tweets.
     * @throws TwitterException If there was a problem during the fetch or
     * parsing process.
     */
    public List<Tweet> getTweets(Fetcher fetcher) throws TwitterException;
}
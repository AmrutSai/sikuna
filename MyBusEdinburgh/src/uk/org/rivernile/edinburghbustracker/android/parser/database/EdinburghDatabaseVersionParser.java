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

package uk.org.rivernile.edinburghbustracker.android.parser.database;

import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;
import uk.org.rivernile.android.bustracker.parser.database
        .DatabaseEndpointException;
import uk.org.rivernile.android.bustracker.parser.database.DatabaseVersion;
import uk.org.rivernile.android.bustracker.parser.database
        .DatabaseVersionParser;
import uk.org.rivernile.android.fetchers.Fetcher;
import uk.org.rivernile.android.fetchers.readers.JSONFetcherStreamReader;

/**
 * This parser gets version data from the bus stop database server.
 * 
 * @author Niall Scott
 */
public class EdinburghDatabaseVersionParser implements DatabaseVersionParser {

    /**
     * {@inheritDoc}
     */
    @Override
    public DatabaseVersion getDatabaseVersion(final Fetcher fetcher)
            throws DatabaseEndpointException {
        if (fetcher == null) {
            throw new IllegalArgumentException("The fetcher must not be null.");
        }
        
        final JSONFetcherStreamReader reader = new JSONFetcherStreamReader();
        
        try {
            fetcher.executeFetcher(reader);
            
            final JSONObject jo = reader.getJSONObject();
            return new DatabaseVersion(
                    jo.getString("db_schema_version"),
                    jo.getString("topo_id"),
                    jo.getString("db_url"),
                    jo.getString("checksum"));
        } catch (IOException e) {
            throw new DatabaseEndpointException(e.getMessage());
        } catch (JSONException e) {
            throw new DatabaseEndpointException(e.getMessage());
        }
    }
}
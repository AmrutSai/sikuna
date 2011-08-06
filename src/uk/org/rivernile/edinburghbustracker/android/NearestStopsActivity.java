/*
 * Copyright (C) 2011 Niall 'Rivernile' Scott
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

package uk.org.rivernile.edinburghbustracker.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import com.google.android.maps.GeoPoint;
import java.util.Comparator;
import java.util.List;

public class NearestStopsActivity extends ListActivity
        implements LocationListener, Filterable {

    private static final int REQUEST_PERIOD = 10000;
    private static final float MIN_DISTANCE = 3.0f;

    private static final int LATITUDE_SPAN = 4499;
    private static final int LONGITUDE_SPAN = 8001;

    private static final int DIALOG_TURNONGPS = 1;
    private static final int DIALOG_CONFIRM_DELETE = 2;
    private static final int DIALOG_FILTER = 3;
    
    private static final int MENU_FILTER = Menu.FIRST;

    private static final int CONTEXT_MENU_VIEW = ContextMenu.FIRST;
    private static final int CONTEXT_MENU_SAVE = ContextMenu.FIRST + 1;
    private static final int CONTEXT_MENU_MAP = ContextMenu.FIRST + 2;

    private LocationManager locMan;
    private Location loc;
    private SearchResult currSelected;
    private ServiceFilter serviceFilter;
    private SharedPreferences sp;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.neareststops_title);
        setContentView(R.layout.neareststops);
        registerForContextMenu(getListView());
        sp = getSharedPreferences(PreferencesActivity.PREF_FILE, 0);
        
        locMan = (LocationManager)getSystemService(LOCATION_SERVICE);
        
        // See http://android-developers.blogspot.com/2011/06/deep-dive-into-location.html
        List<String> matchingProviders = locMan.getAllProviders();
        float accuracy, bestAccuracy = Float.MAX_VALUE;
        long time, bestTime = Long.MIN_VALUE;
        for(String provider : matchingProviders) {
            Location location = locMan.getLastKnownLocation(provider);
            if(location != null) {
                accuracy = location.getAccuracy();
                time = location.getTime();
        
                if(time > REQUEST_PERIOD && accuracy < bestAccuracy) {
                    loc = location;
                    bestAccuracy = accuracy;
                    bestTime = time;
                }
                else if(time < REQUEST_PERIOD && 
                    bestAccuracy == Float.MAX_VALUE && time > bestTime) {
                    loc = location;
                    bestTime = time;
                }
            }
        }
        
        if(!locMan.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && savedInstanceState == null &&
                !sp.getBoolean("neareststops_gps_prompt_disable", false)) {
            showDialog(DIALOG_TURNONGPS);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        locMan.requestLocationUpdates(getBestProvider(), REQUEST_PERIOD,
                MIN_DISTANCE, this);
        
        serviceFilter = ServiceFilter.getInstance(this);
        serviceFilter.setCallback(this);
        
        doUpdate();
    }

    @Override
    public void onPause() {
        super.onPause();

        locMan.removeUpdates(this);
    }
    
    @Override
    protected Dialog onCreateDialog(final int id) {
        switch(id) {
            case DIALOG_TURNONGPS:
                LayoutInflater vi = (LayoutInflater)getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
                View v = vi.inflate(R.layout.neareststops_gpsdialog, null);
                CheckBox cb = (CheckBox)v.findViewById(R.id.chkTurnongps);
                cb.setOnCheckedChangeListener(new CompoundButton
                        .OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(final CompoundButton v,
                            boolean isChecked) {
                        SharedPreferences.Editor edit = sp.edit();
                        edit.putBoolean("neareststops_gps_prompt_disable",
                                isChecked);
                        edit.commit();
                    }
                });
                
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCancelable(true)
                        .setTitle(R.string.neareststops_turnongps_title)
                        .setView(v)
                        .setInverseBackgroundForced(true)
                        .setPositiveButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog,
                            final int id) {
                        startActivity(new Intent(
                                Settings.ACTION_SECURITY_SETTINGS));
                    }
                }).setNegativeButton(R.string.no,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog,
                            final int id) {
                        dialog.dismiss();
                    }
                });
                return builder.create();
            case DIALOG_CONFIRM_DELETE:
                AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
                builder2.setCancelable(true)
                    .setTitle(R.string.favouritestops_dialog_confirm_title)
                    .setPositiveButton(R.string.okay,
                    new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog,
                            final int id)
                    {
                        SettingsDatabase.getInstance(getApplicationContext())
                                .deleteFavouriteStop(currSelected.stopCode);
                    }
                }).setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                     public void onClick(final DialogInterface dialog,
                             final int id)
                     {
                        dialog.dismiss();
                     }
                });
                return builder2.create();
            case DIALOG_FILTER:
                if(serviceFilter == null) {
                    serviceFilter = ServiceFilter.getInstance(this);
                    serviceFilter.setCallback(this);
                }
                return serviceFilter.getFilterDialog();
            default:
                return null;
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        menu.add(0, MENU_FILTER, 1, R.string.neareststops_menu_filter)
                .setIcon(R.drawable.ic_menu_filter);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch(item.getItemId()) {
            case MENU_FILTER:
                showDialog(DIALOG_FILTER);
                break;
            default:
                break;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick(final ListView l, final View v,
            final int position, final long id)
    {
        SearchResultsArrayAdapter ad = (SearchResultsArrayAdapter)this
                .getListAdapter();
        
        if(ad != null && ad.getCount() > position) {
            Intent intent = new Intent(this, DisplayStopDataActivity.class);
            intent.setAction(DisplayStopDataActivity.ACTION_VIEW_STOP_DATA);
            intent.putExtra("stopCode", ad.getItem(position).stopCode);
            startActivity(intent);
        }
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v,
            final ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
        SearchResultsArrayAdapter ad = (SearchResultsArrayAdapter)this
                .getListAdapter();
        if(ad == null) return;
        currSelected = ad.getItem(info.position);
        menu.setHeaderTitle(currSelected.stopName + " (" +
                currSelected.stopCode + ")");
        menu.add(0, CONTEXT_MENU_VIEW, 1, R.string.favouritestops_menu_view);
        
        if(SettingsDatabase.getInstance(this)
                .getFavouriteStopExists(currSelected.stopCode)) {
            menu.add(0, CONTEXT_MENU_SAVE, 2, R.string
                    .neareststops_context_remasfav);
        } else {
            menu.add(0, CONTEXT_MENU_SAVE, 2, R.string
                    .neareststops_context_addasfav);
        }
        menu.add(0, CONTEXT_MENU_MAP, 3, R.string
                .neareststops_context_showonmap);
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        AdapterContextMenuInfo info =
                (AdapterContextMenuInfo)item.getMenuInfo();
        if(currSelected == null) return super.onContextItemSelected(item);
        Intent intent;
        switch (item.getItemId()) {
            case CONTEXT_MENU_VIEW:
                intent = new Intent(this, DisplayStopDataActivity.class);
                intent.setAction(DisplayStopDataActivity.ACTION_VIEW_STOP_DATA);
                intent.putExtra("stopCode", currSelected.stopCode);
                startActivity(intent);
                return true;
            case CONTEXT_MENU_SAVE:
                if(SettingsDatabase.getInstance(this)
                        .getFavouriteStopExists(currSelected.stopCode)) {
                    showDialog(DIALOG_CONFIRM_DELETE);
                } else {
                    intent = new Intent(this,
                            AddEditFavouriteStopActivity.class);
                    intent.setAction(AddEditFavouriteStopActivity
                            .ACTION_ADD_EDIT_FAVOURITE_STOP);
                    intent.putExtra("stopCode", currSelected.stopCode);
                    intent.putExtra("stopName", currSelected.stopName);
                    startActivity(intent);
                }
                return true;
            case CONTEXT_MENU_MAP:
                intent = new Intent(this, BusStopMapActivity.class);
                intent.putExtra("stopCode", currSelected.stopCode);
                intent.putExtra("zoom", 19);
                startActivity(intent);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void doUpdate() {
        if(loc == null) return;

        GeoPoint currLoc = new GeoPoint((int)(loc.getLatitude() * 1E6),
                (int)(loc.getLongitude() * 1E6));
        int minX = currLoc.getLatitudeE6() + LATITUDE_SPAN;
        int minY = currLoc.getLongitudeE6() - LONGITUDE_SPAN;
        int maxX = currLoc.getLatitudeE6() - LATITUDE_SPAN;
        int maxY = currLoc.getLongitudeE6() + LONGITUDE_SPAN;
        BusStopDatabase bsd = BusStopDatabase.getInstance(this);
        if(serviceFilter == null) {
            serviceFilter = ServiceFilter.getInstance(this);
            serviceFilter.setCallback(this);
        }
        Cursor c;
        if(serviceFilter.isFiltered()) {
            c = bsd.getFilteredStopsByCoords(minX, minY, maxX, maxY,
                    serviceFilter.toString());
        } else {
            c = bsd.getBusStopsByCoords(minX, minY, maxX, maxY);
        }

        SearchResultsArrayAdapter items = new SearchResultsArrayAdapter();
        GeoPoint stopPoint;
        if(c.getCount() > 0) {
            while(!c.isLast()) {
                c.moveToNext();

                stopPoint = new GeoPoint(c.getInt(2), c.getInt(3));
                double distance = MapSearchHelper.calculateGeographicalDistance(
                        currLoc, stopPoint) * 1000;
                String stopCode = c.getString(0);
                String stopName = c.getString(1);
                String services = BusStopDatabase.getInstance(this)
                        .getBusServicesForStopAsString(stopCode);
                items.add(new SearchResult(stopCode, stopName, services,
                        distance, stopPoint));
            }
        }
        c.close();

        items.sort(mDistanceCompare);
        setListAdapter(items);
    }

    @Override
    public void onLocationChanged(final Location location) {
        loc = location;
        doUpdate();
    }

    @Override
    public void onProviderEnabled(final String provider) {
        locMan.removeUpdates(this);
        locMan.requestLocationUpdates(getBestProvider(), REQUEST_PERIOD,
                MIN_DISTANCE, this);
    }

    @Override
    public void onProviderDisabled(final String provider) {
        locMan.removeUpdates(this);
        locMan.requestLocationUpdates(getBestProvider(), REQUEST_PERIOD,
                MIN_DISTANCE, this);
    }

    @Override
    public void onStatusChanged(final String provider, final int status,
            final Bundle b) { }

    public String getBestProvider() {
        if(locMan.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        } else {
            return LocationManager.NETWORK_PROVIDER;
        }
    }
    
    @Override
    public void onFilterSet() {
        doUpdate();
    }

    private Comparator<SearchResult> mDistanceCompare =
            new Comparator<SearchResult>() {
        @Override
        public int compare(final SearchResult a, final SearchResult b) {
            // Because the type of distance is a double and the granularity
            // does matter to us, we cannot just do the trick of
            // return a.distance - b.distance;
            // We return 0 if they are equal, positive if
            // a.distance > b.distance and negative if a.distance < b.distance
            if(a.distance == b.distance) return 0;

            return (a.distance > b.distance) ? 1 : -1;
        }
    };

    private class SearchResult {

        public String stopCode;
        public String stopName;
        public String services;
        public double distance;
        public GeoPoint point;

        public SearchResult(final String stopCode, final String stopName,
                final String services, final double distance,
                final GeoPoint point) {
            this.stopCode = stopCode;
            this.stopName = stopName;
            this.services = services;
            this.distance = distance;
            this.point = point;
        }
    }

    public class SearchResultsArrayAdapter extends ArrayAdapter<SearchResult> {

        private LayoutInflater vi;

        public SearchResultsArrayAdapter() {
            super(NearestStopsActivity.this, R.layout.neareststops_list_item,
                    android.R.id.text1);

            vi = (LayoutInflater)NearestStopsActivity.this.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(final int position, final View convertView,
                final ViewGroup parent) {
            View row;
            if(convertView != null) {
                row = convertView;
            } else {
                row = vi.inflate(R.layout.neareststops_list_item, null);
            }

            TextView distance = (TextView)row.findViewById(
                    R.id.txtNearestDistance);
            TextView stopDetails = (TextView)row.findViewById(
                    android.R.id.text1);
            TextView buses = (TextView)row.findViewById(android.R.id.text2);
            SearchResult sr = getItem(position);
            distance.setText(getText(R.string.distance) + "\n" +
                    (int)sr.distance + "m");
            stopDetails.setText(sr.stopName + " (" + sr.stopCode + ")");

            buses.setText(sr.services);

            return row;
        }
    }
}
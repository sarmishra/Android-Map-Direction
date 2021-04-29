package com.example.mapdirection;

import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private MarkerOptions mMarkerOptions;
    private LatLng mOrigin;
    private LatLng mDestination;
    private Polyline mPolyline;


    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        // Obtain the SupportMapFragment and get notified when
        //   the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager( ).findFragmentById( R.id.map );
        mapFragment.getMapAsync( this );
    }  // End of onCreate


    @Override
    public void onMapReady( GoogleMap googleMap ) {
        mMap = googleMap;
        getMyLocation( );
    }  // End of onMapReady


    @Override
    // This method is invoked for every call on requestPermissions.
    public void onRequestPermissionsResult( int requestCode,
                                            String[ ] permissions, int[ ] grantResults ) {
        if ( requestCode == 100 )
            if ( !verifyAllPermissions( grantResults ) )
                Toast.makeText( getApplicationContext( ), "No sufficient permissions",
                        Toast.LENGTH_LONG ).show( );
            else
                getMyLocation( );
        else
            super.onRequestPermissionsResult( requestCode, permissions, grantResults );
    }  // End of onRequestPermissionsResult


    private boolean verifyAllPermissions( int[ ] grantResults ) {
        for ( int result : grantResults )
            if ( result != PackageManager.PERMISSION_GRANTED )
                return false;
        return true;
    }  // End of verifyAllPermissions


    private void getMyLocation( ) {
        // Getting LocationManager object from System Service LOCATION_SERVICE
        mLocationManager = (LocationManager) getSystemService( LOCATION_SERVICE );

        mLocationListener = new LocationListener( ) {

            @Override
            public void onLocationChanged( Location location ) {
                mOrigin = new LatLng( location.getLatitude( ), location.getLongitude( ) );
                mMap.moveCamera( CameraUpdateFactory.newLatLngZoom( mOrigin, 15 ) );
                if ( mOrigin != null && mDestination != null )
                    drawRoute( );
            }

            @Override
            public void onStatusChanged( String provider, int status, Bundle extras ) {
                // TODO Auto-generated method stub
            }  // End of onStatusChanged

            @Override
            public void onProviderEnabled( String provider ) {
                // TODO Auto-generated method stub
            }  // End of onProviderEnabled

            @Override
            public void onProviderDisabled( String provider ) {
                // TODO Auto-generated method stub
            }  // End of onProviderDisabled

        };  // End of LocationListener


        // Check user permission.
        int currentApiVersion = Build.VERSION.SDK_INT;
        if ( currentApiVersion >= Build.VERSION_CODES.M ) {
            if ( checkSelfPermission( Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_DENIED ) {
                mMap.setMyLocationEnabled( true );
                mLocationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 10000, 0, mLocationListener );

                mMap.setOnMapLongClickListener( new GoogleMap.OnMapLongClickListener( ) {

                    @Override
                    public void onMapLongClick( LatLng latLng ) {
                        mDestination = latLng;
                        mMap.clear( );
                        mMarkerOptions = new MarkerOptions( ).position( mDestination ).title( "Destination" );
                        // Add new marker to the Google Map Android API V2.
                        mMap.addMarker( mMarkerOptions );
                        if ( mOrigin != null && mDestination != null )  drawRoute( );
                    }  // End of onMapLongClick

                } );  // End of setOnMapLongClickListener

            }  // End of 2nd if
            else
                requestPermissions( new String[ ]
                        { android.Manifest.permission.ACCESS_FINE_LOCATION }, 100 );

        }  // End of 1st if

    }  // End of getMyLocation


    private void drawRoute( ) {
        // Getting URL to the Google Directions API
        String url = getDirectionsUrl( mOrigin, mDestination );
        DownloadTask downloadTask = new DownloadTask( );
        // Start downloading json data from Google Directions API.
        downloadTask.execute( url );
    }  // End of drawRoute


    private String getDirectionsUrl( LatLng origin, LatLng dest ) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // API key
        String key = "key=" + getString( R.string.google_maps_key );
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + key;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
        return url;
    }  // End of getDirectionsUrl


    // A method to download json data from url
    private String downloadUrl( String strUrl ) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;

        try {
            URL url = new URL( strUrl );
            // Creating an http connection to communicate with url
            urlConnection = ( HttpURLConnection ) url.openConnection( );
            // Connecting to URL
            urlConnection.connect( );
            // Reading data from URL
            iStream = urlConnection.getInputStream( );
            BufferedReader br =
                    new BufferedReader( new InputStreamReader( iStream ) );
            StringBuffer sb = new StringBuffer( );
            String line = "";
            while( ( line = br.readLine( ) ) != null )  sb.append( line );
            data = sb.toString( );
            br.close( );
        }  // End of try
        catch( Exception e ) {
            Log.d( "Exception on download", e.toString( ) );
        }
        finally {
            iStream.close( );
            urlConnection.disconnect( );
        }
        return data;
    }  // End of downloadUrl


    // A class to download data from Google Directions URL
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground( String... url ) {
            // For storing data from web service
            String data = "";
            try {
                // Fetching the data from web service
                data = downloadUrl( url[0] );
                Log.d( "DownloadTask", "DownloadTask : " + data );
            }
            catch( Exception e ) {
                Log.d( "Background Task", e.toString( ) );
            }
            return data;
        }  // End of DownloadTask: doInBackground


        // Executes in UI thread, after the execution of doInBackground( )
        @Override
        protected void onPostExecute( String result ) {
            super.onPostExecute( result );
            ParserTask parserTask = new ParserTask( );
            // Invokes the thread for parsing the JSON data
            parserTask.execute( result );
        }  // End of DownloadTask: onPostExecute

    }  // End of DownloadTask


    // A class to parse the Google Directions in JSON format
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> > {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground( String... jsonData ) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jObject = new JSONObject( jsonData[0] );
                DirectionsJSONParser parser = new DirectionsJSONParser();
                // Starts parsing data.
                routes = parser.parse( jObject );
            }
            catch( Exception e ) {
                e.printStackTrace();
            }
            return routes;
        }  // End of ParserTask: doInBackground


        // Executes in UI thread, after the parsing process.
        @Override
        protected void onPostExecute( List<List<HashMap<String, String>>> result ) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for ( int i=0; i<result.size( ); i++ ) {
                points = new ArrayList<LatLng>( );
                lineOptions = new PolylineOptions( );
                // Fetching i-th route
                List<HashMap<String, String>> path = result.get( i );

                // Fetching all the points in i-th route
                for ( int j=0; j<path.size( ); j++ ) {
                    HashMap<String,String> point = path.get( j );
                    double lat = Double.parseDouble( point.get( "lat" ) );
                    double lng = Double.parseDouble( point.get( "lng" ) );
                    LatLng position = new LatLng( lat, lng );
                    points.add( position );
                }  // End of inner for

                // Adding all the points in the route to LineOptions
                lineOptions.addAll( points );
                lineOptions.width( 2 );
                lineOptions.color( Color.RED );
            }  // End of outer for

            // Drawing polyline in the Google Map for the i-th route
            if ( lineOptions != null ) {
                if ( mPolyline != null )  mPolyline.remove( );
                mPolyline = mMap.addPolyline( lineOptions );
            }
            else
                Toast.makeText( getApplicationContext( ), "No route is found",
                        Toast.LENGTH_LONG ).show( );

        }  // End of ParserTask: onPostExecute

    }  // End of ParserTask

}  // End of MainActivity
/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*MatchMaker.
 *
 * Power*MatchMaker is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*MatchMaker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.matchmaker.munge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class GoogleAddressLookup extends AbstractMungeStep {

    
    public static final String GOOGLE_MAPS_API_KEY = "GoogleMapsApiKey";
    public static final String GOOGLE_GEOCODER_URL = "GoogleGeocoderUrl";

    /**
     * The status code returned with the Google result.  Even if the lookup fails or the
     * API key is incorrect, this three-digit status code will still be set.  It will only
     * come out null if the URL for the geocoder service is incorrect (therefore there
     * could be no response from the Google Maps Geocoder).
     * <p>
     * The meaning of the status codes is available in <a href="http://www.google.com/apis/maps/documentation/reference.html#GGeoStatusCode"
     * >the Google Maps API documentation</a>.
     */
    private final MungeStepOutput<BigDecimal> statusCode;
    
    private final MungeStepOutput<String> country;
    private final MungeStepOutput<String> adminArea;
    private final MungeStepOutput<String> subAdminArea;
    private final MungeStepOutput<String> locality;
    private final MungeStepOutput<String> street;
    private final MungeStepOutput<String> postCode;
    private final MungeStepOutput<BigDecimal> latitude;
    private final MungeStepOutput<BigDecimal> longitude;
    
    /**
     * The accuracy constant for the location information.  Constant codes are
     * documented in <a href="http://www.google.com/apis/maps/documentation/reference.html#GGeoAddressAccuracy"
     * >the Google Maps API documentation</a>.
     */
    private final MungeStepOutput<BigDecimal> accuracy;
    
    public GoogleAddressLookup() {
        super("Google Maps Address Lookup",false);
        
        super.addInput(new InputDescriptor("Address", String.class));
        
        addChild(statusCode = new MungeStepOutput<BigDecimal>("Lookup Status", BigDecimal.class));
        addChild(country = new MungeStepOutput<String>("Country Code", String.class));
        addChild(adminArea = new MungeStepOutput<String>("Administrative Area", String.class));
        addChild(subAdminArea = new MungeStepOutput<String>("Sub-Administrative Area", String.class));
        addChild(locality = new MungeStepOutput<String>("Locality", String.class));
        addChild(street = new MungeStepOutput<String>("Street Address", String.class));
        addChild(postCode = new MungeStepOutput<String>("Postal Code", String.class));
        addChild(latitude = new MungeStepOutput<BigDecimal>("Latitude", BigDecimal.class));
        addChild(longitude = new MungeStepOutput<BigDecimal>("Longitude", BigDecimal.class));
        addChild(accuracy = new MungeStepOutput<BigDecimal>("Accuracy Code", BigDecimal.class));
        
        setParameter(GOOGLE_MAPS_API_KEY, "");
        setParameter(GOOGLE_GEOCODER_URL, "http://maps.google.com/maps/geo");
    }
    
    @Override
    public void doOpen(Logger logger) throws Exception {
        String key = getParameter(GOOGLE_MAPS_API_KEY);
        if (key == null || key.length() == 0) {
            throw new IllegalStateException("Can't open step: Google Maps API Key is required.");
        }
    }

    @Override
    public Boolean doCall() throws Exception {
        // Clear out all the output values in case the request fails!
        for (MungeStepOutput<?> output : getChildren()) {
            output.setData(null);
        }

        String key = getParameter(GOOGLE_MAPS_API_KEY);
        String url = getParameter(GOOGLE_GEOCODER_URL);
        String address = (String) getMSOInputs().get(0).getData();
        url += "?output=json&key="+key+"&q="+URLEncoder.encode(address, "utf-8");
        
        String responseText = readURL(url);
        logger.debug("Address Lookup Response for \""+address+"\": " + responseText);
        JSONObject response = new JSONObject(responseText);

        JSONObject status = response.getJSONObject("Status");
        int statusCode = status.getInt("code");
        this.statusCode.setData(BigDecimal.valueOf(statusCode));
        
        if (!response.has("Placemark")) {
            logger.error("Address lookup for " + address + " failed. Google error code was " + statusCode + ".");
            return Boolean.TRUE;
        }
        
        JSONArray placemarks = response.getJSONArray("Placemark");
        // TODO count the number of placemarks in the response and provide it as an output
        
        JSONObject placemark = placemarks.getJSONObject(0);
        JSONObject addressDetails = placemark.getJSONObject("AddressDetails");
        JSONObject country = addressDetails.getJSONObject("Country");
        this.country.setData(country.getString("CountryNameCode"));

        JSONObject adminArea = country.getJSONObject("AdministrativeArea");
        this.adminArea.setData(adminArea.getString("AdministrativeAreaName"));

        if (adminArea.has("SubAdministrativeArea")) {
            JSONObject subAdminArea = adminArea.getJSONObject("SubAdministrativeArea");
            this.subAdminArea.setData(subAdminArea.getString("SubAdministrativeAreaName"));

            if (subAdminArea.has("Locality")) {
                JSONObject locality = subAdminArea.getJSONObject("Locality");
                this.locality.setData(locality.getString("LocalityName"));

                JSONObject thoroughfare = locality.getJSONObject("Thoroughfare");
                this.street.setData(thoroughfare.getString("ThoroughfareName"));

                JSONObject postalCode = locality.getJSONObject("PostalCode");
                this.postCode.setData(postalCode.getString("PostalCodeNumber"));
            }
        }
        
        this.accuracy.setData(BigDecimal.valueOf(addressDetails.getInt("Accuracy")));
        
        JSONObject location = placemark.getJSONObject("Point");
        JSONArray coordinates = location.getJSONArray("coordinates");
        this.longitude.setData(BigDecimal.valueOf(coordinates.getDouble(0)));
        this.latitude.setData(BigDecimal.valueOf(coordinates.getDouble(1)));
        
        return Boolean.TRUE;
    }

    private String readURL(String url) throws IOException {
        HttpURLConnection dest = (HttpURLConnection) new URL(url).openConnection();
        dest.setDoOutput(false);
        dest.setDoInput(true);
        dest.setRequestMethod("GET");
        dest.connect();

        InputStream inStream = dest.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
        StringBuilder sb = new StringBuilder(1000);
        char[] cbuf = new char[1000];
        int size;
        while ((size = in.read(cbuf, 0, cbuf.length)) > 0) {
            sb.append(cbuf, 0, size);
        }
        in.close();
        dest.disconnect();
        return sb.toString();
    }
}

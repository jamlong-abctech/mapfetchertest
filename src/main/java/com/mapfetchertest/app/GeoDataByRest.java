package com.mapfetchertest.app;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 *
 */
public class GeoDataByRest {

    private static final Logger log = LoggerFactory.getLogger(GeoDataByRest.class);

    public static void main(String[] args) {


        Date start = new Date();
        log.info("Starting to get geo data from geodata service .....");

        RequestAddress addressObj = new RequestAddress();
        RequestAttribute attributeObj = new RequestAttribute();
        attributeObj.setAttributes(addressObj);
        RequestBody requestBodyObj = new RequestBody();
        List<RequestAttribute> attributeList = Arrays.asList(attributeObj);
        requestBodyObj.setRecords(attributeList);
        Gson gson = new Gson();
        String json = gson.toJson(requestBodyObj);


        String username = ""; //get from email contact
        String password = ""; //get from email contact

        String street = "Sundveien 44A";
        String postnumber = "3128";

        String geoCodeAddressServiceURL="http://services2.geodataonline" +
                ".no/arcgis/rest/services/Geosok/GeosokLokasjon2/GeocodeServer/geocodeAddresses";

        String geoMapServerServiceURL="http://services2.geodataonline.no/arcgis/rest/services/temp/GeomapAdmin/MapServer/";

        Client client = Client.create();
        //client.addFilter(new HTTPBasicAuthFilter(use response.getEntity(String.class);rname, password));

        String toke=getAccessToken(username,password);
        WebResource webResource = client.resource(geoCodeAddressServiceURL)
                .queryParam("addresses",
                json).queryParam("outSR","4326").queryParam("f","json").queryParam("token",toke);
        ClientResponse response = webResource.type("application/json").get(ClientResponse.class);
        String output = response.getEntity(String.class);


        webResource = client.resource(geoCodeAddressServiceURL)
                .queryParam("addresses",
                        json).queryParam("outSR","25833").queryParam("f","json").queryParam("token",toke);
        response = webResource.type("application/json").get(ClientResponse.class);
        String output2 = response.getEntity(String.class);

        //get fylke
        webResource = client.resource("http://services2.geodataonline.no/arcgis/rest/services/temp/GeomapAdmin/MapServer/9/query?geometryType=esriGeometryPoint&geometry=10.401499526220702,59.261582700096227&inSR=4326&f=json&returnGeometry=false");
        response = webResource.type("application/json").get(ClientResponse.class);
        String output3 = response.getEntity(String.class);

        //get kommue
        webResource = client.resource("http://services2.geodataonline" +
                ".no/arcgis/rest/services/temp/GeomapAdmin/MapServer/10/query?geometryType=esriGeometryPoint&geometry" +
                "=10.401499526220702,59.261582700096227&inSR=4326&f=json&returnGeometry=false");
        response = webResource.type("application/json").get(ClientResponse.class);
        String output4 = response.getEntity(String.class);

        //get bydel
        webResource = client.resource("http://services2.geodataonline.no/arcgis/rest/services/temp/GeomapAdmin/MapServer/15/query?geometryType=esriGeometryPoint&geometry=10.72740918042572,59.927389611681825&inSR=4326&f=json&returnGeometry=false");
        response = webResource.type("application/json").get(ClientResponse.class);
        String output5 = response.getEntity(String.class);



        //get polygon by cardinal
        webResource = client.resource("http://services2.geodataonline" +
                ".no/arcgis/rest/services/Geomap_UTM33_EUREF89/GeomapEiendom/MapServer/2/query?where=MATRIKKELKOMMUNE" +
                "=542%20AND%20GNR=91%20AND%20BNR=344&f=json&outSR=4326&token="+toke);
        response = webResource.type("application/json").get(ClientResponse.class);
        String output6 = response.getEntity(String.class);

        //get label points
        String geometry="[{\"rings\":[[[9.4287884761269307,60.956974409973732],[9.4283489157546008," +
                "60.956719213197708],[9.4280146301043395,60.956946883072298],[9.4280013192068566,60.957165631642674],[9.4287714828138345,60.957275948292825],[9.4287884761269307,60.956974409973732]]]}]";
        webResource = client.resource("http://services2.geodataonline" +
                ".no/arcgis/rest/services/Utilities/Geometry/GeometryServer/labelPoints?sr=4326&f=json").queryParam
                ("polygons",geometry);

        response = webResource.type("application/json").get(ClientResponse.class);
        String output7 = response.getEntity(String.class);


        Date stop = new Date();
        long diffSeconds = (stop.getTime() - start.getTime());
        log.info(output);
        log.info(output2);
        log.info(output3);
        log.info(output4);
        log.info(output5);
        log.info(output6);
        log.info(output7);
        log.info("Total used time: " +(float)  diffSeconds/1000 % 60 + " seconds.");



    }

    public static String getAccessToken(String user,String password) {
        Client client = Client.create();
        WebResource webResource = client.resource("http://services2.geodataonline.no/arcgis/tokens/generateToken");
        javax.ws.rs.core.MultivaluedMap<String, String> params =
                new com.sun.jersey.core.util.MultivaluedMapImpl();
        params.add("f", "json");
        params.add("username", user);
        params.add("password", password);
        params.add("client", "requestip");

        ClientResponse response = webResource.type("application/x-www-form-urlencoded").accept("application/json")
                .post(ClientResponse.class, params);

        if (response.getStatus() == 200) {
            String jsonToken = response.getEntity(String.class);
            Gson gson = new Gson();
            return gson.fromJson(jsonToken, Token.class).getToken();
        }
        return "";
    }
}





package com.mapfetchertest.app;
import com.esri.arcgisws.*;
import com.esri.arcgisws.runtime.exception.ArcGISWebServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

public class GeoDataBySoap
{
    private static final Logger log = LoggerFactory.getLogger(GeoDataBySoap.class);

    public static void main( String[] args )
    {

        Date start=new Date();
        log.info("Starting to get geo data from geodata service .....");
        String username = "";
        String password = "";

        String street="Sundveien 44 A";
        String postnumber="3128";

        String geocodeURL = "http://services2.geodataonline.no/arcgis/services/Geosok/GeosokLokasjon2/GeocodeServer";
        String geometryURL = "http://services2.geodataonline.no/arcgis/services/Utilities/Geometry/GeometryServer/";
        String adminUrl = "http://services2.geodataonline.no/arcgis/services/temp/GeomapAdmin/MapServer";

        try
        {
            password = URLEncoder.encode(password, "UTF-8");
            username = URLEncoder.encode(username, "UTF-8");
        }
        catch(UnsupportedEncodingException ex) {}

        /* Step 1.-----------------------------------------------------------------------------------------------------*/
        // Call geocodeService to get geocoordinate and post location by using address and post code from calling URI
        // http://services2.geodataonline.no/arcgis/services/Geosok/GeosokLokasjon2/GeocodeServer
        /*------------------------------------------------------------------------------------------------------------*/

        GeocodeServerBindingStub geocodeService = new GeocodeServerBindingStub(geocodeURL, username, password);
        PropertySetProperty[] address = new PropertySetProperty[2];
        //Set street
        PropertySetProperty streetProp = new PropertySetProperty();
        streetProp.setKey("Adresse");
        streetProp.setValue(street);
        address[0] = streetProp;
        //Set postnumber
        PropertySetProperty zoneProp = new PropertySetProperty();
        zoneProp.setKey("Postnummer");
        zoneProp.setValue(postnumber);
        address[1] = zoneProp;


        //Input geocode property set
        PropertySet geocodeProp = new PropertySet();
        geocodeProp.setPropertyArray(address);

        //Geocode address
        PointN geocodePoints = null;
        String postLocation="";
        try{
            PropertySet geoCodeAddressPropertySet = geocodeService.geocodeAddress(geocodeProp, null);
            for (PropertySetProperty result :geoCodeAddressPropertySet.getPropertyArray()) {
                if (result.getKey().equalsIgnoreCase("Shape")) {
                    geocodePoints = (PointN) result.getValue();

                }
                String matchAddress="";
                if (result.getKey().equalsIgnoreCase("Match_addr")) {
                    matchAddress = (String) result.getValue();
                    if (matchAddress!="") {
                        postLocation = matchAddress.substring(matchAddress.indexOf(",") + 1, matchAddress.length());
                        postLocation.replaceAll("(?>-?\\d+(?:[\\./]\\d+)?)", "").trim();
                    }
                    break;
                }
            }

        }catch(ArcGISWebServiceException e){
            e.printStackTrace();

        }catch(NullPointerException e){
            e.printStackTrace();
        }

        /* Step 2
        .-----------------------------------------------------------------------------------------------------*/
        // Call geometryService to projection geocoordinate in normal points format convert to lat/lon format  by
        // Calling URI
        // http://services2.geodataonline.no/arcgis/services/Utilities/Geometry/GeometryServer/
        /*------------------------------------------------------------------------------------------------------------*/

        GeometryServerBindingStub geometryService = new GeometryServerBindingStub(geometryURL, username, password);
        SpatialReference inputSpatialReference = geometryService.findSRByWKID("EPSG", 25833, -1, true, true);
        SpatialReference outputSpatialReference = new GeographicCoordinateSystem();
        outputSpatialReference.setWKID(4326);
        Geometry[] inputGeometry = new Geometry[] { geocodePoints};
        boolean transformForward = true;
        GeoTransformation transformation = null;

        EnvelopeN extent = null;
        Geometry[] outputGeometry = geometryService.project(inputSpatialReference, outputSpatialReference, transformForward, transformation, extent, inputGeometry);
        PointN projectedPoint = null;
        for (Geometry result : outputGeometry) {
            projectedPoint = (PointN) result;
        }

        String kommune = null;
        String fylke = null;



        /* Step 3
        .-----------------------------------------------------------------------------------------------------*/
        // Call mapserverService to find geo string () from lat/long coordinate by calling URI
        // http://services2.geodataonline.no/arcgis/services/temp/GeomapAdmin/MapServer
        /*------------------------------------------------------------------------------------------------------------*/

        MapServerBindingStub mapserverService = new MapServerBindingStub(adminUrl, username, password);
        SpatialFilter queryFilter = new SpatialFilter();
        queryFilter.setFilterGeometry(geocodePoints);
        queryFilter.setSpatialRel(EsriSpatialRelEnum.esriSpatialRelIntersects);
        String mapName = mapserverService.getDefaultMapName();

        // Get Fylke
        RecordSet resultsQueryFylke = mapserverService.queryFeatureData(mapserverService.getDefaultMapName(), 9, queryFilter);
        Field[] fieldsFylke = resultsQueryFylke.getFields().getFieldArray();
        int j = 0;
        Object[] recFylke = resultsQueryFylke.getRecords()[0].getValues();
        for(Field field : fieldsFylke)
        {
            if(field.getAliasName()!=null){
                if(field.getAliasName().equals("navn"))
                {
                    fylke = recFylke[j].toString();
                }
            }
            j = j + 1;
        }

        // Get Kommune
        RecordSet resultsQuery = mapserverService.queryFeatureData(mapserverService.getDefaultMapName(), 10,
                queryFilter);
        Field[] fields = resultsQuery.getFields().getFieldArray();
        int i = 0;
        Object[] rec = resultsQuery.getRecords()[0].getValues();
        for(Field field : fields)
        {
            if(field.getAliasName()!=null){
                if(field.getAliasName().equals("navn"))
                {
                    kommune = rec[i].toString();
                }
            }
            i = i + 1;
        }

        RecordSet resultsQueryBydel = mapserverService.queryFeatureData(mapName, 15, queryFilter);
        Record[] recBydel=resultsQueryBydel.getRecords() ;

        String bydel="";
        if(recBydel.length>0){
            Object[] recs = recBydel[0].getValues();
            bydel=recs[4].toString();
        }

        Date stop=new Date();


        long diffSeconds =(stop.getTime() - start.getTime())/ 1000 % 60;

        //Show results
        log.info("=====Input Address====");
        log.info("Adresse: " + street);
        log.info("Postnr.: " + postnumber);

        log.info("=====Output====");
        log.info("lat,lon,x,y: " + projectedPoint.getY() + "\t," + projectedPoint.getX()+"\t,"+ geocodePoints
                .getY() + "\t," + geocodePoints.getX());
        log.info("Post Address: "+postLocation);
        log.info("Kommune: " + kommune);
        log.info("Fylke: \t " + fylke);
        log.info("Bydeler:"+bydel);

        log.info("Geography String:"+"Norge/"+fylke+"/"+kommune+"/"+bydel);
        log.info("Total used time: "+ diffSeconds +" second.");

        log.info("Stop to get geo data from geodata service .....");


    }
}

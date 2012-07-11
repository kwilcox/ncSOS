/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.asascience.ncsos.cdmclasses;

import com.asascience.ncsos.getobs.SOSObservationOffering;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joda.time.DateTime;
import org.w3c.dom.Document;
import ucar.nc2.ft.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateFromString;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Station;

/**
 *
 * @author SCowan
 */
public class Section extends baseCDMClass implements iStationData {
    private final ArrayList<String> eventTimes;
    private final String[] variableNames;
    private SectionFeatureCollection sectionData;
    private ArrayList<SectionFeature> sectionList;
    
    public Section(String[] stationName, String[] eventTime, String[] variableNames) {
        startDate = null;
        endDate = null;
        this.variableNames = variableNames;
        this.reqStationNames = new ArrayList<String>();
        reqStationNames.addAll(Arrays.asList(stationName));
        this.eventTimes = new ArrayList<String>();
        eventTimes.addAll(Arrays.asList(eventTime));
    }
    
    public static Document getCapsResponse(FeatureCollection dataset, Document document, String featureOfInterestBase, String GMLName, List<String> observedPropertyList) {
        try {
            String trajectoryID = null;
            SectionFeatureCollection sectSet = (SectionFeatureCollection) dataset;
            for (sectSet.resetIteration();sectSet.hasNext();) {
                SectionFeature sFeature = sectSet.next();
                trajectoryID = sFeature.getName();
                for (sFeature.resetIteration();sFeature.hasNext();) {
                    ProfileFeature pFeature = sFeature.next();
                    pFeature.calcBounds();
                    
                    pFeature.getCalendarDateRange();
                }
                LatLonRect bbox = getBoundingBox(sFeature);
                CalendarDateRange sectionDateRange = getDateRange(sFeature);
                DateFormatter formatter = new DateFormatter();
                
                SOSObservationOffering newOffering = new SOSObservationOffering();

                newOffering.setObservationStationLowerCorner(Double.toString(bbox.getLowerLeftPoint().getLatitude()), Double.toString(bbox.getLowerLeftPoint().getLongitude()));
                newOffering.setObservationStationUpperCorner(Double.toString(bbox.getUpperRightPoint().getLatitude()), Double.toString(bbox.getUpperRightPoint().getLongitude()));

                //check the data
                if (sectionDateRange != null) {
                    newOffering.setObservationTimeBegin(formatter.toDateTimeStringISO(sectionDateRange.getStart().toDate()));
                    newOffering.setObservationTimeEnd(formatter.toDateTimeStringISO(sectionDateRange.getEnd().toDate()));
                } //find the dates out!
                else {
                    System.out.println("no dates yet");
                }


                newOffering.setObservationStationDescription(sFeature.getCollectionFeatureType().toString());
                if (trajectoryID != null) {
                    newOffering.setObservationStationID("Trajectory" + trajectoryID);
                    newOffering.setObservationProcedureLink(GMLName+("Trajectory" + trajectoryID));
                    newOffering.setObservationName(GMLName+(trajectoryID));
                    newOffering.setObservationFeatureOfInterest(featureOfInterestBase+("Trajectory" + trajectoryID));
                } else {
                    newOffering.setObservationFeatureOfInterest(featureOfInterestBase+(sFeature.getName()));
                    newOffering.setObservationStationID((sFeature.getName()));
                    newOffering.setObservationProcedureLink(GMLName+((sFeature.getName())));
                    newOffering.setObservationFeatureOfInterest(featureOfInterestBase+(sFeature.getName()));
                }
                newOffering.setObservationSrsName("EPSG:4326");  // TODO?  
                newOffering.setObservationObserveredList(observedPropertyList);
                document = CDMUtils.addObsOfferingToDoc(newOffering,document);
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        } finally {
            return document;
        }
    }
    
    public static LatLonRect getBoundingBox(SectionFeature section) {
        LatLonRect retval = null;
        double lLat, lLon, uLat, uLon;
        lLat = lLon = Double.MAX_VALUE;
        uLat = uLon = -1 * Double.MAX_VALUE;
        
        try {
            // get a list of lats and lons from section
            for (section.resetIteration();section.hasNext();) {
                ProfileFeature pFeature = section.next();
                
                // check the lat and lon and find the lowest/highest values
                double lat = pFeature.getLatLon().getLatitude();
                double lon = pFeature.getLatLon().getLongitude();
                if (lat > uLat)
                    uLat = lat;
                else if (lat < lLat)
                    lLat = lat;
                if (lon > uLon)
                    uLon = lon;
                else if (lon < lLon)
                    lLon = lon;
            }
            retval = new LatLonRect(new LatLonPointImpl(lLat, lLon), new LatLonPointImpl(uLat, uLon));
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        } finally {
            return retval;
        }
    }
    
    private static CalendarDateRange getDateRange(SectionFeature section) {
        CalendarDateRange retval = new CalendarDateRange(CalendarDate.of(0), 60);
        
        try {
            Date earliestDate = new Date();
            Date latestDate = new Date(0);

            for (section.resetIteration();section.hasNext();) {
                ProfileFeature profile = section.next();
                profile.calcBounds();
                // skip if we don't have any points
                if (profile.size() == 0)
                    continue;
                
                if (profile.getTime().after(latestDate))
                    latestDate = profile.getTime();
                if (profile.getTime().before(earliestDate))
                    earliestDate = profile.getTime();
            }
            CalendarDate cDate = CalendarDate.of(earliestDate);
            retval = new CalendarDateRange(cDate, (CalendarDate.of(latestDate).getDifferenceInMsecs(cDate) / 1000));
        } catch (Exception ex) {
            System.out.println("Error in getDateRange - " + ex.getLocalizedMessage());
        } finally {
            return retval;
        }
    }
    
    /************************
     * iStationData Methods *
     ************************/
    
    @Override
    public void setData(Object featureCollection) throws IOException {
        System.out.println("in Section.setData");
        this.sectionData = (SectionFeatureCollection) featureCollection;
        
        sectionList = new ArrayList<SectionFeature>();

        DateTime dtSearchStart = null;
        DateTime dtSearchEnd = null;

        //check first to see if the event times are not null
        if (eventTimes != null) {
            //turn event times in to dateTimes to compare
            if (eventTimes.size() >= 1) {
                dtSearchStart = new DateTime(df.getISODate(eventTimes.get(0)), chrono);
            }
            if (eventTimes.size() == 2) {

                dtSearchEnd = new DateTime(df.getISODate(eventTimes.get(1)), chrono);
            }

            //temp
            DateTime dtStart = new DateTime();
            DateTime dtEnd = new DateTime(0);
            //check
            DateTime dtStartt = null;
            DateTime dtEndt = null;
            
            upperLat = upperLon = Double.NEGATIVE_INFINITY;
            lowerLat = lowerLon = Double.POSITIVE_INFINITY;

            for (sectionData.resetIteration();sectionData.hasNext();) {
                SectionFeature sectFeature = sectionData.next();
                LatLonRect bbox = getBoundingBox(sectFeature);
                CalendarDateRange dateRange = getDateRange(sectFeature);
                
                String trajName = "trajectory" + sectFeature.getName();

                //scan through the stationname for a match of id
                for (Iterator<String> it = reqStationNames.iterator(); it.hasNext();) {
                    String stName = it.next();
                    System.out.println("comparing: " + stName + " to " + trajName);
                    if (stName.equalsIgnoreCase(trajName)) {
                        System.out.println("adding " + trajName + " to section list");
                        sectionList.add(sectFeature);
                
                        dtStartt = new DateTime(dateRange.getStart().toDate(), chrono);
                        dtEndt = new DateTime(dateRange.getEnd().toDate(), chrono);

                        if (dtStartt.isBefore(dtStart)) {
                            dtStart = dtStartt;
                        }
                        if (dtEndt.isAfter(dtEnd)) {
                            dtEnd = dtEndt;
                        }

                        if (bbox.getLatMax() > upperLat) {
                            upperLat = bbox.getLatMax();
                        }
                        if (bbox.getLatMin() < lowerLat) {
                            lowerLat = bbox.getLatMin();
                        }
                        //lon
                        if (bbox.getLonMax() > upperLon) {
                            upperLon = bbox.getLonMax();
                        }
                        if (bbox.getLonMax() < lowerLon) {
                            lowerLon = bbox.getLonMin();
                        }
                        
                        break;
                    }
                }
                setStartDate(df.toDateTimeStringISO(dtStart.toDate()));
                setEndDate(df.toDateTimeStringISO(dtEnd.toDate()));
                if (reqStationNames != null) {
                    setNumberOfStations(reqStationNames.size());
                }
            }
        }
    }

    public void setInitialLatLonBounaries(List<Station> tsStationList) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getDataResponse(int stNum) {
        try {
            if (sectionData != null) {
                return createSectionData(stNum);
            }
        } catch (Exception ex) {
//            Logger.getLogger(Trajectory.class.getName()).log(Level.SEVERE, null, ex);
            return DATA_RESPONSE_ERROR + Profile.class;
        }
        return DATA_RESPONSE_ERROR + Profile.class;
    }

    @Override
    public String getStationName(int idNum) {
        if (sectionList != null) {
            System.out.println("looking for staion number " + idNum);
            return "Trajectory" + sectionList.get(idNum).getName();
        } else {
            return Invalid_Station;
        }
    }

    @Override
    public double getLowerLat(int stNum) {
        if (sectionList != null) {
            return (getBoundingBox(sectionList.get(stNum)).getLatMin());
        } else {
            return Invalid_Value;
        }
    }

    @Override
    public double getLowerLon(int stNum) {
        if (sectionList != null) {
            return (getBoundingBox(sectionList.get(stNum)).getLonMin());
        } else {
            return Invalid_Value;
        }
    }

    @Override
    public double getUpperLat(int stNum) {
        if (sectionList != null) {
            return (getBoundingBox(sectionList.get(stNum)).getLatMax());
        } else {
            return Invalid_Value;
        }
    }

    @Override
    public double getUpperLon(int stNum) {
        if (sectionList != null) {
            return (getBoundingBox(sectionList.get(stNum)).getLonMax());
        } else {
            return Invalid_Value;
        }
    }

    @Override
    public String getTimeEnd(int stNum) {
        if (sectionList != null) {
            return df.toDateTimeStringISO(getDateRange(sectionList.get(stNum)).getEnd().toDate());
        } else {
            return ERROR_NULL_DATE;
        }
    }

    @Override
    public String getTimeBegin(int stNum) {
        if (sectionList != null) {
            return df.toDateTimeStringISO(getDateRange(sectionList.get(stNum)).getStart().toDate());
        } else {
            return ERROR_NULL_DATE;
        }
    }
    
    @Override
    public double getLowerAltitude(int stNum) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getUpperAltitude(int stNum) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getDescription(int stNum) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private String createSectionData(int stNum) {
        StringBuilder builder = new StringBuilder();
        SectionFeature sectionFeature = sectionList.get(stNum);
        addTrajectoryProfileData(builder, sectionFeature, stNum);
        return builder.toString();
    }

    private void addTrajectoryProfileData(StringBuilder builder, SectionFeature sectionFeature, int stNum) {
        try {
            List<String> valueList = new LinkedList<String>();
            PointFeatureCollectionIterator profileCollectionIter = sectionFeature.getPointFeatureCollectionIterator(-1);
            
            for (;profileCollectionIter.hasNext();) {
                PointFeatureIterator pointIter = profileCollectionIter.next().getPointFeatureIterator(-1);
                DateTime pointTime;
                DateTime dtStart;
                DateTime dtEnd;

                if (eventTimes != null && eventTimes.size() > 0) {
                    if (eventTimes.size() == 2) {
                        dtStart = new DateTime(df.getISODate(eventTimes.get(0)), chrono);
                        dtEnd = new DateTime(df.getISODate(eventTimes.get(1)), chrono);

                        for (;pointIter.hasNext();) {
                            PointFeature point = pointIter.next();
                            valueList.clear();
                            
                            pointTime = new DateTime(point.getObservationTimeAsCalendarDate().toDate());
                            
                            if (pointTime.isEqual(dtStart) || pointTime.isEqual(dtEnd) || (pointTime.isAfter(dtStart) && pointTime.isBefore(dtEnd))) {
                                addDataLine(valueList, point, builder);
                            }
                        }
                    } else {
                        dtStart = new DateTime(df.getISODate(eventTimes.get(0)), chrono);

                        for (;pointIter.hasNext();) {
                            PointFeature point = pointIter.next();
                            valueList.clear();
                            
                            pointTime = new DateTime(point.getObservationTimeAsCalendarDate().toDate());
                            
                            if (pointTime.isEqual(dtStart)) {
                                addDataLine(valueList, point, builder);
                            }
                        }
                    }
                } else {
                    for (;pointIter.hasNext();) {
                        PointFeature point = pointIter.next();
                        valueList.clear();
                        addDataLine(valueList, point, builder);
                    }
                }
                pointIter.finish();
            }
            profileCollectionIter.finish();
        } catch (Exception e) {
            System.out.println("Error in addTrajectoryProfileData " + e.getMessage());
        }
    }

    private void addDataLine(List<String> valueList, PointFeature point, StringBuilder builder) throws IOException {
        valueList.add("time=" + df.toDateTimeStringISO(point.getObservationTimeAsCalendarDate().toDate()));

        for (String variableName : variableNames) {
            valueList.add(variableName + "=" + point.getData().getScalarObject(variableName).toString());
        }
        
        for (String str : valueList) {
            builder.append(str).append(",");
        }
        builder.deleteCharAt(builder.length()-1).append(";");
    }

    
    
}

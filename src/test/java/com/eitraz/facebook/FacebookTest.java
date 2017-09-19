package com.eitraz.facebook;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.junit.Test;

import biweekly.ICalVersion;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.io.text.ICalWriter;
import biweekly.property.Summary;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.Version;
import com.restfb.json.JsonArray;
import com.restfb.json.JsonObject;
import com.restfb.util.DateUtils;

public class FacebookTest {
    private FacebookClient facebookClient;
    
    private VEvent createCalendarEvent(String id, String name, Date startTime, Date endTime) {
        VEvent calendarEvent = new VEvent();
        calendarEvent.setUid(id);

        Summary summary = calendarEvent.setSummary(name);
        summary.setLanguage("sv-SE");
        
        calendarEvent.setDateStart(startTime);
        calendarEvent.setDateEnd(endTime);
        
        return calendarEvent;
    }
    
    @Test
    public void aTest() {
        facebookClient = new DefaultFacebookClient("AccessToken", Version.VERSION_2_10);
        
        JsonObject page = facebookClient.fetchObject("FridhemsRidklubb", JsonObject.class, Parameter.with("fields", "events"));

        JsonObject eventsContainer = page.getJsonObject("events");
        JsonArray events = eventsContainer.getJsonArray("data");

        ICalendar calendar = new ICalendar();

        for (int i=0; i<events.length(); i++) {
            JsonObject event = events.getJsonObject(i);

            String name = event.getString("name");
            System.out.println("Name: " + name);

            // Multiple times
            if (event.has("event_times")) {
                JsonArray eventTimes = event.getJsonArray("event_times");

                for (int j=0; j<eventTimes.length(); j++) {
                    JsonObject eventTime = eventTimes.getJsonObject(j);

                    String id = eventTime.getString("id");
                    Date startTime = DateUtils.toDateFromLongFormat(eventTime.getString("start_time"));
                    Date endTime = DateUtils.toDateFromLongFormat(eventTime.getString("end_time"));
                    
                    calendar.addEvent(createCalendarEvent(id, name, startTime, endTime));
                }
            }
            // Single time
            else if (event.has("end_time")) {
                String id = event.getString("id");
                Date startTime = DateUtils.toDateFromLongFormat(event.getString("start_time"));
                Date endTime = DateUtils.toDateFromLongFormat(event.getString("end_time"));
                
                calendar.addEvent(createCalendarEvent(id, name, startTime, endTime));
            }
            // Full day
            else {
                String id = event.getString("id");
                Date startTime = DateUtils.toDateFromLongFormat(event.getString("start_time"));
                Date endTime = DateUtils.toDateFromLongFormat(event.getString("start_time"));

                calendar.addEvent(createCalendarEvent(id, name, startTime, endTime));
            }
        }
        
        File calendarFile = new File("calendar.ics");
        ICalWriter writer = null;

        try {
            writer = new ICalWriter(calendarFile, ICalVersion.V2_0);
            writer.write(calendar);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null)
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }
}

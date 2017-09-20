package com.eitraz.facebook;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;

@RestController
public class FacebookEventsToICalController {

    @Value("${facebook.accessToken}")
    private String accessToken;

    @GetMapping("facebook/{page}/events")
    public ResponseEntity<String> events(@PathVariable("page") String pageName) {
        FacebookClient facebookClient = new DefaultFacebookClient(accessToken, Version.VERSION_2_10);

        JsonObject page = facebookClient.fetchObject(pageName, JsonObject.class, Parameter.with("fields", "events"));

        JsonObject eventsContainer = page.getJsonObject("events");
        JsonArray events = eventsContainer.getJsonArray("data");

        ICalendar calendar = new ICalendar();

        for (int i = 0; i < events.length(); i++) {
            JsonObject event = events.getJsonObject(i);

            String name = event.getString("name");

            // Multiple times
            if (event.has("event_times")) {
                JsonArray eventTimes = event.getJsonArray("event_times");

                for (int j = 0; j < eventTimes.length(); j++) {
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

        StringWriter stringWriter = new StringWriter();
        try (ICalWriter writer = new ICalWriter(stringWriter, ICalVersion.V2_0)) {
            writer.write(calendar);
            writer.flush();

            return ResponseEntity.ok().body(stringWriter.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    private VEvent createCalendarEvent(String id, String name, Date startTime, Date endTime) {
        VEvent calendarEvent = new VEvent();
        calendarEvent.setUid(id);

        Summary summary = calendarEvent.setSummary(name);
        summary.setLanguage("sv-SE");

        calendarEvent.setDateStart(startTime);
        calendarEvent.setDateEnd(endTime);

        return calendarEvent;
    }
}

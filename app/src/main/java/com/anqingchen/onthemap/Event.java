package com.anqingchen.onthemap;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;

import org.bson.BsonDateTime;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonObjectId;
import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonWriter;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.ObjectId;

public class Event{

    public static final Codec<Event> simple_codec = new Codec<Event>() {
        @Override
        public Event decode(BsonReader reader, DecoderContext decoderContext) {
            final BsonDocument document = new BsonDocumentCodec().decode(reader, decoderContext);
            return fromBsonDocument(document, false);
        }

        @Override
        public void encode(BsonWriter writer, Event value, EncoderContext encoderContext) {
            new BsonDocumentCodec().encode(
                    writer, toBsonDocument(value), encoderContext);
        }

        @Override
        public Class<Event> getEncoderClass() {
            return Event.class;
        }
    };

    public static final Codec<Event> full_codec = new Codec<Event>() {
        @Override
        public Event decode(BsonReader reader, DecoderContext decoderContext) {
            final BsonDocument document = new BsonDocumentCodec().decode(reader, decoderContext);
            return fromBsonDocument(document, true);
        }

        @Override
        public void encode(BsonWriter writer, Event value, EncoderContext encoderContext) {
            new BsonDocumentCodec().encode(
                    writer, toBsonDocument(value), encoderContext);
        }

        @Override
        public Class<Event> getEncoderClass() {
            return Event.class;
        }
    };

    private ObjectId _id;
    private LatLng eventLatLng;
    private String eventName;
    private String eventDesc;
    private String eventOrg;
    private long eventStartDate, eventEndDate;      // Event Start/End times are stored in UTC Epoch time

    // Constructors
    public Event(double lat, double lang, String eventName, String eventDesc, String eventOrg, long eventStartDate, long eventEndDate) {
        this(new LatLng(lat, lang), eventName, eventDesc, eventOrg, eventStartDate, eventEndDate);
    }

    public Event(LatLng latLng, String eventName, String eventDesc, String eventOrg, long eventStartDate, long eventEndDate) {
        this.eventLatLng = latLng;
        this.eventName = eventName;
        this.eventDesc = eventDesc;
        this.eventOrg = eventOrg;
        this.eventStartDate = eventStartDate;
        this.eventEndDate = eventEndDate;
    }

    public Event(ObjectId id, double lat, double lang) {
        this._id = id;
        this.eventLatLng = new LatLng(lat, lang);
    }

    // Setters
    public void setEventLatLng(LatLng eventLatLng) {
        this.eventLatLng = eventLatLng;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public void setEventDesc(String eventDesc) {
        this.eventDesc = eventDesc;
    }

    public void setEventOrg(String eventOrg) { this.eventOrg = eventOrg; }

    public void setEventStartDate(long eventStartDate) {
        this.eventStartDate = eventStartDate;
    }

    public void setEventEndDate(long eventEndDate) {
        this.eventEndDate = eventEndDate;
    }

    // Getters
    public LatLng getEventLatLng() {
        return eventLatLng;
    }

    public String getEventName() {
        return eventName;
    }

    public String getEventDesc() {
        return eventDesc;
    }

    public String getEventOrg() { return eventOrg; }

    public long getEventStartDate() {
        return eventStartDate;
    }

    public long getEventEndDate() {
        return eventEndDate;
    }

    public ObjectId get_id() { return _id; }

    public SymbolOptions toSymbol() {
        String iconImage = "volunteer-marker";
        JsonParser parser = new JsonParser();
        JsonElement idElement = parser.parse(get_id().toHexString());
        return new SymbolOptions()
                .withLatLng(getEventLatLng())
                .withIconImage(iconImage)
                .withData(idElement);
    }

    private static Event fromBsonDocument(BsonDocument document, boolean full) {
        if (!full) return new Event(document.getObjectId("_id").getValue(), document.getDouble("lat").getValue(), document.getDouble("lng").getValue());
        return new Event(document.getDouble("lat").getValue(), document.getDouble("lng").getValue(),
                document.getString("name").getValue(), document.getString("desc").getValue(), document.getString("org").getValue(),
                document.getDateTime("start_time").getValue(), document.getDateTime("end_time").getValue());
    }

    private static BsonDocument toBsonDocument(Event event) {
        BsonDocument temp = new BsonDocument();
        temp.put("_id", new BsonObjectId());
        temp.put("name", new BsonString(event.getEventName()));
        temp.put("desc", new BsonString(event.getEventDesc()));
        temp.put("org", new BsonString(event.getEventOrg()));
        temp.put("lat", new BsonDouble(event.getEventLatLng().getLatitude()));
        temp.put("lng", new BsonDouble(event.getEventLatLng().getLongitude()));
        temp.put("start_time", new BsonDateTime(event.getEventStartDate()));
        temp.put("end_time", new BsonDateTime(event.getEventEndDate()));
        return temp;
    }
}

package com.w3engineers.highbandtest.protocol.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.w3engineers.highbandtest.protocol.data.gson.Exclude;
import com.w3engineers.highbandtest.protocol.data.gson.HiddenAnnotationExclusionStrategy;
import com.w3engineers.highbandtest.protocol.data.gson.RuntimeTypeAdapterFactory;

import java.lang.reflect.Modifier;

public class BaseMessage {

    @Exclude
    protected static Gson mGson;

    private String type = "type";

    public BaseMessage() {
        setMetaData();
        initGson();
    }

    private static void initGson() {

        if(mGson == null) {
            RuntimeTypeAdapterFactory<BaseMessage> typeAdapterFactory = RuntimeTypeAdapterFactory
                    .of(BaseMessage.class, "type")
                    .registerSubtype(HelloMessage.class, HelloMessage.class.getSimpleName())
                    .registerSubtype(Credential.class, Credential.class.getSimpleName());

            mGson = new GsonBuilder().setExclusionStrategies(new HiddenAnnotationExclusionStrategy())
                    .registerTypeAdapterFactory(typeAdapterFactory)
                    .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                    .create();
        }
    }

    private void setMetaData() {
        type = getClass().getSimpleName();
    }


    public final String toJson() {
        setMetaData();
        return mGson.toJson(this);
    }

    public static BaseMessage toBaseMessage(String data) {
        initGson();
        BaseMessage baseMessage = null;

        try {
            baseMessage = mGson.fromJson(data, BaseMessage.class);
        }catch (JsonParseException e) {
            e.printStackTrace();
        }

        return baseMessage;
    }
}

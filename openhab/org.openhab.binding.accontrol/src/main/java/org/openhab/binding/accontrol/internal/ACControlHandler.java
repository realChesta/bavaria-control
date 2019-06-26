/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.accontrol.internal;

import static org.openhab.binding.accontrol.internal.ACControlBindingConstants.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ACControlHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Chesta - Initial contribution
 */
@NonNullByDefault
public class ACControlHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(ACControlHandler.class);
    private final JsonParser jsonParser = new JsonParser();

    private @Nullable ACControlHandlerConfiguration config;
    private String hostname;
    private @Nullable InetAddress cachedDestination = null;

    private boolean ACPower = false;
    private int FanSpeed = 1;
    private float RoomTemperature = -132;
    private ScheduledFuture<?> pollingJob;

    public ACControlHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        logger.debug("got command [" + command.getClass().getName() + "] " + command.toString() 
            + " for chanel " + channelUID.getId());

        if (command instanceof RefreshType) {
            if (!this.refreshStatus()) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR);
            }

            switch (channelUID.getId()) {
                case CHANNEL_POWER:
                    logger.debug("got power refresh");
                    this.updateState(CHANNEL_POWER, OnOffType.from(this.ACPower));
                    break;
                case CHANNEL_FAN_SPEED:
                logger.debug("got fan speed refresh");
                    this.updateState(CHANNEL_FAN_SPEED, new DecimalType(this.FanSpeed));
                    break;
                case CHANNEL_TEMP:
                logger.debug("got temperature refresh");
                    this.updateState(CHANNEL_TEMP, new DecimalType(this.RoomTemperature));
                    break;
            }
        }
        else if (command instanceof OnOffType && channelUID.getId().equals(CHANNEL_POWER)) {
            logger.debug("trying to toggle power...");
            try {
                togglePower();
            }
            catch (Exception ex) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR);
            }
        }
        else if (command instanceof DecimalType && channelUID.getId().equals(CHANNEL_FAN_SPEED)) {
            try {
                setFanSpeed(((DecimalType)command).intValue());
            }
            catch (Exception ex) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR);
            }
        }
    }

    @Override
    public void initialize() {
        logger.debug("ACControl is initializing!");
        config = getConfigAs(ACControlHandlerConfiguration.class);

        this.hostname = config.hostname;

        updateStatus(ThingStatus.UNKNOWN);

        scheduler.execute(() -> {
            if (refreshStatus()) {
                updateStatus(ThingStatus.ONLINE);
            }
            else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR);
            }
        });

        // Update the temperature every 60 seconds
        pollingJob = scheduler.scheduleWithFixedDelay(()-> {
            if (refreshStatus()) {
                this.updateState(CHANNEL_TEMP, new DecimalType(this.RoomTemperature));
            }
            else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR);
            }
        }, 0, 60, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        pollingJob.cancel(true);
    }

    private boolean refreshStatus() {
        try {
            String jsonResponse = httpRequest("http://" + this.hostname + "/ac", "GET");
            JsonObject json = jsonParser.parse(jsonResponse).getAsJsonObject();
            this.ACPower = json.get("ac-status").getAsInt() == 1;
            this.FanSpeed = json.get("ac-fan-speed").getAsInt();
            this.RoomTemperature = json.get("room-temperature").getAsFloat();
            return true;
        }
        catch (Exception ex) {
            return false;
        }
    }

    private OnOffType togglePower() throws Exception {
        String resp = httpRequest("http://" + this.hostname + "/ac-toggle?noinit=true", "POST");
        this.ACPower = resp.equals("1");
        OnOffType result = OnOffType.from(resp);
        updateState(CHANNEL_POWER, result);
        return result;
    }

    private DecimalType setFanSpeed(int speed) throws Exception {
        do {
            toggleFanSpeed();
        }
        while (this.FanSpeed != speed);

        DecimalType result = new DecimalType(this.FanSpeed);
        updateState(CHANNEL_FAN_SPEED, result);
        return result;
    }

    private void toggleFanSpeed() throws Exception {
        String resp = httpRequest("http://" + this.hostname + "/ac-toggle-speed", "POST");
        this.FanSpeed = Integer.parseInt(resp);
    }

    public String httpRequest(String urlString, String method) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(method);

        int responseCode = con.getResponseCode();

        if (responseCode != 200) {
            throw new IOException();
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
        in.close();

        logger.debug("http: [" + urlString + " | " + method + " ] " + response.toString());
        
        return response.toString();
    }
}

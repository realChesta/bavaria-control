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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link ACControlBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Chesta - Initial contribution
 */
@NonNullByDefault
public class ACControlBindingConstants {

    private static final String BINDING_ID = "accontrol";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_AC_CONTROLLER = new ThingTypeUID(BINDING_ID, "controller");

    // List of all Channel ids
    public static final String CHANNEL_TEMP = "roomTemperature";
    public static final String CHANNEL_POWER = "powerState";
    public static final String CHANNEL_FAN_SPEED = "fanSpeed";

    public static final int DESTINATION_TTL = 300 * 1000; // in ms, 300 s
}

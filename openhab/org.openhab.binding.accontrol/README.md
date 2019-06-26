# ACControl Binding

This is a binding for a small AC controller device i made for myself.



## Thing Configuration

Enter the device's IP address (or if your network supports it `esp-ac-control`) when adding a new device.

## Channels

| channel  | type   | description                  |
|----------|--------|------------------------------|
| roomTemperature  | Number:Temperature | Provides the current room temperature. |
| powerState | Switch | Sets the AC power state. |
| fanSpeed | Number [1,2] | Sets the AC fan speed. |


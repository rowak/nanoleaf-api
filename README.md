# Nanoleaf Java Library
This is a synchronous *and* asynchronous Java library for the Nanoleaf RESTful API. The Aurora (Light Panels), Canvas, and Shapes devices are all supported, and all API features are (will be) supported.

## Documentation (not yet ready)

## Installation
### Maven (not yet ready)

### Manual (not yet ready)

## Connecting to a Device (not yet ready)

## Controlling a Device
Once you have created an instance of the device, you can start using its methods.

### State
Change the static state of the device and get basic information. Below are a few examples.

##### On/Off
```Java
boolean isOn = device.isOn();   // returns true if the device is on and false if it is off
device.setOn(true);             // sets the on state of the device
device.toggleOn();              // toggles the state of the device (on -> off, off -> on)
```

#### Brightness

#### Hue

#### Saturation

#### Color Temperature

### Effects

### Panel Layout

### Rhythm (Aurora only)

### External Streaming

## The Effect Class

### Effect Builders

#### Custom Effects

#### Static Effects

## Schedules (WIP)

## Exceptions

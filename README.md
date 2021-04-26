# Nanoleaf Java Library
This is an extensive synchronous and asynchronous Java library for the Nanoleaf RESTful API. The Aurora (Light Panels), Canvas, and Shapes devices are all supported, and all API features are (will be) supported.

This library is the successor to my old Aurora Java library. It has a significant number of improvements, including:
- Nanoleaf Shapes and Canvas devices are supported
- Asynchronous methods
- Helper classes for group devices
- Effects class redesigned for Effect v2
- Many broken features have been fixed

## Documentation (not yet ready)

## Installation
### Maven (not yet ready)

### Manual (not yet ready)

## Connecting to a Device
First, search for all of the existing Nanoleaf devices connected to your local network, then select one from the returned list. You can also do this asynchronously.

```Java
int timeout = 2000;
List<NanoleafDeviceMeta> devices = NanoleafSetup.findDevices(timeout);
```

Next create an access token to authenticate with the device. You must first physically hold down the power button on your device for 5-7 seconds until the LED starts flashing before running the following code. Make sure to write down your access token for future use, however you can create as many as you like.
```Java
String accessToken = NanoleafSetup.createAccessToken()
```

Finally, you can connect to a device using the following code. Select any device from the devices list to connect to.
```Java
NanoleafDeviceMeta meta = ...
NanoleafDevice device = NanoleafSetup.createDevice(meta, accessToken);
```

You can also create devices if you already know their type. For example:
```Java
String ip = ...
int port = ...   // usually 16021
String accessToken = ...
Aurora aurora = new Aurora(ip, port, accessToken);
```

## Controlling a Device
Once you have created an Nanoleaf device instance, you can start using its methods.

### State
Change the static state of the device and get basic information. Below are a few examples.

##### On/Off
```Java
boolean isOn = device.isOn();   // returns true if the device is on and false if it is off
device.setOn(true);             // sets the on state of the device
device.toggleOn();              // toggles the state of the device (on -> off, off -> on)
```

#### Brightness
```Java
int brightness = device.getBrightness();
device.setBrightness(50);         // returns the brightness of the device
device.increaseBrightness(25);    // increases the brightness by a percent amount
device.decreaseBrightness(75);    // decreases the brightness by a percent amount
device.fadeToBrightness(100, 5);  // smoothly fades to 100% brightness over a period of 5 seconds
```

#### Hue
```Java
int hue = device.getHue();
device.setHue(50);       // returns the hue of the device
device.increaseHue(25);  // increases the hue by a percent amount
device.decreaseHue(75);  // decreases the hue by a percent amount
```

#### Saturation
```Java
int saturation = device.getSaturation();
device.setSaturation(50);       // returns the saturation of the device
device.increaseSaturation(25);  // increases the saturation by a percent amount
device.decreaseSaturation(75);  // decreases the saturation by a percent amount
```

#### Color Temperature
```Java
int ct = device.getColorTemperature();
device.setColorTemperature(50);       // returns the saturation of the device
device.increaseColorTemperature(25);  // increases the saturation by a percent amount
device.decreaseColorTemperature(75);  // decreases the saturation by a percent amount
```

### Effects
The API includes various effect methods for adding, removing, renaming, previewing, and getting effects from the Nanoleaf device. Below are a few examples, but refer to the project documentation for many more.

```Java
String currentEffect = device.getCurrentEffectName();
device.setEffect(effectName);
device.addEffect(effect);
device.deleteEffect(effectName);
device.renameEffect(effectName, newName);
device.previewEffect(effect);
```

### Panel Layout
Information about the arrangement of a Nanoleaf device's panels can be retrieved. Below are a few examples.
```Java
int numPanels = device.getNumPanels();
int sideLength = device.getSideLength();
Panel[] panels = device.getPanels();
Panel[] panels = device.getPanelsRotated();
```

### Rhythm (Aurora only)
Information about the rhythm module on the Aurora such as mode, connected/not connected, active/not active, and aux available can be accessed using getters. Below are a few examples.

```Java
boolean connected = aurora.isRhythmConnected();
boolean active = aurora.isRhyhtmMicActive();
boolean auxAvailable = aurora.isRhythmAuxAvailable();
```

### External Streaming
External streaming is a useful feature that allows for fast and continuous updating of a Nanoleaf device over UDP.

The following example initializes external streaming mode, then sets a few panels to purple.
```Java
Panel[] panels = device.getPanels();
device.enableExternalStreaming();                               // enable external streaming
for (int i = 0; i < 4; i++)
    device.setPanelExternalStreaming(panels[i], "#FF00FF", 1);  // set a few panels to purple
```

You can also send much more complicated static and animated effects very quickly using external streaming.

## The Effect Class
There are three types of effects: plugins, static, and custom. Plugins (also called motions) are effects written using the Nanoleaf SDK (C++) that define how effects should be rendered. Most effects are of this type. Static effects are motionless effects that can be used for displaying a still image or setting the color of all the panels. Custom effects are frame-by-frame animations that are very customizable.

### Custom Effects
Custom effects allow you to create highly customizable animations. A custom effect is made up of a sequence of frames defined for each panel, where each frame has a color and a set time that it takes to transition to that color.

The following example creates a custom effect that cycles through a few colors on all the panels with various transition times.

```Java
CustomEffect ef = new CustomEffect.Builder(device)
                    .addFrameToAllPanels(new Frame(Color.RED, 3))
				    .addFrameToAllPanels(new Frame(Color.ORANGE, 10))
				    .addFrameToAllPanels(new Frame(Color.YELLOW, 5))
				    .addFrameToAllPanels(new Frame(Color.GREEN, 7))
				    .addFrameToAllPanels(new Frame(Color.BLUE, 3))
				    .addFrameToAllPanels(new Frame(Color.MAGENTA, 11))
				    .build("My Animation", true);  // build an animation called "My Animation" that loops
device.displayEffect(ef);
```

### Static Effects
Static effects are a subset of custom effects; they are created very similarly but they can only have one frame per panel. Static effects are created using the StaticEffect.Builder class.

## Events (WIP)
Events have not yet been fully implemented.

## Schedules (WIP)
Schedules have not yet been fully implemented. In fact, they are completely broken on the latest firmware.

## Asynchronous
Almost every synchronous method that communicates with the Nanoleaf device has an accompanying asynchronous method. The naming scheme for these methods is "methodName...Async" (for example, turnOn() and turnOnAsync()).

Asynchronous methods take a parameterized `NanoleafCallback` object as an additional argument, which is a callback interface. When the task completes, NanoleafCallback.onCompleted(status, data, device) is called which returns three parameters. The first parameter (status) indicates the completion status of the task, and on success, is set to NanoleafCallback.SUCCESS. The second parameter (data) returns the data expected to be returned by the task, or null if the task did not succeed. The third parameter (device) is the `NanoleafDevice` object of the device that completed the task.

The following example gets all of the effects on a Nanoleaf device asynchronously:
```Java
device.getAllEffectsAsync((status, effects, device) -> {
    if (status == NanoleafCallback.SUCCESS) {
        // success!
    }
    else {
        // uh oh
    }
});
```

## Exceptions
### NanoleafException
This exception will be thrown if an HTTP error code is returned from the Nanoleaf device. You may run into the following error codes:
- 401 (Unauthorized)  --  Thrown if you try to use methods from a Nanoleaf device with an invalid access token
- 422 (Unprocessable entity)  --  Thrown if the Nanoleaf device rejects the arguments you provided for a method 

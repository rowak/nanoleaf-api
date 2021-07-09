# Nanoleaf Java Library
This is an extensive synchronous and asynchronous Java library for the Nanoleaf RESTful API. The Aurora (Light Panels), Canvas, and Shapes devices are all supported, and all API features are supported.

This library is the successor to my old Aurora Java library. It has a significant number of improvements, including:
- Nanoleaf Shapes and Canvas devices are supported
- Asynchronous methods
- Helper classes for group devices
- Effects class redesigned for Effect v2
- Many broken features have been fixed

## [Documentation](https://rawcdn.githack.com/rowak/nanoleaf-api/21241ecdfc84c54b7177469de00489a421c94b34/doc/index.html)

## Table of Contents
1. **[Installation](#installation)**
    1. **[Maven](#maven)**
    2. **[Manual](#manual)**
2. **[Connecting to a Device](#connecting-to-a-device)**
3. **[Controlling a Device](#controlling-a-device)**
    1. **[State](#state)**
        1. **[On/Off](#onoff)**
        2. **[Brightness](#brightness)**
        3. **[Hue](#hue)**
        4. **[Saturation](#saturation)**
        5. **[Color Temperature](#color-temperature)**
    2. **[Effects](#effects)**
    3. **[Panel Layout](#panel-layout)**
    4. **[Rhythm (Aurora Only)](#rhythm-aurora-only)**
    5. **[External Streaming](#external-streaming)**
4. **[The Effect Class](#the-effect-class)**
    1. **[Plugin Effects](#plugin-effects)**
    2. **[Custom Effects](#custom-effects)**
    3. **[Static Effects](#static-effects)**
5. **[Events](#events)**
    1. **[Low-Latency Touch Events](#low-latency-touch-events)**
6. **Schedules (WIP)**
7. **[Asynchronous](#asynchronous)**
8. **[Groups](#groups)**
9. **[Exceptions](#exceptions)**
10. **[Used Libraries](#used-libraries)**

## Installation
### Maven
```xml
<dependency>
  <groupId>io.github.rowak</groupId>
  <artifactId>nanoleaf-api</artifactId>
  <version>0.1.1</version>
</dependency>
```

### Manual
You can download the [compiled jar](https://repo1.maven.org/maven2/io/github/rowak/nanoleaf-api/) and import it into your project.

## Connecting to a Device
First, search for all of the existing Nanoleaf devices connected to your local network, then select one from the returned list. You can also do this asynchronously.

```Java
int timeout = 2000;
List<NanoleafDeviceMeta> devices = NanoleafSetup.findNanoleafDevices(timeout);
```

Next create an access token to authenticate with the chosen device. You must first physically hold down the power button on your device for 5-7 seconds until the LED starts flashing before running the following code. Make sure to write down your access token for future use, however you can create as many as you like.
```Java
NanoleafDeviceMeta meta = ...
String accessToken = NanoleafSetup.createAccessToken(meta.getHostName(), meta.getPort());
```

Finally, you can connect to a device using the following code.
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
The API includes various effect methods for adding, removing, renaming, previewing, and getting effects from the Nanoleaf device. Below are a few examples, but refer to the [project documentation](#documentation) for many more.

```Java
String currentEffect = device.getCurrentEffectName();
device.setEffect(effectName);
device.addEffect(effect);
device.deleteEffect(effectName);
device.renameEffect(effectName, newName);
device.displayEffect(effect);
```

### Panel Layout
Information about the arrangement of a Nanoleaf device's panels can be retrieved. Below are a few examples.
```Java
int numPanels = device.getNumPanels();
int sideLength = device.getSideLength();
List<Panel> panels = device.getPanels();
List<Panel> panels = device.getPanelsRotated();
```

Another useful method for processing panel data is the `device.getNeighborPanels(Panel, List<Panel>)` method, which returns the panels that are directly connected to a specific panel.

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
List<Panel> panels = device.getPanels();
device.enableExternalStreaming();                                   // enable external streaming
for (int i = 0; i < 4; i++)
    device.setPanelExternalStreaming(panels.get(i), "#FF00FF", 1);  // set a few panels to purple
```

You can also send much more complicated static and animated effects very quickly using external streaming.

Note that the Nanoleaf Shapes devices seem to have a limit on how fast they can stream. It seems that about 50ms between requests is the limit. The Aurora does *not* seem to have this limitation.

## The Effect Class
There are three types of effects: plugins, static, and custom. Plugins (also called motions) are effects written using the Nanoleaf SDK (C++) that define how effects should be rendered. Most effects are of this type. Static effects are motionless effects that can be used for displaying a still image or setting the color of all the panels. Custom effects are frame-by-frame animations that are very customizable.

### Plugin Effects
Plugin effects are the most common type of effect. Plugins (or motions) define how the effect is rendered, and the effect contains additional information that can change the appearance of the plugin a bit. The basic behavior of the plugin itself cannot be changed.

Plugin effects can be retrieved from a device, modified, and then re-uploaded to the device to change the effect appearance. The following example will set the transition time of the effect called "My effect" (assumed to be already installed on the device) to 25, which is pretty fast.
```Java
PluginEffect ef = (PluginEffect)device.getEffect("My effect");
ef.getPlugin().putOption("transTime", 25);
device.addEffect(ef);
```

Note that not all plugins use the same plugin options. There are some basic ones that are mostly supported by all plugins such as "transTime" (which is short for "transition time"), but some plugins have special plugin options. You can experiment with this or refer to the official [Nanoleaf API documentation](https://forum.nanoleaf.me/docs#_qnaqwxdrcgiv) for more details (however it is still somewhat lacking).

Alternatively, plugins can also be created from scratch. The following example creates a new plugin effect from scratch using the "wheel" plugin:
```Java
PluginEffect eff = new PluginEffect();
Plugin p = PluginTemplates.getWheelTemplate(); // Plugin templates make effect creation easier
p.putOption("transTime", 10);                  // Change the default transition time to 10
eff.setPlugin(p);
eff.setName("My Animation");
eff.setVersion("2.0");                         // Effect v2.0 is the latest version
eff.setEffectType("plugin");
eff.setColorType("HSB");
eff.setPalette(new Palette.Builder()
                .addColor(Color.RED)
                .addColor(Color.GREEN)
                .addColor(Color.BLUE)
                .build());
device.addEffect(eff);
```

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

## Events
Event listeners can be created to listen for changes to the state of Nanoleaf devices. You can choose to listen for all, or only specific classes of events. A listener for listening to all classes of events is created as follows:
```Java
device.registerEventListener(new NanoleafEventListener() {
    @Override
    public void onOpen() {
        // Called when the listener is created
    }

    @Override
    public void onClosed() {
        // Called when the listener is closed
    }

    @Override
    public void onEvent(Event[] events) {
        // Called when an event occurs (~1-2 second delay)
    }
}, true, true, true, true);
```

An array of events is returned for multiple events that happen at the same time. When you no longer need the event listeners, you can unregister then with the `closeEventListeners()` method on any Nanoleaf device.

### Low-Latency Touch Events
If you need lower latency when working with touch events, you can register a low-latency touch event listener for near-realtime touch events over UDP. The information returned from a low-latency event is more detailed and a bit lower-level than normal events. A low-latency touch event listener is created as follows:
```Java
device.enableTouchEventStreaming(some_port_number); // You need to specify a port to listen on
device.registerTouchEventStreamingListener(new NanoleafTouchEventListener() {
    @Override
    public void onOpen() {
        // Called when the listener is created
    }

    @Override
    public void onClosed() {
        // Called when the listener is closed
    }

    @Override
    public void onEvent(DetailedTouchEvent[] events) {
        // Called when a touch event occurs
    }
});
```

When you no longer need the event listeners, you can unregister them with the `closeTouchEventListeners()` method on any Nanoleaf device.

## Schedules (WIP)
Schedules have not yet been fully implemented. In fact, they are completely broken on the latest firmware.

## Asynchronous
Almost every synchronous method that communicates with the Nanoleaf device has an accompanying asynchronous method. The naming scheme for these methods is "methodName...Async" (for example, turnOn() and turnOnAsync()).

Asynchronous methods take a parameterized `NanoleafCallback` object as an additional argument, which is a callback interface. When the task completes, NanoleafCallback.onCompleted(status, data, device) is called which returns three parameters. The first parameter (status) indicates the completion status of the task, and on success, is set to NanoleafCallback.SUCCESS. The second parameter (data) returns the data expected to be returned by the task, or null if the task did not succeed. The third parameter (device) is the `NanoleafDevice` object of the device that completed the task.

Note that you should close all Nanoleaf devices that use asynchronous methods when you are done using the device (such as just before the application terminates). If you fail to do this, the program will hang when the main thread runs out of code to execute, since other threads will still be running. You can close a Nanoleaf device using the `NanoleafDevice.closeAsync()` method.

Also note that asynchronous requests can *not* be chained together. By this I mean you can't create a second asynchronous request inside the *callback* for an asynchronous request. This will always result with a NanoleafCallback.FAILURE response status. You can however create synchronous requests inside the callback for any asynchronous request as a workaround.

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
...
device.closeAsync(); // close when you're done!
```

## Groups
The `NanoleafGroup` class is a helper for controlling multiple Nanoleaf devices as one. It has almost the same interface as the `NanoleafDevice` class, but there are no getters for individual device properties. Both synchronous and asynchronous methods are available, however, the synchronous methods don't really make much sense to use in most cases, since the operation will be applied to all devices in the group one by one.

The following example shows how to create and use a group:
```Java
Aurora aurora = new Aurora(...);
Shapes hexagons = new Shapes(...);
NanoleafGroup group = new NanoleafGroup();
group.addDevice(aurora);
group.addDevice(hexagons);
group.setOnAsync(true, (status, data, device) -> {
    if (status == NanoleafCallback.SUCCESS) {
        if (device instanceof Aurora) {
            // Aurora turned on
        }
        else if (device instanceof Shapes) {
            // Shapes turned on
        }
    }
});
```

## Exceptions
### NanoleafException
This exception will be thrown if an HTTP error code is returned from the Nanoleaf device. You may run into the following error codes:
- 401 (Unauthorized)  --  Thrown if you try to use methods from a Nanoleaf device with an invalid access token
- 422 (Unprocessable entity)  --  Thrown if the Nanoleaf device rejects the arguments you provided for a method 

Note that exceptions should not be thrown when using the asynchronous methods. These methods use status codes for error reporting.

## Used Libraries
- [JSON-Java](https://github.com/stleary/JSON-java)
- [OkHttp](https://github.com/square/okhttp)
- [OkSSE](https://github.com/heremaps/oksse)
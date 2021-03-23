## Document Scanner Cordova Plugin
Plugin for live document detection and auto cropping, returns either a URI or a base64 encoded string of the captured image.

##### Supported platforms
- Android. Min SDK = 24, Target SDK = 30. Requires AndroidX.

### Usage
`DocScannerPlugin.scan(successCallback, errorCallback, options)`

### Parameters
- {function} success - callback function called when an image has been scanned successfully. Returns image URI or image as base64 depending on the options passed in;
- {function} error - callback function called when an error occurs. It returns the error message as a string;
- {object} options - options for document scanner;

### Properties
| Prop                        | Default                  |   Type    | Description                                                       |
| :-------------------------- | :----------------------: | :-------: | :---------------------------------------------------------------- |
| overlayColor                | `rgba(66,165,245, 0.7)`  | `string`  | RGBA color of the detected rectangle                              |
| borderColor                 | `rgba(66,165,245, 0.7)`  | `string`  | RGBA color of the borders of detected rectangle                   |
| detectionCountBeforeCapture | `15`                     | `integer` | Number of correct rectangle to detect before capture              |
| enableTorch                 | `false`                  | `bool`    | Allows to active or deactivate flash during document detection    |
| brightness                  | `10`                     | `float`   | Increase or decrease image brightness. Normal as default.         |
| contrast                    | `1`                      | `float`   | Increase or decrease image contrast. Normal as default            |
| useBase64                   | `false`                  | `bool`    | If base64 representation should be passed instead of image uri's  |
| captureMultiple             | `false`                  | `bool`    | Keeps the scanner on after a successful capture                   |
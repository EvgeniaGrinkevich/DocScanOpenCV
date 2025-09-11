## Document Scanner Cordova Plugin
Plugin for live document detection and auto cropping, returns either a URI or a base64 encoded string of the captured image.

##### Supported platforms
- Android. Min SDK = 24, Target SDK = 35.

### Usage
`DocScannerPlugin.scan(successCallback, errorCallback, options)`

### Parameters
- {function} success - callback function called when an image has been scanned successfully. Returns image URI or image as base64 depending on the options passed in;
- {function} error - callback function called when an error occurs. It returns the error message as a string;
- {object} options - options for document scanner;

### Properties
| Prop                        | Default                  |   Type    | Description                                                                     |
| :-------------------------- | :----------------------: | :-------: | :-----------------------------------------------------------------------------: |
| overlayColor                | `rgba(66,165,245, 0.7)`  | `string`  | Color of the detected rectangle. See supported colors below.                    |
| borderColor                 | `rgba(66,165,245, 0.7)`  | `string`  | Color color of the borders of detected rectangle. See supported colors below.   |
| detectionCountBeforeCapture | `15`                     | `integer` | Number of correct rectangle to detect before capture                            |
| enableTorch                 | `false`                  | `bool`    | Allows to active or deactivate flash during document detection                  |
| brightness                  | `10`                     | `float`   | Increase or decrease image brightness.Recommended values [0-100]                |
| contrast                    | `1`                      | `float`   | Increase or decrease image contrast. Recommended values [1.0-3.0]               |           
| useBase64                   | `false`                  | `bool`    | If base64 representation should be passed instead of image uri's                |
| captureMultiple             | `false`                  | `bool`    | Keeps the scanner on after a successful capture                                 |

### Sample options JSON
All properties are optional
```
{
  "overlayColor": "rgba(66,165,245, 0.7)",
  "borderColor": "rgba(66,165,245, 0.7)",
  "detectionCountBeforeCapture": 15,
  "enableTorch": false,
  "brightness": 10,
  "contrast": 1,
  "useBase64": true,
  "captureMultiple": true
}
```

### Supported overlay colors
`#RRGGBB`, `#AARRGGBB`, `rgba(RRR, GGG, BBB, A.A)`

The following names are also accepted: 
`red`, `blue`, `green`, `black`, `white`, `gray`, `cyan`, `magenta`, `yellow`, `lightgray`, `darkgray`, `grey`, 
`lightgrey`, `darkgrey`, `aqua`, `fuchsia`, `lime`, `maroon`, `navy`, `olive`, `purple`, `silver`, `teal`.


### Contrast and brightness OpenCV transformation 
For contrast and brightness adjustment, plugin use OpenCV `Mat.convertTo()` [function](https://docs.opencv.org/master/d3/d63/classcv_1_1Mat.html#adf88c60c5b4980e05bb556080916978b ) 

For additional information, please see the [link](https://docs.opencv.org/2.4/doc/tutorials/core/basic_linear_transform/basic_linear_transform.html)




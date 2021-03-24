var argscheck = require('cordova/argscheck');
var exec = require('cordova/exec');

module.exports = {
    scan: function (successCallback, errorCallback, options) {
        argscheck.checkArgs('FFO', 'DocScannerPlugin.scan', arguments);
        options = options || {};

        var getValue = argscheck.getValue;

        var overlayColor = getValue(options.overlayColor, "");
        var borderColor = getValue(options.borderColor, "");
        var detectionCountBeforeCapture = getValue(options.detectionCountBeforeCapture, -1);
        var enableTorch = getValue(options.enableTorch, false);
        var brightness = getValue(options.brightness, -1);
        var contrast = getValue(options.contrast, -1);
        var useBase64 = getValue(options.useBase64, false);
        var captureMultiple = getValue(options.captureMultiple, false);

        var args = [overlayColor, borderColor, detectionCountBeforeCapture, enableTorch, brightness, contrast, useBase64, captureMultiple];
        exec(successCallback, errorCallback, 'DocScannerPlugin', 'scan', args);
    }
};
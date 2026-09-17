/* Preview protocol 1.0. Load before business scripts. ES5 syntax; Promise polyfill required on old WebView. */
(function (root) {
  'use strict';
  var sdk = root.WishFoxSDK = root.WishFoxSDK || {};
  if (sdk.__mediaPreviewInstalled) return;
  sdk.__mediaPreviewInstalled = true;
  var pending = {}, listeners = [], counter = 0;
  var previousDispatch = sdk.__dispatch;
  sdk.__dispatch = function (message) {
    if (typeof message === 'string') message = JSON.parse(message);
    if (message.type === 'response' && pending[message.id]) {
      var task = pending[message.id];
      delete pending[message.id];
      clearTimeout(task.timer);
      if (message.success) task.resolve(message.data);
      else { var error = new Error(message.message || message.code); error.code = message.code; task.reject(error); }
    }
    if (message.type === 'event' && message.event === 'media.previewChanged') {
      listeners.slice().forEach(function (listener) {
        try { listener(message); } catch (ignored) { /* One subscriber cannot break delivery. */ }
      });
    }
    if (previousDispatch) previousDispatch.call(sdk, message);
  };
  // Use the project's complete Bridge when available. This fallback only supplies transport.
  function invoke(method, params) {
    if (sdk.invoke) return sdk.invoke(method, params);
    return new Promise(function (resolve, reject) {
      if (!root.WishFoxNative || typeof root.WishFoxNative.postMessage !== 'function') {
        var unavailable = new Error('BRIDGE_NOT_AVAILABLE'); unavailable.code = 'BRIDGE_NOT_AVAILABLE'; reject(unavailable); return;
      }
      var id = 'preview-' + Date.now() + '-' + (++counter) + '-' + Math.random().toString(36).slice(2, 10);
      var timer = setTimeout(function () {
        delete pending[id]; var error = new Error('TIMEOUT'); error.code = 'TIMEOUT'; reject(error);
      }, 10000);
      pending[id] = { resolve: resolve, reject: reject, timer: timer };
      try {
        root.WishFoxNative.postMessage(JSON.stringify({ version: '1.0', type: 'request', id: id,
          method: method, timestamp: Date.now(), params: params || {} }));
      } catch (error) { clearTimeout(timer); delete pending[id]; reject(error); }
    });
  }
  sdk.media = sdk.media || {};
  sdk.media.previewVideo = function (params) { return invoke('media.previewVideo', params); };
  sdk.media.previewImage = function (params) { return invoke('media.previewImage', params); };
  sdk.media.closePreview = function (previewId) { return invoke('media.closePreview', { previewId: previewId }); };
  sdk.media.getPreviewCapabilities = function () { return invoke('bridge.getCapabilities', {}); };
  sdk.media.onPreviewChanged = function (handler) {
    listeners.push(handler);
    return function () { var index = listeners.indexOf(handler); if (index >= 0) listeners.splice(index, 1); };
  };
}(window));

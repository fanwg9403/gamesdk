/* Auth protocol 1.0. ES5; old WebView requires a Promise polyfill before this file. */
(function (root) {
  'use strict';
  var sdk = root.WishFoxSDK = root.WishFoxSDK || {};
  if (sdk.__authInstalled) return;
  sdk.__authInstalled = true;
  var pending = {}, counter = 0, refreshTask = null, loginTask = null, listeners = [];
  var previousDispatch = sdk.__dispatch;
  function error(code) { var value = new Error(code); value.code = code; return value; }
  sdk.__dispatch = function (message) {
    if (typeof message === 'string') {
      try { message = JSON.parse(message); } catch (ignored) { return; }
    }
    if (!message) return;
    var task = pending[message.id];
    if (message.type === 'response' && task) {
      delete pending[message.id];
      clearTimeout(task.timer);
      if (message.success) task.resolve(message.data);
      else task.reject(error(message.code || 'SESSION_EXCHANGE_FAILED'));
    }
    if (message.type === 'event' && message.event === 'auth.changed') {
      listeners.slice().forEach(function (listener) {
        try { listener(message); } catch (ignored) { }
      });
    }
    if (previousDispatch) previousDispatch.call(sdk, message);
  };
  function invoke(method, params, timeout) {
    return new Promise(function (resolve, reject) {
      if (!root.WishFoxNative || typeof root.WishFoxNative.postMessage !== 'function') {
        reject(error('BRIDGE_NOT_AVAILABLE')); return;
      }
      var id = 'auth-' + Date.now() + '-' + (++counter) + '-' + Math.random().toString(36).slice(2, 10);
      var timer = timeout ? setTimeout(function () {
        delete pending[id]; reject(error('TIMEOUT'));
      }, timeout) : null;
      pending[id] = { resolve: resolve, reject: reject, timer: timer };
      try {
        root.WishFoxNative.postMessage(JSON.stringify({ version: '1.0', type: 'request', id: id,
          method: method, timestamp: Date.now(), params: params || {} }));
      } catch (failure) { clearTimeout(timer); delete pending[id]; reject(failure); }
    });
  }
  sdk.auth = sdk.auth || {};
  sdk.auth.getState = function () { return invoke('auth.getState', {}, 5000); };
  sdk.auth.login = function (params) {
    if (loginTask) return loginTask;
    // 用户可长时间阅读协议，不以普通网络超时结束登录。
    loginTask = invoke('auth.login', params, 0).then(function (value) {
      loginTask = null; return value;
    }, function (failure) { loginTask = null; throw failure; });
    return loginTask;
  };
  sdk.auth.refreshSession = function (params) {
    if (refreshTask) return refreshTask;
    refreshTask = invoke('auth.refreshSession', params, 20000).then(function (value) {
      refreshTask = null; return value;
    }, function (failure) { refreshTask = null; throw failure; });
    return refreshTask;
  };
  sdk.auth.onChanged = function (handler) {
    listeners.push(handler);
    return function () { var index = listeners.indexOf(handler); if (index >= 0) listeners.splice(index, 1); };
  };
  root.addEventListener('pagehide', function () {
    Object.keys(pending).forEach(function (id) {
      var task = pending[id]; delete pending[id]; clearTimeout(task.timer);
      task.reject(error('PAGE_UNLOADED'));
    });
    loginTask = refreshTask = null;
  });
}(window));

const assert = require('assert');
const fs = require('fs');
const vm = require('vm');
const auth = fs.readFileSync('foxsdk/src/main/assets/wishfox-auth-bridge.js', 'utf8');
const media = fs.readFileSync('foxsdk/src/main/assets/wishfox-media-bridge.js', 'utf8');

function setup(order = [auth, media], native = true) {
  const requests = [], timers = new Map(), events = {};
  let timerId = 0;
  const window = {addEventListener: (event, action) => { events[event] = action; }};
  if (native) window.WishFoxNative = {postMessage: raw => requests.push(JSON.parse(raw))};
  const context = {window, Promise,
    setTimeout: (fn, ms) => { timers.set(++timerId, {fn, ms}); return timerId; },
    clearTimeout: id => timers.delete(id)};
  order.forEach(script => vm.runInNewContext(script, context));
  return {sdk: window.WishFoxSDK, requests, timers, events, window, context,
    respond: (request, code, data) => window.WishFoxSDK.__dispatch({type: 'response',
      id: request.id, method: request.method, success: code === 'OK', code, data})};
}

(async () => {
  for (const order of [[auth, media], [media, auth]]) {
    const env = setup(order), sdk = env.sdk;
    const a = sdk.auth.refreshSession({reason: 'expired'});
    const b = sdk.auth.refreshSession();
    assert.strictEqual(a, b, 'refresh must be single-flight');
    assert.equal(env.requests.length, 1);
    assert.equal(env.requests[0].method, 'auth.refreshSession');
    assert.equal([...env.timers.values()][0].ms, 20000);
    env.respond(env.requests[0], 'OK', {sessionToken: 'short', expiresIn: 300});
    assert.equal((await a).sessionToken, 'short');
    assert.equal(env.timers.size, 0);
    const login = sdk.auth.login({exchangeH5Session: true});
    assert.strictEqual(login, sdk.auth.login());
    assert.equal(env.timers.size, 0, 'reading agreement must not time out login');
    env.respond(env.requests[1], 'USER_CANCELLED');
    await assert.rejects(login, error => error.code === 'USER_CANCELLED');
    const image = sdk.media.previewImage({url: 'https://cdn.example.com/a.png'});
    env.respond(env.requests[2], 'OK', {previewId: 'p1'});
    assert.equal((await image).previewId, 'p1');
    let count = 0;
    const off = sdk.auth.onChanged(() => count++);
    sdk.__dispatch({type: 'event', event: 'auth.changed', data: {status: 'anonymous'}});
    off();
    sdk.__dispatch({type: 'event', event: 'auth.changed', data: {status: 'anonymous'}});
    assert.equal(count, 1);
    const method = sdk.auth.refreshSession;
    vm.runInNewContext(auth, env.context);
    assert.strictEqual(sdk.auth.refreshSession, method, 'installation is idempotent');
    const retry = sdk.auth.refreshSession();
    [...env.timers.values()][0].fn();
    await assert.rejects(retry, error => error.code === 'TIMEOUT');
    // A late response cannot resurrect a timed-out request.
    env.respond(env.requests[3], 'OK', {sessionToken: 'late'});
    const current = sdk.auth.refreshSession();
    env.respond(env.requests[4], 'AUTH_REQUIRED');
    await assert.rejects(current, error => error.code === 'AUTH_REQUIRED');
    const state = sdk.auth.getState();
    env.respond(env.requests[5], 'OK', {status: 'authenticated', sessionStatus: 'none'});
    assert.equal((await state).sessionStatus, 'none');
    const wait = sdk.auth.login();
    env.events.pagehide();
    await assert.rejects(wait, error => error.code === 'PAGE_UNLOADED');
  }
  const unavailable = setup([auth], false);
  await assert.rejects(unavailable.sdk.auth.refreshSession(), error => error.code === 'BRIDGE_NOT_AVAILABLE');
  const throwing = setup([auth]);
  throwing.window.WishFoxNative.postMessage = () => { throw new Error('transport failed'); };
  await assert.rejects(throwing.sdk.auth.refreshSession(), /transport failed/);
  assert.equal(throwing.timers.size, 0);
  console.log('auth-bridge-check: single-flight, lifecycle, timeouts, errors, dispatch ordering and media coexistence passed');
})().catch(error => { console.error(error); process.exitCode = 1; });

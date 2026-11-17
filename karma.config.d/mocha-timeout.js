// Mocha's 2 s default times out heavy browser test suites; match the 120 s Node test timeout,
// and raise Karma's Socket.IO ping/activity timeouts so synchronous suites do not disconnect.
config.client = config.client || {};
config.client.mocha = Object.assign({}, config.client.mocha, { timeout: 120000 });
config.set({
  pingTimeout: 120000,
  browserNoActivityTimeout: 120000,
  browserDisconnectTimeout: 30000,
  browserDisconnectTolerance: 3,
});

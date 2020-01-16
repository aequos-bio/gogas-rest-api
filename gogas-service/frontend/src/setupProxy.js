// eslint-disable-next-line import/no-extraneous-dependencies
const proxy = require("http-proxy-middleware");

module.exports = function configureProxy(app) {
  app.use(
    proxy(["/authenticate", "/info", "/api"], {
      target: "http://localhost:8081",
      ws: true
    })
  );
};

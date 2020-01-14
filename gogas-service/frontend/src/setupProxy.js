const proxy = require("http-proxy-middleware");

module.exports = function (app) {
  app.use(
    proxy(["/authenticate", "/info", "/api"], {
      target: "http://localhost:8081",
      ws: true
    })
  );
};

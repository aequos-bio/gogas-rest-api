const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function configureProxy(app) {
  app.use(
    createProxyMiddleware(['/authenticate', '/info', '/api'], {
      target: 'http://localhost:8081',
      ws: true,
    }),
  );
};

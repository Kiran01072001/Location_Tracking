const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
  app.use(
    '/api',
    createProxyMiddleware({
      target: 'http://183.82.114.29:9511',
      changeOrigin: true,
    })
  );

  app.use(
    '/ws',
    createProxyMiddleware({
      target: 'http://183.82.114.29:9511',
      changeOrigin: true,
      ws: true, // Enable WebSocket proxying
    })
  );
};

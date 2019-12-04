const withSourceMaps = require('@zeit/next-source-maps')()

module.exports = withSourceMaps({
  webpack: (config, options) => {
    config.module.rules.push({
      test: /\.svg$/,
      use: ['@svgr/webpack'],
    })

    // Ask Webpack to replace @sentry/node imports with @sentry/browser when
    // building the browser's bundle and not being served by Node.js.
    if (!options.isServer) {
      config.resolve.alias['@sentry/node'] = '@sentry/browser'
    }

    return config
  },
})

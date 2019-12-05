const withSourceMaps = require('@zeit/next-source-maps')()
const { ANALYZE } = process.env

module.exports = withSourceMaps({
  webpack: (config, { isServer }) => {
    if (ANALYZE && !isServer) {
      const { BundleAnalyzerPlugin } = require('webpack-bundle-analyzer')

      config.plugins.push(
        new BundleAnalyzerPlugin({
          analyzerMode: 'static',
          analyzerPort: 8888,
          openAnalyzer: true,
          defaultSizes: 'gzip',
        }),
      )
    }

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

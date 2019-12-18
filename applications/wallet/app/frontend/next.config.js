const withSourceMaps = require('@zeit/next-source-maps')()

require('dotenv').config()

const { ANALYZE, GOOGLE_OAUTH_API_KEY } = process.env

module.exports = withSourceMaps({
  publicRuntimeConfig: {
    GOOGLE_OAUTH_API_KEY,
  },
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

    return config
  },
})

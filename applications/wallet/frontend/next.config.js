require('dotenv').config()

const {
  ANALYZE,
  CI_COMMIT_SHA,
  GOOGLE_OAUTH_CLIENT_ID,
  ENVIRONMENT,
  ENABLE_SENTRY,
} = process.env

module.exports = {
  productionBrowserSourceMaps: true,
  reactStrictMode: true,
  env: {
    CI_COMMIT_SHA,
  },
  publicRuntimeConfig: {
    GOOGLE_OAUTH_CLIENT_ID,
    ENVIRONMENT,
    ENABLE_SENTRY,
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

    const fileLoaderRule = config.module.rules.find(
      (rule) => rule.test && rule.test.test('.svg'),
    )

    fileLoaderRule.exclude = /\.svg$/

    config.module.rules.push({
      test: /\.svg$/,
      loader: require.resolve('@svgr/webpack'),
    })

    return config
  },
}

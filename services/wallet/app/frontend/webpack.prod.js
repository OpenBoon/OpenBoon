const { join, resolve } = require('path')
const merge = require('webpack-merge')
const common = require('./webpack.common.js')
const TerserPlugin = require('terser-webpack-plugin')
const OptimizeCSSAssetsPlugin = require("optimize-css-assets-webpack-plugin")
const CopyWebpackPlugin = require("copy-webpack-plugin")
const ImageminPlugin = require("imagemin-webpack-plugin").default

const ROOT_DIR = resolve(__dirname)

module.exports = merge(common, {
  mode: 'production',
  output: {
    path: join(ROOT_DIR, 'build'),
    publicPath: '/wallet',
    filename: 'bundle.[hash].js'
  },
  devtool: false,
  optimization: {
    splitChunks: {
      cacheGroups: {
        commons: {
          test: /[\\/]node_modules[\\/]/,
          name: 'vendors',
          chunks: 'all'
        }
      }
    },
    minimizer: [
      new TerserPlugin({
        cache: true,
        parallel: true,
        terserOptions: {
          output: {
            comments: false,
          }
        }
      }),
      new OptimizeCSSAssetsPlugin({})
    ]
  },
  plugins: [
    // Copy the images folder and optimize all the images
    new CopyWebpackPlugin([{
      from: 'src/images/',
      to: 'images/'
    }]),
    new ImageminPlugin({
      test: /\.(png|svg|jpg|gif)$/
    })
  ]
})
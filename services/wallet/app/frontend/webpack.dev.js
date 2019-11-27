const { join, resolve } = require('path')
const common = require('./webpack.common.js')
const merge = require('webpack-merge')
const webpack = require('webpack')
const Dotenv = require('dotenv-webpack')

const ROOT_DIR = resolve(__dirname)

module.exports = merge(common, {
  mode: 'development',
  output: {
    path: join(ROOT_DIR, 'build'),
    publicPath: '',
    filename: 'bundle.[hash].js'
  },
  devtool: 'inline-source-map',
  devServer: {
    contentBase: '/build',
    hot: true,
    historyApiFallback: true,
    publicPath: '',
  },
  plugins: [
    new webpack.HotModuleReplacementPlugin(),
    new Dotenv({
      path: './.env.development',
      safe: true
    })
  ]
})
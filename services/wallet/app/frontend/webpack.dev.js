const { join, resolve } = require('path')
const merge = require('webpack-merge')
const common = require('./webpack.common.js')

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
    contentBase: './build',
    hot: true,
    historyApiFallback: true,
    publicPath: ''
  }
})
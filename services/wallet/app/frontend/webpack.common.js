const { join, resolve } = require('path')
const webpack = require('webpack')
const { CleanWebpackPlugin } = require('clean-webpack-plugin')
const HtmlWebpackPlugin = require('html-webpack-plugin')
const MiniCssExtractPlugin = require('mini-css-extract-plugin')

const ENV = process.env.NODE_ENV
const ROOT_DIR = resolve(__dirname)
const SRC_DIR = join(ROOT_DIR, 'src')

module.exports = {
  mode: ENV,
  entry: SRC_DIR,
  output: {
    path: join(ROOT_DIR, 'build'),
    publicPath: 'wallet',
    filename: 'bundle.[hash].js'
  },
  module: {
    rules: [
      {
        test: [/.js$/],
        exclude: /node_modules/,
        use: ['babel-loader']
      },
      {
        test: [/.css$|.scss$/],
        use: [
          MiniCssExtractPlugin.loader,
          'css-loader',
          'sass-loader'
        ]
      }, {
        test: /\.(png|svg|jpg|gif)$/,
        use: [
          {
            loader: 'file-loader',
            options: {
              name: '[name].[hash].[ext]',
              outputPath: './images'
            }
          }
        ]
      }, {
        test: /\.(woff|woff2|eot|ttf|otf)$/,
        use: [
          'file-loader'
        ]
      }
    ]
  },
  resolve: {
    extensions: ['*', '.js']
  },
  devtool: 'inline-source-map',
  devServer: {
    contentBase: './build',
    hot: true
  },
  plugins: [
    new CleanWebpackPlugin(),
    new webpack.HotModuleReplacementPlugin(),
    new HtmlWebpackPlugin({
      template: join(SRC_DIR, 'index.html'),
      filename: join(ROOT_DIR, 'build/index.html')
    }),
    new MiniCssExtractPlugin({
      filename: '[name].[hash].css'
    })
  ]
}
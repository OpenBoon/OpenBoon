const { join, resolve } = require('path')
const webpack = require('webpack')
const { CleanWebpackPlugin } = require('clean-webpack-plugin')
const HtmlWebpackPlugin = require('html-webpack-plugin')
const MiniCssExtractPlugin = require('mini-css-extract-plugin')
const dotenv = require('dotenv')

const ROOT_DIR = resolve(__dirname)
const SRC_DIR = join(ROOT_DIR, 'src')
const ENV = process.env.NODE_ENV
const ARCHIVIST_API_URL = process.env.ARCHIVIST_API_URL

// handle configurable environmental variables
const defaultEnv = dotenv.config().parsed;
const customEnv = {
  ARCHIVIST_API_URL
}
const envKeys = Object.keys(defaultEnv).reduce((current, nextKey) => {
  const nextValue =
    customEnv[nextKey] ?
      customEnv[nextKey] : JSON.stringify(defaultEnv[nextKey])
  current[nextKey] = nextValue;
  return current
}, {})

const envConfig = { 'process.env': { ...envKeys } }

module.exports = {
  mode: ENV,
  entry: SRC_DIR,
  output: {
    path: join(ROOT_DIR, 'dist'),
    publicPath: '/',
    filename: 'bundle.[hash].js',
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
    contentBase: './dist',
    hot: true,
    historyApiFallback: true,
  },
  plugins: [
    new CleanWebpackPlugin(),
    new webpack.HotModuleReplacementPlugin(),
    new HtmlWebpackPlugin({
      template: join(SRC_DIR, 'index.html'),
      filename: join(ROOT_DIR, 'dist/index.html')
    }),
    new MiniCssExtractPlugin({
      filename: '[name].[hash].css'
    }),
    new webpack.DefinePlugin(envConfig),
  ]
}
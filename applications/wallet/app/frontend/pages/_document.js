import React from 'react'
import Document, { Head, Main, NextScript } from 'next/document'

import StylesReset from '../src/Styles/Reset'

class MyDocument extends Document {
  render() {
    return (
      <html lang="en">
        <Head>
          <meta name="viewport" content="width=device-width, initial-scale=1" />
          <meta charSet="utf-8" />
          <script src="https://apis.google.com/js/platform.js" async defer />
        </Head>
        <StylesReset />
        <body>
          <Main />
          <NextScript />
        </body>
      </html>
    )
  }
}

export default MyDocument

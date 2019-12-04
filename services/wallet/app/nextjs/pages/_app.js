/* eslint-disable react/jsx-props-no-spreading */
import App from 'next/app'
import * as Sentry from '@sentry/browser'

import Authentication from '../src/Authentication'

Sentry.init({
  dsn: 'https://d772538aae2649d38a8931583ed7719b@sentry.io/1504338',
})

class MyApp extends App {
  render() {
    const { Component, pageProps } = this.props

    return (
      <Authentication>
        {({ user, logout }) => (
          <Component user={user} logout={logout} {...pageProps} />
        )}
      </Authentication>
    )
  }
}

export default MyApp

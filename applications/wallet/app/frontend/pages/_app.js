/* eslint-disable react/jsx-props-no-spreading */
import App from 'next/app'
import * as Sentry from '@sentry/browser'

import Authentication from '../src/Authentication'

if (process.env.NODE_ENV === 'production') {
  Sentry.init({
    dsn: 'https://09e9c3fc777c469ab784ff4367ff54bb@sentry.io/1848515',
  })
}

class MyApp extends App {
  render() {
    const { Component, pageProps } = this.props

    return (
      <Authentication>
        {({ user, logout, projectId }) => (
          <Component
            user={user}
            logout={logout}
            projectId={projectId}
            {...pageProps}
          />
        )}
      </Authentication>
    )
  }
}

export default MyApp

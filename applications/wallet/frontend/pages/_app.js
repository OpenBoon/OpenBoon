/* eslint-disable react/jsx-props-no-spreading */
import App from 'next/app'
import getConfig from 'next/config'
import * as Sentry from '@sentry/browser'
import 'focus-visible'

import Authentication from '../src/Authentication'

const { publicRuntimeConfig: { SENTRY_DSN } = {} } = getConfig()

if (process.env.NODE_ENV === 'production' && SENTRY_DSN) {
  Sentry.init({
    dsn: SENTRY_DSN,
    release: process.env.CI_COMMIT_SHA,
  })
}

class MyApp extends App {
  static async getInitialProps({ Component, ctx }) {
    const versions = {
      COMMIT_SHA: process.env.CI_COMMIT_SHA,
    }

    if (Component.getInitialProps) {
      const pageProps = await Component.getInitialProps(ctx)

      return { ...versions, pageProps }
    }

    return { ...versions }
  }

  render() {
    const { Component, pageProps, router, err = '' } = this.props

    if (router.route === '/_error') {
      return <Component {...pageProps} err={err} />
    }

    return (
      <Authentication>
        <Component {...pageProps} />
      </Authentication>
    )
  }
}

export default MyApp

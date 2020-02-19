/* eslint-disable react/jsx-props-no-spreading */
import App from 'next/app'
import getConfig from 'next/config'
import * as Sentry from '@sentry/browser'
import 'focus-visible'

import Authentication from '../src/Authentication'

const { publicRuntimeConfig: { ENVIRONMENT, ENABLE_SENTRY } = {} } = getConfig()

if (ENABLE_SENTRY === 'true') {
  Sentry.init({
    dsn: 'https://09e9c3fc777c469ab784ff4367ff54bb@sentry.io/1848515',
    release: process.env.CI_COMMIT_SHA,
    environment: ENVIRONMENT,
  })
}

const AUTHENTICATION_LESS_ROUTES = [
  '/_error',
  '/create-account',
  '/reset-password',
  '/accounts-confirm',
]

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

    if (AUTHENTICATION_LESS_ROUTES.includes(router.route)) {
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

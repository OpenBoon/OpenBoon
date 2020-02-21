/* eslint-disable react/jsx-props-no-spreading */
import App from 'next/app'
import getConfig from 'next/config'
import * as Sentry from '@sentry/browser'
import 'focus-visible'

import User from '../src/User'
import Router from 'next/router'
import Authentication from '../src/Authentication'
import { getUser } from '../src/Authentication/helpers'

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
      const { userId } = getUser()

      if (userId) {
        Router.push('/')
        return null
      }

      return <Component {...pageProps} err={err} />
    }

    return (
      <User initialUser={{}}>
        <Authentication>
          <Component {...pageProps} />
        </Authentication>
      </User>
    )
  }
}

export default MyApp

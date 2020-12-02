/* eslint-disable react/jsx-props-no-spreading */
import App from 'next/app'
import getConfig from 'next/config'
import * as Sentry from '@sentry/browser'

import 'focus-visible'
import '@reach/combobox/styles.css'
import '@reach/listbox/styles.css'
import '@reach/skip-nav/styles.css'
import 'react-grid-layout/css/styles.css'
import 'react-resizable/css/styles.css'

import User from '../src/User'
import Authentication from '../src/Authentication'

const { publicRuntimeConfig: { ENVIRONMENT, ENABLE_SENTRY } = {} } = getConfig()

if (ENABLE_SENTRY === 'true') {
  Sentry.init({
    dsn: 'https://09e9c3fc777c469ab784ff4367ff54bb@sentry.io/1848515',
    release: process.env.CI_COMMIT_SHA,
    environment: ENVIRONMENT,
    beforeSend(event) {
      if (event.exception) {
        Sentry.showReportDialog({ eventId: event.event_id })
      }
      return event
    },
    ignoreErrors: [
      'ResizeObserver loop limit exceeded',
      'The play() request was interrupted by a call to pause()',
    ],
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
    const {
      Component,
      pageProps,
      router: { route },
      err = '',
    } = this.props

    if (route === '/_error') {
      return <Component {...pageProps} err={err} />
    }

    return (
      <User initialUser={{}}>
        <Authentication route={route}>
          <Component {...pageProps} />
        </Authentication>
      </User>
    )
  }
}

export default MyApp

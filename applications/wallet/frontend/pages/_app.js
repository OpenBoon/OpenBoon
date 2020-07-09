/* eslint-disable react/jsx-props-no-spreading */
import App from 'next/app'
import getConfig from 'next/config'
import Router from 'next/router'
import * as Sentry from '@sentry/browser'
import { Integrations as ApmIntegrations } from '@sentry/apm'
import { timestampWithMs } from '@sentry/utils'
import { CacheProvider } from '@emotion/core'
import createCache from '@emotion/cache'

import 'focus-visible'
import '@reach/combobox/styles.css'
import '@reach/listbox/styles.css'
import 'react-grid-layout/css/styles.css'
import 'react-resizable/css/styles.css'

import User from '../src/User'
import Authentication from '../src/Authentication'

import { getPathname } from '../src/Fetch/helpers'

const emotionCache = createCache({
  key: 'emotion-cache',
  prefix: false,
})

const { publicRuntimeConfig: { ENVIRONMENT, ENABLE_SENTRY } = {} } = getConfig()

Router.events.on('routeChangeStart', (pathname) => {
  ApmIntegrations.Tracing.finishIdleTransaction(timestampWithMs())
  ApmIntegrations.Tracing.startIdleTransaction({
    name: getPathname({ pathname }),
    op: 'navigation',
  })
})

if (ENABLE_SENTRY === 'true') {
  Sentry.init({
    dsn: 'https://09e9c3fc777c469ab784ff4367ff54bb@sentry.io/1848515',
    integrations: [
      new ApmIntegrations.Tracing({
        beforeNavigate: getPathname,
        startTransactionOnLocationChange: false,
      }),
    ],
    tracesSampleRate: 1,
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
      <CacheProvider value={emotionCache}>
        <User initialUser={{}}>
          <Authentication route={route}>
            <Component {...pageProps} />
          </Authentication>
        </User>
      </CacheProvider>
    )
  }
}

export default MyApp

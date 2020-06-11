/* eslint-disable react/jsx-props-no-spreading */
import PropTypes from 'prop-types'
import getConfig from 'next/config'
import * as Sentry from '@sentry/browser'
import 'focus-visible'

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

const MyApp = ({ Component, pageProps, router: { route }, err = '' }) => {
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

MyApp.propTypes = {
  Component: PropTypes.node.isRequired,
  pageProps: PropTypes.shape({}).isRequired,
  router: PropTypes.shape({ route: PropTypes.string.isRequired }).isRequired,
  err: PropTypes.string.isRequired,
}

export default MyApp

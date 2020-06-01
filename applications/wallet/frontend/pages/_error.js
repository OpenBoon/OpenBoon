// This file was referenced from https://github.com/zeit/next.js/blob/canary/examples/with-sentry-simple/pages/_error.js

import Error from 'next/error'
import * as Sentry from '@sentry/browser'
import PropTypes from 'prop-types'

const MyError = ({ statusCode, hasGetInitialPropsRun, err }) => {
  if (!hasGetInitialPropsRun && err) {
    // getInitialProps is not called in case of
    // https://github.com/zeit/next.js/issues/8592. As a workaround, we pass
    // err via _app.js so it can be captured
    Sentry.captureException(err)
  }

  return <Error statusCode={statusCode} />
}

MyError.getInitialProps = async ({ res, err, asPath }) => {
  const errorInitialProps = await Error.getInitialProps({ res, err })

  // Workaround for https://github.com/zeit/next.js/issues/8592, mark when
  // getInitialProps has run
  errorInitialProps.hasGetInitialPropsRun = true

  if (errorInitialProps.statusCode === 404) {
    return errorInitialProps
  }

  if (err) {
    // Running on the client (browser).
    //
    // Next.js will provide an err if:
    //
    //  - a page's `getInitialProps` threw or returned a Promise that rejected
    //  - an exception was thrown somewhere in the React lifecycle (render,
    //    componentDidMount, etc) that was caught by Next.js's React Error
    //    Boundary. Read more about what types of exceptions are caught by Error
    //    Boundaries: https://reactjs.org/docs/error-boundaries.html
    Sentry.captureException(err)

    return errorInitialProps
  }

  // If this point is reached, getInitialProps was called without any
  // information about what the error might be. This is unexpected and may
  // indicate a bug introduced in Next.js, so record it in Sentry
  Sentry.captureException(
    new Error(`_error.js getInitialProps missing data at path: ${asPath}`),
  )

  return errorInitialProps
}

MyError.propTypes = {
  statusCode: PropTypes.number.isRequired,
  hasGetInitialPropsRun: PropTypes.bool.isRequired,
  err: PropTypes.string.isRequired,
}

export default MyError

import { Component } from 'react'
import * as Sentry from '@sentry/browser'
import PropTypes from 'prop-types'

import ErrorSvg from './error.svg'

class ErrorBoundary extends Component {
  state = { hasError: false }

  static getDerivedStateFromError() {
    return { hasError: true }
  }

  componentDidCatch(error, info) {
    Sentry.withScope(scope => {
      scope.setExtras(info)
      Sentry.captureException(error)
    })
  }

  render() {
    const { hasError } = this.state
    const { children } = this.props

    if (hasError) {
      return (
        <div>
          <ErrorSvg />
          <br /> Hmmm, something went wrong.
          <br /> Please try refreshing.
        </div>
      )
    }

    return children
  }
}

ErrorBoundary.propTypes = {
  children: PropTypes.node.isRequired,
}

export default ErrorBoundary

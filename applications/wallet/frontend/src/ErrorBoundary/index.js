import { Component } from 'react'
import * as Sentry from '@sentry/browser'
import PropTypes from 'prop-types'

import { colors, typography, constants } from '../Styles'

import ErrorSvg from '../Icons/error.svg'

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
        <div
          css={{
            height: '100%',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            textAlign: 'center',
            color: colors.structure.steel,
            backgroundColor: colors.structure.lead,
            fontSize: typography.size.regular,
            lineHeight: typography.height.regular,
            boxShadow: constants.boxShadows.default,
          }}>
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

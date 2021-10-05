import { Component } from 'react'
import PropTypes from 'prop-types'

import { colors, typography, constants, spacing } from '../Styles'

import ErrorSvg from '../Icons/error.svg'

const STYLES = {
  GLOBAL: {
    height: '100vh',
    padding: spacing.spacious,
  },
  LOCAL: {
    height: '100%',
  },
}

export const VARIANTS = Object.keys(STYLES).reduce(
  (accumulator, style) => ({ ...accumulator, [style]: style }),
  {},
)

class ErrorBoundary extends Component {
  state = { hasError: false }

  static getDerivedStateFromError() {
    return { hasError: true }
  }

  render() {
    const { hasError } = this.state
    const { variant, isTransparent, children } = this.props

    if (hasError) {
      return (
        <div className="ErrorBoundary" css={STYLES[variant]}>
          <div
            css={{
              height: '100%',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              textAlign: 'center',
              color: colors.structure.steel,
              backgroundColor: isTransparent
                ? colors.structure.transparent
                : colors.structure.lead,
              fontSize: typography.size.regular,
              lineHeight: typography.height.regular,
              boxShadow: isTransparent ? 'none' : constants.boxShadows.default,
            }}
          >
            <ErrorSvg width={604} css={{ maxWidth: '80%' }} />
            <br /> Hmmm, something went wrong.
            <br /> Please try refreshing.
          </div>
        </div>
      )
    }

    return children
  }
}

ErrorBoundary.defaultProps = {
  isTransparent: false,
}

ErrorBoundary.propTypes = {
  variant: PropTypes.oneOf(Object.keys(VARIANTS)).isRequired,
  isTransparent: PropTypes.bool,
  children: PropTypes.node.isRequired,
}

export default ErrorBoundary

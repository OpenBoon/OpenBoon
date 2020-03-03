import { Suspense } from 'react'
import PropTypes from 'prop-types'

import ErrorBoundary from '../ErrorBoundary'
import Loading from '../Loading'

const SuspenseBoundary = ({ children }) => {
  return (
    <ErrorBoundary>
      <Suspense fallback={<Loading />}>{children}</Suspense>
    </ErrorBoundary>
  )
}

SuspenseBoundary.propTypes = {
  children: PropTypes.node.isRequired,
}

export default SuspenseBoundary

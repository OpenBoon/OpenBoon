import { Suspense } from 'react'
import { ErrorBoundary } from 'react-error-boundary'
import PropTypes from 'prop-types'

import Error from '../Error'
import Loading from '../Loading'

const SuspenseBoundary = ({ children }) => {
  return (
    <ErrorBoundary FallbackComponent={<Error />}>
      <Suspense fallback={<Loading />}>{children}</Suspense>
    </ErrorBoundary>
  )
}

SuspenseBoundary.propTypes = {
  children: PropTypes.node.isRequired,
}

export default SuspenseBoundary

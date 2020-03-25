import { Suspense } from 'react'
import PropTypes from 'prop-types'
import { useRouter } from 'next/router'

import ErrorBoundary, { VARIANTS } from '../ErrorBoundary'
import Loading from '../Loading'

const SuspenseBoundary = ({ children }) => {
  const {
    query: { projectId },
  } = useRouter()

  return (
    <ErrorBoundary key={projectId} variant={VARIANTS.LOCAL}>
      <Suspense fallback={<Loading />}>{children}</Suspense>
    </ErrorBoundary>
  )
}

SuspenseBoundary.propTypes = {
  children: PropTypes.node.isRequired,
}

export default SuspenseBoundary

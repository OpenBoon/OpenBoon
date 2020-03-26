import { useContext, Suspense } from 'react'
import PropTypes from 'prop-types'

import { ROLES } from '../Roles/helpers'

import { UserContext } from '../User'
import RoleBoundary from '../RoleBoundary'
import ErrorBoundary, { VARIANTS } from '../ErrorBoundary'
import Loading from '../Loading'

const SuspenseBoundary = ({ role, children }) => {
  const {
    user: { roles = {}, projectId },
  } = useContext(UserContext)

  if (role && (!roles[projectId] || !roles[projectId].includes(role))) {
    return <RoleBoundary />
  }

  return (
    <ErrorBoundary key={projectId} variant={VARIANTS.LOCAL}>
      <Suspense fallback={<Loading />}>{children}</Suspense>
    </ErrorBoundary>
  )
}

SuspenseBoundary.defaultProps = {
  role: null,
}

SuspenseBoundary.propTypes = {
  role: PropTypes.oneOf(Object.keys(ROLES)),
  children: PropTypes.node.isRequired,
}

export { SuspenseBoundary as default, ROLES }

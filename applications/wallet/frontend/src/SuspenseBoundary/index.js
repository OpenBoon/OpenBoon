import { useContext, Suspense } from 'react'
import PropTypes from 'prop-types'
import { useRouter } from 'next/router'

import { ROLES } from '../Roles/helpers'

import { UserContext } from '../User'
import RoleBoundary from '../RoleBoundary'
import ErrorBoundary, { VARIANTS } from '../ErrorBoundary'
import Loading from '../Loading'

const SuspenseBoundary = ({ role, isTransparent, children }) => {
  const {
    query: { projectId },
  } = useRouter()

  const {
    user: { roles = {} },
  } = useContext(UserContext)

  if (role && (!roles[projectId] || !roles[projectId].includes(role))) {
    return <RoleBoundary />
  }

  return (
    <ErrorBoundary
      key={projectId}
      variant={VARIANTS.LOCAL}
      isTransparent={isTransparent}
    >
      <Suspense fallback={<Loading isTransparent={isTransparent} />}>
        {children}
      </Suspense>
    </ErrorBoundary>
  )
}

SuspenseBoundary.defaultProps = {
  role: null,
  isTransparent: false,
}

SuspenseBoundary.propTypes = {
  role: PropTypes.oneOf(Object.keys(ROLES)),
  isTransparent: PropTypes.bool,
  children: PropTypes.node.isRequired,
}

export { SuspenseBoundary as default, ROLES }

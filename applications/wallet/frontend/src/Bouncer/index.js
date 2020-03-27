import { useContext } from 'react'
import PropTypes from 'prop-types'

import { ROLES } from '../Roles/helpers'

import { UserContext } from '../User'

const Bouncer = ({ role, children }) => {
  const {
    user: { roles = {}, projectId },
  } = useContext(UserContext)

  // Sorry mate, your name ain't on the list
  if (!roles[projectId] || !roles[projectId].includes(role)) return null

  return children
}

Bouncer.propTypes = {
  role: PropTypes.oneOf(Object.keys(ROLES)).isRequired,
  children: PropTypes.node.isRequired,
}

export { Bouncer as default, ROLES }

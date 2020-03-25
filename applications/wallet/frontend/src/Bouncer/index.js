import { useContext } from 'react'
import PropTypes from 'prop-types'

import { UserContext } from '../User'

export const ROLES = {
  ML_Tools: 'ML_Tools',
  API_Keys: 'API_Keys',
  User_Admin: 'User_Admin',
}

const Bouncer = ({ role, children }) => {
  const {
    user: { roles = [], projectId },
  } = useContext(UserContext)

  // Sorry mate, your name ain't on the list
  if (!roles[projectId] || !roles[projectId].includes(role)) return null

  return children
}

Bouncer.propTypes = {
  role: PropTypes.oneOf(Object.keys(ROLES)).isRequired,
  children: PropTypes.node.isRequired,
}

export default Bouncer

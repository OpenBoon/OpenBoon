import { createContext, useState } from 'react'
import PropTypes from 'prop-types'

export const noop = () => {}

export const UserContext = createContext({
  user: {},
  setUser: noop,
  googleAuth: {},
  setGoogleAuth: noop,
})

const User = ({ initialUser, children }) => {
  const [user, setUser] = useState(initialUser)
  const [googleAuth, setGoogleAuth] = useState({ signIn: noop, signOut: noop })

  return (
    <UserContext.Provider value={{ user, setUser, googleAuth, setGoogleAuth }}>
      {children}
    </UserContext.Provider>
  )
}

User.propTypes = {
  initialUser: PropTypes.shape({}).isRequired,
  children: PropTypes.node.isRequired,
}

export default User

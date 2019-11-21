import React from 'react'
import { withRouter } from 'react-router-dom'

import { useAuth } from '../../context/authContext'
import { useUser } from '../../context/userContext'
import Login from './Login'

function ConnectedLogin(props) {
  const authContext = useAuth()
  const userContext = useUser()

  return <Login {...authContext} {...userContext} {...props} />
}
export default withRouter(ConnectedLogin)

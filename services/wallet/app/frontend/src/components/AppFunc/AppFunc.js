import React, { useReducer } from 'react'
import { BrowserRouter as Router, Route } from 'react-router-dom'
import RequireAuthFunc from '../RequireAuth'
import Wallet from '../Wallet'
// import Login from '../Login'
import Auth from '../Auth'

// import { authState, authReducer } from '../../reducers/authReducer'
import { authenticateUser } from '../../actions/authActions'

function AppFunc() {
  const AuthContext = React.createContext()

  const authState = {
    accessToken: '',
    refreshToken: '',
    isAuthenticated: false,
  }

  function authReducer(state = authState, action) {
    switch (action.type) {
      case 'AUTH_USER':
        return { ...state, isAuthenticated: !state.isAuthenticated }
      default:
        return state
    }
  }

  const [state, dispatch] = useReducer(authReducer, authState)

  return (
    <AuthContext.Provider
      value={
        {
          ...state,
          updateAuthState: () => {
            authenticateUser(dispatch)
          }
        }}>
      <AuthContext.Consumer>
        {props => {
          return (
            <Router>
              <RequireAuthFunc exact path="/" {...props} component={Wallet} />
              <Route path="/login" render={(routeProps) => <Auth {...routeProps}  {...props} />} />
            </Router>
          )
        }}
      </AuthContext.Consumer>
    </AuthContext.Provider>
  )
}

export default AppFunc
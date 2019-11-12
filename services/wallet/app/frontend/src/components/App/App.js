import React, { Component } from 'react'
import { BrowserRouter as Router, Route } from 'react-router-dom'

import RequireAuth from '../RequireAuth'
import Wallet from '../Wallet'
import Login from '../Login'
import { storeAuthAccessTokens } from '../../services/authServices'
import { AuthContext } from '../../context/authContext'
import { ACCESS_TOKEN, REFRESH_TOKEN } from '../../constants/authConstants'

class App extends Component {
  setTokens = data => {
    storeAuthAccessTokens(data.data)
    this.setState({ authTokens: data.data })
  }

  state = {
    authTokens: {},
    setAuthTokens: this.setTokens
  }

  componentWillMount() {
    // check localstorage
    const accessToken = localStorage.getItem(
      ACCESS_TOKEN,
    )

    const refreshToken = localStorage.getItem(
      REFRESH_TOKEN,
    )
    if (accessToken && refreshToken) {
      this.setState({ authTokens: { access: accessToken, refresh: refreshToken } })
    }
  }

  render() {
    return (
      <AuthContext.Provider value={this.state}>
        <AuthContext.Consumer>
          {props => (
            <Router>
              <RequireAuth exact path='/' component={Wallet} />
              <Route path='/login' component={Login} />
            </Router>
          )}
        </AuthContext.Consumer>
      </AuthContext.Provider >
    )
  }
}

export default App
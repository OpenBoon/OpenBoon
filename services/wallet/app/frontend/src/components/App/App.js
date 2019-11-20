import React from 'react'
import { BrowserRouter as Router, Route } from 'react-router-dom'
import PropTypes from 'prop-types'

import RequireAuth from '../RequireAuth'
import User from '../../models/User'
import Login from '../Login'
import Workspace from '../Workspace'

function App(props) {
  return (
    <Router>
      <Route path="/" component={Login} {...props} />
    </Router>
  )
}

App.propTypes = {
  user: PropTypes.instanceOf(User),
}

App.defaultProps = {
  user: new User({}),
}

export default App

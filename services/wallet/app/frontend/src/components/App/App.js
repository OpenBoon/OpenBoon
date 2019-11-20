import React from 'react'
import { BrowserRouter as Router, Route } from 'react-router-dom'
import PropTypes from 'prop-types'

import Login from '../Login'
import RequireAuth from '../RequireAuth'
import User from '../../models/User'

function App(props) {
  return (
    <Router>
      <Route path="/login" component={Login} {...props} />
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

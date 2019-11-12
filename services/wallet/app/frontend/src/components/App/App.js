import React, { Component } from 'react'
import { BrowserRouter as Router, Route } from 'react-router-dom'

import Login from '/components/Login'

class App extends Component {
  render() {
    return (
      <Router>
        <Route exact path="/wallet" component={Login} />
      </Router>
    )
  }
}

export default App

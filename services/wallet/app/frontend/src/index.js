import React from 'react'
import ReactDOM from 'react-dom'
import AppProviders from './components/AppProviders'
import App from './components/App'

ReactDOM.render(
  <AppProviders>
    <App />
  </AppProviders>,
  document.getElementById('root'))

import React from 'react'
import ReactDOM from 'react-dom'

// Include all our app-wide style classes
import './styles/core.scss'

import AppProviders from './components/AppProviders'
import App from './components/App'

ReactDOM.render(
  <AppProviders>
    <App />
  </AppProviders>,
  document.getElementById('root'),
)

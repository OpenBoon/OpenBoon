import React from 'react'
import ReactDOM from 'react-dom'
import * as Sentry from '@sentry/browser'
import AppProviders from './components/AppProviders'
import App from './components/App'

Sentry.init({
  dsn: 'https://d772538aae2649d38a8931583ed7719b@sentry.io/1504338',
})

ReactDOM.render(
  <AppProviders>
    <App />
  </AppProviders>,
  document.getElementById('root'),
)

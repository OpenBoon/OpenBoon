import React from 'react'
import ReactDOM from 'react-dom'

import App from './components/App'

// Include all our app-wide style classes
require('./styles/base/core.scss')

ReactDOM.render(<App />, document.getElementById('root'))

import App from 'next/app'

import '../styles/tailwind.css'

import Authentication from '../src/Authentication'

class MyApp extends App {
  render() {
    const { Component, pageProps } = this.props

    return (
      <Authentication>
        <Component {...pageProps} />
      </Authentication>
    )
  }
}

export default MyApp

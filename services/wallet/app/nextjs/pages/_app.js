import App from 'next/app'

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

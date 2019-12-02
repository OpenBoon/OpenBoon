import App from 'next/app'

import Authentication from '../src/Authentication'

class MyApp extends App {
  render() {
    const { Component, pageProps } = this.props

    return (
      <Authentication>
        {({ user, logout }) => (
          <Component user={user} logout={logout} {...pageProps} />
        )}
      </Authentication>
    )
  }
}

export default MyApp

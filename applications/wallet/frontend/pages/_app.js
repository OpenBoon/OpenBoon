/* eslint-disable react/jsx-props-no-spreading */
import App from 'next/app'

import 'focus-visible'
import '@reach/combobox/styles.css'
import '@reach/listbox/styles.css'
import '@reach/skip-nav/styles.css'
import 'react-grid-layout/css/styles.css'
import 'react-resizable/css/styles.css'

import User from '../src/User'
import Authentication from '../src/Authentication'

class MyApp extends App {
  static async getInitialProps({ Component, ctx }) {
    const versions = {
      COMMIT_SHA: process.env.CI_COMMIT_SHA,
    }

    if (Component.getInitialProps) {
      const pageProps = await Component.getInitialProps(ctx)

      return { ...versions, pageProps }
    }

    return { ...versions }
  }

  render() {
    const {
      Component,
      pageProps,
      router: { route },
      err = '',
    } = this.props

    if (route === '/_error') {
      return <Component {...pageProps} err={err} />
    }

    return (
      <User initialUser={{}}>
        <Authentication route={route}>
          <Component {...pageProps} />
        </Authentication>
      </User>
    )
  }
}

export default MyApp

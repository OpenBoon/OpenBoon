import App from 'next/app'
import { SWRConfig } from 'swr'

import '../styles/tailwind.css'

import Authentication from '../src/Authentication'
import Header from '../src/Header'

class MyApp extends App {
  render() {
    const { Component, pageProps } = this.props

    return (
      <Authentication>
        <SWRConfig
          value={{
            fetcher: (resource, init) =>
              fetch(resource, init).then((res) => res.json()),
            suspense: true,
          }}
        >
          <div className="h-screen">
            <div className="flex flex-col items-center w-full h-full">
              <Header />
              <div className="w-screen max-w-screen-xl h-full">
                <Component {...pageProps} />
              </div>
            </div>
          </div>
        </SWRConfig>
      </Authentication>
    )
  }
}

export default MyApp

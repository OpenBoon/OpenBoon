import { useState, useEffect } from 'react'
import getConfig from 'next/config'
import Head from 'next/head'

import Button, { VARIANTS } from '../Button'

const {
  publicRuntimeConfig: { GOOGLE_OAUTH_API_KEY },
} = getConfig()

let GoogleAuth

const LoginWithGoogle = () => {
  const [isLoggedIn, setIsLoggedIn] = useState(false)

  useEffect(() => {
    window.onload = () => {
      window.gapi.load('auth2', async () => {
        GoogleAuth = await window.gapi.auth2.init({
          client_id: `${GOOGLE_OAUTH_API_KEY}.apps.googleusercontent.com`,
        })
        setIsLoggedIn(GoogleAuth.isSignedIn.get())
      })
    }
  })

  return (
    <>
      <Head>
        <script src="https://apis.google.com/js/platform.js" async defer />
      </Head>

      <Button
        variant={VARIANTS.PRIMARY}
        isDisabled={false}
        onClick={async () => {
          if (isLoggedIn) {
            const response = await GoogleAuth.signOut()
            setIsLoggedIn(false)
            console.warn(response)
          } else {
            const response = await GoogleAuth.signIn()
            setIsLoggedIn(true)
            console.warn(response.Zi)
            console.warn(GoogleAuth.currentUser.get())
          }
        }}>
        {isLoggedIn ? 'Sign Out' : 'Sign In with Google'}
      </Button>
    </>
  )
}

export default LoginWithGoogle

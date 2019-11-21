import React from 'react'
import { Redirect } from 'react-router-dom'

import { ACCESS_TOKEN } from '../../constants/authConstants'
import Page from '../Page'

function Workspace() {
  const token = localStorage.getItem(ACCESS_TOKEN)
  if (!token) {
    return <Redirect to={'/'} />
  }

  return (
    <Page>
      <div className="Workspace">
        <p>{'Hello World!'}</p>
      </div>
    </Page>
  )
}

export default Workspace

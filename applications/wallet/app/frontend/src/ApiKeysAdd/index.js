import { useState } from 'react'

import { spacing } from '../Styles'

import Tabs from '../Tabs'
import Input from '../Input'
import Checkbox from '../Checkbox'

const MAX_WIDTH = 546

const ApiKeys = () => {
  const [name, setName] = useState('')

  return (
    <div css={{ padding: spacing.normal, maxWidth: MAX_WIDTH }}>
      <h2>Project API Keys</h2>
      <Tabs
        tabs={[
          { title: 'View all', href: '/api-keys' },
          { title: 'Create API key', href: '/api-keys/add' },
        ]}
      />
      <p>
        Name your project and fill in the details and we will generate a
        personal key for you to download and keep in a safe place.
      </p>
      <form
        method="post"
        onSubmit={event => event.preventDefault()}
        css={{
          display: 'flex',
          flexDirection: 'column',
        }}>
        <Input
          autoFocus
          id="username"
          label="Email"
          type="text"
          value={name}
          onChange={({ target: { value } }) => setName(value)}
          hasError={false}
        />
        <Checkbox label="Permissions" onClick={() => {}} initialValue />
      </form>
    </div>
  )
}

export default ApiKeys

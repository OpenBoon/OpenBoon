import Head from 'next/head'

import Tabs from '../Tabs'

import ApiKeysAddForm from './Form'

const MAX_WIDTH = 546

const ApiKeysAdd = () => {
  return (
    <div css={{ maxWidth: MAX_WIDTH }}>
      <Head>
        <title>Create API key</title>
      </Head>
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
      <ApiKeysAddForm onSubmit={console.warn} />
    </div>
  )
}

export default ApiKeysAdd

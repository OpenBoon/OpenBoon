import Head from 'next/head'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'

import ApiKeysAddForm from './Form'

const ApiKeysAdd = () => {
  return (
    <>
      <Head>
        <title>Create API key</title>
      </Head>

      <PageTitle>Project API Keys</PageTitle>

      <Tabs
        tabs={[
          { title: 'View all', href: '/[projectId]/api-keys' },
          { title: 'Create API key', href: '/[projectId]/api-keys/add' },
        ]}
      />

      <ApiKeysAddForm />
    </>
  )
}

export default ApiKeysAdd

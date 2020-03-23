import Head from 'next/head'

import PageTitle from '../PageTitle'
import ApiKeysCopy from '../ApiKeys/Copy'
import Tabs from '../Tabs'
import SuspenseBoundary from '../SuspenseBoundary'

import ApiKeysAddForm from './Form'

const ApiKeysAdd = () => {
  return (
    <>
      <Head>
        <title>Create API key</title>
      </Head>

      <PageTitle>Project API Keys</PageTitle>

      <ApiKeysCopy />

      <Tabs
        tabs={[
          { title: 'View all', href: '/[projectId]/api-keys' },
          { title: 'Create API key', href: '/[projectId]/api-keys/add' },
        ]}
      />

      <SuspenseBoundary>
        <ApiKeysAddForm />
      </SuspenseBoundary>
    </>
  )
}

export default ApiKeysAdd

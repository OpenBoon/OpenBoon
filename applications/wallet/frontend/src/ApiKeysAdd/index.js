import Head from 'next/head'
import { useRouter } from 'next/router'

import PageTitle from '../PageTitle'
import Tabs from '../Tabs'

import { onSubmit } from './helpers'

import ApiKeysAddForm from './Form'

const ApiKeysAdd = () => {
  const {
    query: { projectId },
  } = useRouter()

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

      <ApiKeysAddForm onSubmit={onSubmit({ projectId })} />
    </>
  )
}

export default ApiKeysAdd
